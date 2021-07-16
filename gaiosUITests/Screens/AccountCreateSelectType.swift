import XCTest

class AccountCreateSelectType: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.AccountCreateSelectTypeScreen.view]
    }
    
    
    
    @discardableResult
    func tapStandardCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.AccountCreateSelectTypeScreen.cardStandard])
        return self
    }
}
