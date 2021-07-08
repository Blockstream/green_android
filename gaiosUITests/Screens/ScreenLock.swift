import XCTest

class ScreenLock: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ScreenLockScreen.view]
    }
    
    @discardableResult
    func tapPinLbl() -> Self {
        tap(app.buttons[AccessibilityIdentifiers.ScreenLockScreen.pinLbl])
        return self
    }
}
