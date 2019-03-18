import Foundation
import UIKit

// Behind everything, especially the root window.
let UIWindowLevel_Background: UIWindow.Level = UIWindowLevel(-1)
// In front everything, especially the root window and status bar.
let UIWindowLevel_ScreenBlocking: UIWindow.Level = UIWindowLevelStatusBar + UIWindowLevel(1)

class ScreenLockWindow: UIWindow {
    public static let shared = ScreenLockWindow()

    override func setup() {
        isHidden = false
        isOpaque = true
        windowLevel = UIWindowLevel_Background
        backgroundColor = UIColor.black

        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let viewController = storyboard.instantiateViewController(withIdentifier: "ScreenProtectionViewController")
        rootViewController = viewController
    }

    func show() {
        // Show Screen Lock window
        windowLevel = UIWindowLevel_ScreenBlocking
        makeKeyAndVisible()
    }

    func hide() {
        // Hide Screen Lock window
        // Never hide the blocking window (that can lead to bad frames).
        // Instead, manipulate its window level to move it in front of
        // or behind the root window.
        windowLevel = UIWindowLevel_Background
    }
}
