import XCTest

class SendConfirm: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.SendConfirmScreen.view]
    }

    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.SendConfirmScreen.nextBtn)
        return self
    }
}
