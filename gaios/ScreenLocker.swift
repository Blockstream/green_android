import Foundation
import UIKit

class ScreenLocker {

    public static let shared = ScreenLocker()
    private var countdownInterval: TimeInterval?
    // Indicates whether or not the user is currently locked out of the app.
    private var isScreenLockLocked: Bool = false

    // App is inactive or in background
    var appIsInactiveOrBackground: Bool = false {
        didSet {
            // Setter for property indicating that the app is either
            // inactive or in the background, e.g. not "foreground and active."
            if appIsInactiveOrBackground {
                startCountdown()
            } else {
                activateBasedOnCountdown()
                countdownInterval = nil
            }
            ensureUI()
        }
    }

    // App is in background
    var appIsInBackground: Bool = false {
        didSet {
            if appIsInBackground {
                startCountdown()
            } else {
                activateBasedOnCountdown()
            }
            ensureUI()
        }
    }

    func startObserving() {
        // Initialize the screen lock state.
        clear()
        appIsInactiveOrBackground = UIApplication.shared.applicationState != UIApplication.State.active

        NotificationCenter.default.addObserver(self, selector: #selector(applicationDidBecomeActive), name: NSNotification.Name.UIApplicationDidBecomeActive, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillResignActive), name: NSNotification.Name.UIApplicationWillResignActive, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillEnterForeground), name: NSNotification.Name.UIApplicationWillEnterForeground, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationDidEnterBackground), name: NSNotification.Name.UIApplicationDidEnterBackground, object: nil)
    }

    func stopObserving() {
        hideLockWindow()
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationDidBecomeActive, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationWillResignActive, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationWillEnterForeground, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIApplicationDidEnterBackground, object: nil)
    }

    func clear() {
        countdownInterval = nil
        isScreenLockLocked = false
        hideLockWindow()
    }

    func startCountdown() {
        if self.countdownInterval == nil {
            self.countdownInterval = CACurrentMediaTime()
        }
    }

    func activateBasedOnCountdown() {
        if self.isScreenLockLocked {
            // Screen lock is already activated.
            return
        }
        if self.countdownInterval == nil {
            // We became inactive, but never started a countdown.
            return
        }

        let countdown: TimeInterval = CACurrentMediaTime() - countdownInterval!
        let settings = getGAService().getSettings()
        let altimeout = settings != nil ? settings!.altimeout * 60 : 5 * 60
        if Int(countdown) >= altimeout {
            // after timeout
            self.isScreenLockLocked = true
        }
    }

    deinit {
        stopObserving()
    }

    @objc func applicationDidBecomeActive(_ notification: NSNotification) {
        self.appIsInactiveOrBackground = false
    }

    @objc func applicationWillResignActive(_ notification: NSNotification) {
        self.appIsInactiveOrBackground = true
    }

    @objc func applicationWillEnterForeground(_ notification: NSNotification) {
        self.appIsInBackground = false
    }

    @objc func applicationDidEnterBackground(_ notification: NSNotification) {
        self.appIsInBackground = true
    }

    func showLockWindow() {
        // Hide Root Window
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return }
        appDelegate.window!.isHidden = true
        ScreenLockWindow.shared.show()
    }

    func hideLockWindow() {
        ScreenLockWindow.shared.hide()
        // Show Root Window
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return }
        appDelegate.window!.isHidden = false
        // By calling makeKeyAndVisible we ensure the rootViewController becomes first responder.
        // In the normal case, that means the ViewController will call `becomeFirstResponder`
        // on the vc on top of its navigation stack.
        appDelegate.window!.makeKeyAndVisible()
    }

    func ensureUI() {
        if self.isScreenLockLocked {
            if self.appIsInactiveOrBackground {
                showLockWindow()
            } else {
                unlock()
            }
        } else if !self.appIsInactiveOrBackground {
            // App is inactive or background.
            hideLockWindow()
        } else {
            showLockWindow()
        }
    }

    func unlock() {
        if self.appIsInactiveOrBackground {
            return
        }
        clear()
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "autolock"), object: nil, userInfo: nil)
    }
}
