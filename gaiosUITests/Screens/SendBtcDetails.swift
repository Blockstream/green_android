import XCTest

class SendBtcDetails: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendBtcDetailsScreen.view]
    }
    
    @discardableResult
    func typeAmount(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.SendBtcDetailsScreen.amountTextField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func closeKey() -> Self {
        tap(app.staticTexts[AccessibilityIdentifiers.SendBtcDetailsScreen.recipientTitle])
        return self
    }
    
    @discardableResult
    func tapReview() -> Self {
        tap(button: AccessibilityIdentifiers.SendBtcDetailsScreen.reviewBtn)
        return self
    }
    
    
}
