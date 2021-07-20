import XCTest

class HWWScan: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.HWWScanScreen.view]
    }

    @discardableResult
    func checkTitle(_ title: String) -> Self {
        
        let exists = app.staticTexts[title].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("Blockstream Jade not found")
        }
        return self
    }
}
