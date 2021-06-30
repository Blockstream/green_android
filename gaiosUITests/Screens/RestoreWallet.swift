import XCTest

class RestoreWallet: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RestoreWalletScreen.view]
    }
    
    @discardableResult
    func tapRestoreCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.RestoreWalletScreen.restoreCard])
        return self
    }
    
}
