import XCTest

class Send: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendScreen.view]
    }

    @discardableResult
    func pasteAddress() -> Self {
        tap(button: AccessibilityIdentifiers.SendScreen.pasteAddressBtn)
        return self
    }

    @discardableResult
    func typeAmount(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.SendScreen.amountField]
        return type(value: txt, in: e)
    }

    @discardableResult
    func tapDone() -> Self {
        tap(button: AccessibilityIdentifiers.KeyboardView.done)
        return self
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.SendScreen.nextBtn)
        return self
    }
}
