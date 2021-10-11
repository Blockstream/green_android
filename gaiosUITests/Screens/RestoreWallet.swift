import XCTest

class RestoreWallet: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RestoreWalletScreen.view]
    }
    
    @discardableResult
    func tapSingleSigCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.RestoreWalletScreen.cardSingleSig])
        return self
    }

    @discardableResult
    func tapMultiSigCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.RestoreWalletScreen.cardMultiSig])
        return self
    }
}
