import UIKit

class GreenApplication : UIApplication {

    private var timeoutInSeconds: TimeInterval {
        guard let settings = getGAService().getSettings() else { return 5 * 60 }
        let time = TimeInterval(settings.altimeout * 60)
        return time
    }

    private var idleTimer: Timer? = nil

    override init() {
        super.init()

        NotificationCenter.default.addObserver(self, selector: #selector(self.resetIdleTimer(_:)), name: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil)
    }

    @objc func resetIdleTimer(_ notification: NSNotification) {
        self.resetIdleTimer()
    }

    private func resetIdleTimer() {
        DispatchQueue.main.async {
            self.idleTimer?.invalidate()
            self.idleTimer = Timer.scheduledTimer(timeInterval: self.timeoutInSeconds,
                                                  target: self,
                                                  selector: #selector(self.timeout(_:)),
                                                  userInfo: nil,
                                                  repeats: false)
        }
    }

    @objc private func timeout(_ timer: Timer) {
        NSLog("Idle timer expired: locking application...")
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "autolock"), object: nil, userInfo:nil)
    }

    override func sendEvent(_ event: UIEvent) {
        super.sendEvent(event)

        resetIdleTimer()
    }
}
