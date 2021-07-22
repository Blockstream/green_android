import XCTest

class DialogTorSingleSig: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogTorSingleSigScreen.view]
    }
    
    @discardableResult
    func tapContinue() -> Self {
        tap(button: AccessibilityIdentifiers.DialogTorSingleSigScreen.continueBtn)
        return self
    }
    
}
