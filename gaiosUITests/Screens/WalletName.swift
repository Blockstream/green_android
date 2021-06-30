import XCTest

class WalletName: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.WalletNameScreen.view]
    }
    
    @discardableResult
    func typeName(_ name: String?) -> Self {
        var txt = Utils.randomString(length: 6)
        if name != nil {
            txt = name!
        }

        let e = app.textFields[AccessibilityIdentifiers.WalletNameScreen.nameField]
        return type(value: txt, in: e)
    }
    
    @discardableResult
    func closeKey() -> Self {
        let e = app.textFields[AccessibilityIdentifiers.WalletNameScreen.nameField]
        e.typeText("\n")
        return self
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.WalletNameScreen.nextBtn)
        return self
    }
}
