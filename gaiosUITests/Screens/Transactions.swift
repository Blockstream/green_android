import XCTest

class Transactions: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.TransactionsScreen.view]
    }
    
}
