import XCTest

class RecoveryCreate: Screen {
    
    static var mnemonic: [String] = []
    
    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RecoveryCreateScreen.view]
    }

    @discardableResult
    func cleanWords() -> Self {

        RecoveryCreate.mnemonic = []
        return self
    }
    
    @discardableResult
    func readWords() -> Self {

        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word1Lbl].label))
        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word2Lbl].label))
        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word3Lbl].label))
        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word4Lbl].label))
        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word5Lbl].label))
        RecoveryCreate.mnemonic.append(Utils.sanitize(app.staticTexts[AccessibilityIdentifiers.RecoveryCreateScreen.word6Lbl].label))
        
        return self
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.RecoveryCreateScreen.nextBtn)
        return self
    }
}
