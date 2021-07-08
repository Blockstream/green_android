import XCTest

class SetPin: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SetPinScreen.view]
    }
    
    @discardableResult
    func setPin() -> Self {
        for _ in 0..<6 {
            tap(button: AccessibilityIdentifiers.SetPinScreen.btn1)
            sleep(1)
        }
        
        return self
    }
    
    @discardableResult
    func setPin2() -> Self {
        for _ in 0..<6 {
            tap(button: AccessibilityIdentifiers.SetPinScreen.btn2)
            sleep(1)
        }
        
        return self
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.SetPinScreen.nextBtn)
        return self
    }
    
}
