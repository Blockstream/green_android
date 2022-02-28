import XCTest

class SendConfirm: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendConfirmScreen.view]
    }

    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.SendConfirmScreen.nextBtn)
        return self
    }

    @discardableResult
    func checkAmount(_ amount: String) -> Self {
        let exists = app.staticTexts[amount].waitForExistence(timeout: 5)
                    
        if exists != true {
            XCTFail("Amount \(amount) not found")
        }
        
        return self
    }
    
    @discardableResult
    func checkRate(_ txt: String) -> Self {
        let exists = app.staticTexts[txt].waitForExistence(timeout: 5)
                    
        if exists != true {
            XCTFail("\(txt) not found")
        }
        
        return self
    }
}
