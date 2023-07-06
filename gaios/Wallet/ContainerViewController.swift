import Foundation
import UIKit

import gdk

class ContainerViewController: UIViewController {

    private var networkToken: NSObjectProtocol?
    private var torToken: NSObjectProtocol?
    private var requests: Int = 0

    var presentingWallet: WalletItem!

    @IBOutlet weak var networkView: UIView!
    @IBOutlet weak var networkText: UILabel!
    @IBOutlet weak var containerView: UIView!

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
    }

    // tor notification handler
    func updateTor(_ notification: Notification) {
        DispatchQueue.main.async {
            if let json = try? JSONSerialization.data(withJSONObject: notification.userInfo!, options: []),
               let tor = try? JSONDecoder().decode(TorNotification.self, from: json) {
                self.networkText.text = NSLocalizedString("id_tor_status", comment: "") + " \(tor.progress)%"
                self.networkView.backgroundColor = UIColor.errorRed()
                self.networkView.isHidden = tor.progress == 100
            }
        }
    }

    // network notification handler
    func updateConnection(_ notification: Notification) {
        let currentState = notification.userInfo?["current_state"] as? String
        let waitMs = notification.userInfo?["wait_ms"] as? Int
        let connected = currentState == "connected"
        // Show connection bar, only for tor
        if AppSettings.shared.gdkSettings?.tor ?? false {
            if connected {
                self.connected()
            } else {
                self.offline()
            }
        }
    }

    // show network bar on offline mode
    @objc private func offline() {
        DispatchQueue.main.async {
            self.networkView.backgroundColor = UIColor.errorRed()
            self.networkView.isHidden = false
            self.networkText.text = "id_connecting".localized
        }
    }

    // show network bar on connected mode
    func connected() {
        let sessions = WalletManager.current?.activeSessions ?? [:]
        let reconnected = sessions.filter { !$0.value.paused }
        guard sessions.count == reconnected.count else { return }
        DispatchQueue.main.async {
            self.networkText.text = "id_you_are_now_connected".localized
            self.networkView.backgroundColor = UIColor.customMatrixGreen()
            self.networkView.isHidden = false
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + DispatchTimeInterval.milliseconds(2000)) {
                self.networkView.isHidden = true
            }
        }
    }
}
