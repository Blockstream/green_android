import UIKit

class EventWindow: UIWindow {

    private var timer: Timer?
    private var duration: TimeInterval {
        guard let settings = Settings.shared else { return 5 * 60 }
        return TimeInterval(settings.altimeout * 60)
    }

    private var resetTimerToken: NSObjectProtocol?
    private var activeToken: NSObjectProtocol?
    private var resignToken: NSObjectProtocol?

    func startObserving() {
        resetTimerToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil, queue: .main, using: resetTimer)
        activeToken = NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main, using: applicationDidBecomeActive)
        resignToken = NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main, using: applicationWillResignActive)
    }

    func stopObserving () {
        if let token = resetTimerToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = activeToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = resignToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func resetTimer(_ notification: Notification) {
        self.resetTimer()
    }

    private func resetTimer() {
        DispatchQueue.main.async {
            self.timer?.invalidate()
            self.timer = Timer.scheduledTimer(timeInterval: self.duration,
                                                  target: self,
                                                  selector: #selector(self.timeout(_:)),
                                                  userInfo: nil,
                                                  repeats: false)
        }
    }

    @objc private func timeout(_ timer: Timer) {
        guard Settings.shared != nil else {
            // If user not logged to a session
            return
        }
        DispatchQueue.main.async {
            NSLog("Idle timer expired: locking application...")
            let appDelegate = UIApplication.shared.delegate as? AppDelegate
            appDelegate?.logout(with: false)
        }
    }

    func applicationWillResignActive(_ notification: Notification) {
        DispatchQueue.main.async {
            self.timer?.invalidate()
        }
    }

    func applicationDidBecomeActive(_ notification: Notification) {
        resetTimer()
    }

    override func sendEvent(_ event: UIEvent) {
        super.sendEvent(event)
        resetTimer()
    }
}
