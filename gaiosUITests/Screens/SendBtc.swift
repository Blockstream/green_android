import XCTest

class SendBtc: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendBtcScreen.view]
    }
    
    @discardableResult
    func copyClipboard() -> Self {
        let pasteboard = UIPasteboard.general
        if let string = pasteboard.string {
            let e = app.textViews[AccessibilityIdentifiers.SendBtcScreen.textView]
            type(value: string, in: e)
        } else {
            XCTFail("Error empty clipboard")
        }
        return self
    }

    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.SendBtcScreen.nextBtn)
        return self
    }
    
    @discardableResult
    func closeKey() -> Self {
        let e = app.textViews[AccessibilityIdentifiers.SendBtcScreen.textView]
        e.typeText("\n")
        return self
    }
}
