import XCTest

class WalletSuccess: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.WalletSuccessScreen.view]
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.WalletSuccessScreen.nextBtn)
        return self
    }
    
}
