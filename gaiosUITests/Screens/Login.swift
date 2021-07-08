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
    
    @discardableResult
    func digitPin() -> Self {
        for _ in 0..<6 {
            tap(button: AccessibilityIdentifiers.LoginScreen.btn1)
            sleep(1)
        }
        
        return self
    }
    
    @discardableResult
    func digitPin2() -> Self {
        for _ in 0..<6 {
            tap(button: AccessibilityIdentifiers.LoginScreen.btn2)
            sleep(1)
        }
        
        return self
    }
}
