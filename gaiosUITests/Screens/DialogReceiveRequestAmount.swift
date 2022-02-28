import XCTest

class DialogReceiveRequestAmount: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.view]
    }
    
    @discardableResult
    func typeAmount(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.amountField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func tapConfirm() -> Self {
        tap(button: AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.confirmBtn)
        return self
    }
}

