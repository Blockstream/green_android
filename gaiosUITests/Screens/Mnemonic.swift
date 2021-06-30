import XCTest

class Mnemonic: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.MnemonicScreen.view]
    }
    
    @discardableResult
    func typeWords(_ words: [String]) -> Self {
        for (i, w) in words.enumerated() {
            let e = app.textFields["\(i + 1)"]
            type(value: w, in: e)
            sleep(1)
        }
        return self
    }
    
    @discardableResult
    func closeKey() -> Self {
        tap(app.staticTexts[AccessibilityIdentifiers.MnemonicScreen.titleLbl])
        return self
    }
    
    @discardableResult
    func tapDone() -> Self {
        tap(button: AccessibilityIdentifiers.MnemonicScreen.doneBtn)
        return self
    }
    
}
