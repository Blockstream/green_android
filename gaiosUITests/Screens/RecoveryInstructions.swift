import XCTest

class RecoveryInstructions: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RecoveryInstructionsScreen.view]
    }

    @discardableResult
    func tapContinue() -> Self {
        tap(button: AccessibilityIdentifiers.RecoveryInstructionsScreen.continueBtn)
        return self
    }
}
