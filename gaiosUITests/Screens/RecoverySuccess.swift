import XCTest

class RecoverySuccess: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RecoverySuccessScreen.view]
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.RecoverySuccessScreen.nextBtn)
        return self
    }
}
