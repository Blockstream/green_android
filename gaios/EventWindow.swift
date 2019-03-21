import UIKit

class EventWindow : UIWindow {

    private var timer: Timer?
    private var duration: TimeInterval {
        guard let settings = getGAService().getSettings() else { return 5 * 60 }
        return TimeInterval(settings.altimeout * 60)
    }

    func startObserving() {
        NotificationCenter.default.addObserver(self, selector: #selector(self.resetTimer(_:)), name: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationDidBecomeActive), name: NSNotification.Name.UIApplicationDidBecomeActive, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillResignActive), name: NSNotification.Name.UIApplicationWillResignActive, object: nil)
    }

    func stopObserving () {
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationDidBecomeActive, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationWillResignActive, object: nil)
    }

    @objc func resetTimer(_ notification: NSNotification) {
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
        NSLog("Idle timer expired: locking application...")
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "autolock"), object: nil, userInfo:nil)
    }

    @objc func applicationWillResignActive(_ notification: NSNotification) {
        DispatchQueue.main.async {
            self.timer?.invalidate()
        }
    }

    @objc func applicationDidBecomeActive(_ notification: NSNotification) {
        resetTimer()
    }

    override func sendEvent(_ event: UIEvent) {
        super.sendEvent(event)
        resetTimer()
    }
}
