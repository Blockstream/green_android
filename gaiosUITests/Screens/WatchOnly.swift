import XCTest

class WatchOnly: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.WatchOnlyScreen.view]
    }
    
    @discardableResult
    func typeUsername(_ txt: String) -> Self {

        let e = app.textFields[AccessibilityIdentifiers.WatchOnlyScreen.usernameField]
        type(value: txt, in: e)
        return self
    }
    
    @discardableResult
    func typePassword(_ txt: String) -> Self {

        let e = app.secureTextFields[AccessibilityIdentifiers.WatchOnlyScreen.passwordField]
        type(value: txt, in: e)
        e.typeText("\n")
        return self
    }
    
    @discardableResult
    func tapTestnetSwitch() -> Self {
        
        app.switches[AccessibilityIdentifiers.WatchOnlyScreen.testnetSwitch].tap()
        return self
    }
    
    @discardableResult
    func tapLogin() -> Self {
        tap(button: AccessibilityIdentifiers.WatchOnlyScreen.loginBtn)
        return self
    }
    
    
}
