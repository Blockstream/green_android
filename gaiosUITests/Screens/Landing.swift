import XCTest

class Landing: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.LandingScreen.view]
    }

    @discardableResult
    func tapAcceptTerms() -> Self {
        tap(button: AccessibilityIdentifiers.LandingScreen.acceptTermsBtn)
        return self
    }
    
    @discardableResult
    func tapNewWallet() -> Self {
        tap(button: AccessibilityIdentifiers.LandingScreen.newWalletBtn)
        return self
    }
    
    @discardableResult
    func tapRestoreWallet() -> Self {
        tap(button: AccessibilityIdentifiers.LandingScreen.restoreWalletBtn)
        return self
    }
    
    @discardableResult
    func tapWatchOnlyWallet() -> Self {
        tap(button: AccessibilityIdentifiers.LandingScreen.watchOnlyWalletBtn)
        return self
    }
    
}
