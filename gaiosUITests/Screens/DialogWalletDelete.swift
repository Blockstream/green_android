import XCTest

class DialogWalletDelete: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogWalletDeleteScreen.view]
    }
    
    @discardableResult
    func tapDelete() -> Self {
        tap(button: AccessibilityIdentifiers.DialogWalletDeleteScreen.deleteBtn)
        return self
    }
    
}
