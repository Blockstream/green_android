import XCTest

class ManualRestore: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.ManualRestoreScreen.view]
    }
    
    @discardableResult
    func tapSingleSigCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.ManualRestoreScreen.singleSigCard])
        return self
    }
}

