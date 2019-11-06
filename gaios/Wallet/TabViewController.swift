import Foundation
import UIKit
import PromiseKit

class TabViewController: UITabBarController {

    let snackbar = SnackBarNetwork()
    private var startTime = DispatchTime.now()
    private var endTime = DispatchTime.now()

    private var networkToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        networkToken  = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
    }

    func updateConnection(_ notification: Notification) {
        guard let connected = notification.userInfo?["connected"] as? Bool else { return }
        Guarantee().done {
            if connected {
                self.snackbar.connected(self)
            } else {
                guard let waiting = notification.userInfo?["waiting"] as? Int else { return }
                if waiting < 5 {
                    self.snackbar.removeFromSuperview()
                } else {
                    self.snackbar.disconnected(self, seconds: waiting)
                }
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        snackbar.setNeedsDisplay()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if let token = networkToken {
            NotificationCenter.default.removeObserver(token)
        }
    }
}

class SnackBarNetwork: SnackBar {
    var timer = Timer()
    var seconds = 0

    func disconnected(_ controller: UITabBarController, seconds: Int) {
        self.seconds = seconds
        label.text = String(format: NSLocalizedString("id_not_connected_connecting_in_ds_", comment: ""), seconds)
        label.backgroundColor = UIColor.customTitaniumMedium()
        button.setTitle(NSLocalizedString("id_try_now", comment: "").uppercased(), for: .normal)
        button.addTarget(self, action: #selector(self.click), for: .touchUpInside)
        button.isHidden = false
        if timer.isValid { timer.invalidate() }
        timer = Timer.scheduledTimer(timeInterval: 1.0,
                                                  target: self,
                                                  selector: #selector(self.update(_:)),
                                                  userInfo: nil,
                                                  repeats: true)
        addFromController(controller)
    }

    func connected(_ controller: UITabBarController) {
        label.text = NSLocalizedString("id_you_are_now_connected", comment: "")
        label.backgroundColor = UIColor.customMatrixGreen()
        button.isHidden = true
        addFromController(controller)
        if timer.isValid { timer.invalidate() }
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + DispatchTimeInterval.milliseconds(2000)) {
            self.removeFromSuperview()
        }
    }

    override func removeFromSuperview() {
        super.removeFromSuperview()
        if timer.isValid { timer.invalidate() }
    }

    @objc private func update(_ timer: Timer) {
        Guarantee().done {
            if self.seconds > 0 {
                self.seconds -= 1
                self.label.text = String(format: NSLocalizedString("id_not_connected_connecting_in_ds_", comment: ""), self.seconds)
            }
        }
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map {
            self.removeFromSuperview()
        }.map(on: bgq) {
            try getSession().reconnectHint(hint: ["hint": "now"])
        }.catch { _ in }
    }
}
