import XCTest

class DialogMnemonicLenght: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogMnemonicLengthScreen.view]
    }

    @discardableResult
    func tap12() -> Self {
        tap(button: AccessibilityIdentifiers.DialogMnemonicLengthScreen.length12Btn)
        return self
    }

    @discardableResult
    func tap24() -> Self {
        tap(button: AccessibilityIdentifiers.DialogMnemonicLengthScreen.length24Btn)
        return self
    }

}

