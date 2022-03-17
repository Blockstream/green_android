import XCTest

class SendConfirm: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendConfirmScreen.view]
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
    func drag() -> Self {
        
        let e = app.otherElements[AccessibilityIdentifiers.SendConfirmScreen.viewSlider]
        
        print(e.frame)
        let start = e.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let finish = e.coordinate(withNormalizedOffset: CGVector(dx: 10, dy: 0))
        start.press(forDuration: 0.5, thenDragTo: finish)
        
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
