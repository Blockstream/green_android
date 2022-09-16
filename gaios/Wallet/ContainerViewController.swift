import Foundation
import UIKit
import PromiseKit

class ContainerViewController: UIViewController {

    private var networkToken: NSObjectProtocol?
    private var torToken: NSObjectProtocol?
    private var timer = Timer()
    private var seconds = 0

    var presentingWallet: WalletItem!

    @IBOutlet weak var networkView: UIView!
    @IBOutlet weak var networkText: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        networkToken  = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        torToken  = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil, queue: .main, using: updateTor)
        self.networkView.isHidden = true
        view.accessibilityIdentifier = AccessibilityIdentifiers.ContainerScreen.view
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if let token = networkToken {
            NotificationCenter.default.removeObserver(token)
            networkToken = nil
        }
        if let token = torToken {
            NotificationCenter.default.removeObserver(token)
            torToken = nil
        }

        if self.timer.isValid { self.timer.invalidate() }
    }

    // tor notification handler
    func updateTor(_ notification: Notification) {
        Guarantee().map { () -> UInt32 in
            let json = try JSONSerialization.data(withJSONObject: notification.userInfo!, options: [])
            let tor = try JSONDecoder().decode(TorNotification.self, from: json)
            return tor.progress
        }.done { progress in
            self.networkText.text = NSLocalizedString("id_tor_status", comment: "") + " \(progress)%"
            self.networkView.backgroundColor = UIColor.errorRed()
            self.networkView.isHidden = progress == 100
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    // network notification handler
    func updateConnection(_ notification: Notification) {
        let currentState = notification.userInfo?["current_state"] as? String
        let waitMs = notification.userInfo?["wait_ms"] as? Int
        let connected = currentState == "connected"
        self.seconds = (waitMs ?? 0) / 1000

        // Show connection bar, if a disconnection task is runned
        if connected && self.timer.isValid {
            self.connected()
        }
        if self.timer.isValid {
            self.timer.invalidate()
        }

        // Avoid show network bar for short downtime
        if connected || self.seconds == 0 {
            return
        }
        self.offline(nil)
        self.timer = Timer.scheduledTimer(timeInterval: 1.0,
                                          target: self,
                                          selector: #selector(self.offline(_:)),
                                          userInfo: nil,
                                          repeats: true)
    }

    // show network bar on offline mode
    @objc private func offline(_ timer: Timer?) {
        DispatchQueue.main.async {
            self.networkView.backgroundColor = UIColor.errorRed()
            self.networkView.isHidden = false
            if self.seconds > 0 {
                self.seconds -= 1
                self.networkText.text = String(format: NSLocalizedString("id_not_connected_connecting_in_ds_", comment: ""), self.seconds)
            }
        }
    }

    // show network bar on connected mode
    func connected() {
        DispatchQueue.main.async {
            self.networkText.text = NSLocalizedString("id_you_are_now_connected", comment: "")
            self.networkView.backgroundColor = UIColor.customMatrixGreen()
            self.networkView.isHidden = false
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + DispatchTimeInterval.milliseconds(2000)) {
                self.networkView.isHidden = true
            }
        }
    }
}
