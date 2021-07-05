import XCTest

class Transactions: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.TransactionsScreen.view]
    }
    
    @discardableResult
    func tapSettings() -> Self {
        tap(button: AccessibilityIdentifiers.TransactionsScreen.settingsBtn)
        return self
    }
}
