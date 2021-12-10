import XCTest

class ChooseSecurity: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ChooseSecurityScreen.view]
    }

    @discardableResult
    func tapMultiSigCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.ChooseSecurityScreen.multiSigCard])
        return self
    }
    
    @discardableResult
    func tapSingleSigCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.ChooseSecurityScreen.singleSigCard])
        return self
    }
    
    @discardableResult
    func select24() -> Self {
        app.otherElements[AccessibilityIdentifiers.ChooseSecurityScreen.view].segmentedControls.buttons.element(boundBy: 1).tap()
        return self
    }
    
}
