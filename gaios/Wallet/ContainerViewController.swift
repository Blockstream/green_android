import Foundation
import UIKit
import PromiseKit

class ContainerViewController: UIViewController {

    private var networkToken: NSObjectProtocol?
    private var timer = Timer()
    private var seconds = 0

    @IBOutlet weak var networkView: UIView!
    @IBOutlet weak var networkText: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        networkToken  = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        self.networkView.isHidden = true
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if let token = networkToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        let waiting = notification.userInfo?["waiting"] as? Int
        self.seconds = waiting ?? 0
        DispatchQueue.main.async {
            if self.timer.isValid { self.timer.invalidate() }
            if connected! {
                self.networkText.text = NSLocalizedString("id_you_are_now_connected", comment: "")
                self.networkView.backgroundColor = UIColor.customMatrixGreen()
                self.networkView.isHidden = false
                DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + DispatchTimeInterval.milliseconds(2000)) {
                    self.networkView.isHidden = true
                }
            } else {
                self.networkText.text = String(format: NSLocalizedString("id_not_connected_connecting_in_ds_", comment: ""), self.seconds)
                self.networkView.backgroundColor = UIColor.errorRed()
                self.networkView.isHidden = false
                self.timer = Timer.scheduledTimer(timeInterval: 1.0,
                                                          target: self,
                                                          selector: #selector(self.update(_:)),
                                                          userInfo: nil,
                                                          repeats: true)
            }
        }
    }

    @objc private func update(_ timer: Timer) {
        Guarantee().done {
            if self.seconds > 0 {
                self.seconds -= 1
                self.networkText.text = String(format: NSLocalizedString("id_not_connected_connecting_in_ds_", comment: ""), self.seconds)
            }
        }
    }
}
