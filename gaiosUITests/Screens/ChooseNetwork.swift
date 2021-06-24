import XCTest

class ChooseNetwork: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ChooseNetworkScreen.view]
    }

    @discardableResult
    func tapTestnetCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.ChooseNetworkScreen.testnetCard])
        return self
    }
}
