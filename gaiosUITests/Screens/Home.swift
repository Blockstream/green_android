import XCTest

class Home: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.HomeScreen.view]
    }

    @discardableResult
    func tapAddWalletView() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.HomeScreen.addWalletView])
        return self
    }
}
