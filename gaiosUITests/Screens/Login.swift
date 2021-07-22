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

    @discardableResult
    func back() -> Self {
        tap(button: AccessibilityIdentifiers.LoginScreen.backBtn)
        sleep(1)
        return self
    }
    
    @discardableResult
    func digitWrongPin() -> Self {
        for _ in 0..<6 {
            tap(button: AccessibilityIdentifiers.LoginScreen.btn3)
            sleep(1)
        }
        
        return self
    }
    
    
    @discardableResult
    func checkErrorWithAttempts(_ attempts: Int) -> Self {
        
        let exists = app.staticTexts[String(format: "id_attempts_remaining_d".localized(for: Self.self), attempts)].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("id_attempts_remaining_d not found")
        }
        
        return self
    }
    
    @discardableResult
    func checkErrorLast() -> Self {
        
        let exists = app.staticTexts["id_last_attempt_if_failed_you_will".localized(for: Self.self)].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("id_last_attempt_if_failed_you_will not found")
        }
        
        return self
    }
    
    @discardableResult
    func tapAppSettings() -> Self {

        tap(button: AccessibilityIdentifiers.LoginScreen.settingsBtn)
        return self
    }
    
}
