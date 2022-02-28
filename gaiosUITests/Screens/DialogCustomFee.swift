import XCTest

class DialogCustomFee: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogCustomFeeScreen.view]
    }
    
    @discardableResult
    func typeAmount(_ txt: String) -> Self {
        let e = app.textFields[AccessibilityIdentifiers.DialogCustomFeeScreen.feeField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func tapSave() -> Self {
        tap(button: AccessibilityIdentifiers.DialogCustomFeeScreen.saveBtn)
        return self
    }
}
