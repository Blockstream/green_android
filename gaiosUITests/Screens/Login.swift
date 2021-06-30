import XCTest

class Login: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.LoginScreen.view]
    }
    
    @discardableResult
    func tapMenu() -> Self {
        tap(button: AccessibilityIdentifiers.LoginScreen.menuBtn)
        return self
    }
    
}
