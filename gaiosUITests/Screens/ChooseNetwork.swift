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
    
    @discardableResult
    func tapLiquidTestnetCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.ChooseNetworkScreen.liquidTestnetCard])
        return self
    }
    
    @discardableResult
    func swipeListUp() -> Self {
        rootElement.scrollViews.firstMatch.swipeUp()
        return self
    }
    
}
