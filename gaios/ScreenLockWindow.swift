import Foundation
import UIKit

// Behind everything, especially the root window.
let UIWindowLevelBackground: UIWindow.Level = UIWindow.Level(-1)
// In front everything, especially the root window and status bar.
let UIWindowLevelScreenBlocking: UIWindow.Level = UIWindow.Level.statusBar.rawValue + UIWindow.Level(1)

class ScreenLockWindow: UIWindow {
    public static let shared = ScreenLockWindow()

    override func setup() {
        isHidden = false
        isOpaque = true
        windowLevel = UIWindowLevelBackground
        backgroundColor = UIColor.black

        let storyboard = UIStoryboard(name: "Home", bundle: nil)
        let viewController = storyboard.instantiateViewController(withIdentifier: "ScreenProtectionViewController")
        rootViewController = viewController
    }

    func show() {
        // Show Screen Lock window
        windowLevel = UIWindowLevelScreenBlocking
        makeKeyAndVisible()
    }

    func hide() {
        // Hide Screen Lock window
        // Never hide the blocking window (that can lead to bad frames).
        // Instead, manipulate its window level to move it in front of
        // or behind the root window.
        windowLevel = UIWindowLevelBackground
    }
}
