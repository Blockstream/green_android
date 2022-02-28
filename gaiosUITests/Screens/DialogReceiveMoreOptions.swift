import XCTest

class DialogReceiveMoreOption: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogReceiveMoreOptionsScreen.view]
    }
    
    @discardableResult
    func tapRequestAmount() -> Self {
        tap(button: AccessibilityIdentifiers.DialogReceiveMoreOptionsScreen.requestAmountBtn)
        return self
    }
}

