import XCTest

class DialogWatchOnlySetUp: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.view]
    }

//    @discardableResult
//    func tapDeny() -> Self {
//        tap(button: AccessibilityIdentifiers.DialogAnalyticsConsentScreen.denyBtn)
//        return self
//    }

    @discardableResult
    func clearUsername() -> Self {
        return clearTextField(identifier: AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.usernameField)
    }

    @discardableResult
    func typeUsername(_ txt: String) -> Self {
        
        let e = app.textFields[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.usernameField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func typePassword(_ txt: String) -> Self {
        
        let e = app.secureTextFields[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.passwordField]
        e.tap()
        return type(value: txt, in: e)
    }

    @discardableResult
    func hasCredentials(_ username: String) -> Bool {
        let e = app.textFields[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.usernameField]
        return (e.value as! String) == username
    }

    @discardableResult
    func deleteCredentials() -> Self {
        let e = app.buttons[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.deleteBtn]
        e.tap()
        return self
    }

    @discardableResult
    func dismiss() -> Self {
        let e = app.buttons[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.dismissBtn]
        e.tap()
        return self
    }
    
    @discardableResult
    func assertDisabled() -> Self {
        let e = app.buttons[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.deleteBtn]
        if e.isEnabled {
            XCTFail("Error, wallet has not been deleted")
        }
        return self
    }

    @discardableResult
    func saveCredentials() -> Self {
        let e = app.buttons[AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.saveBtn]
        e.tap()
        return self
    }
}
