import XCTest

class RecoveryPhrase: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RecoveryPhraseScreen.view]
    }
    
    @discardableResult
    func tapPhraseCard() -> Self {
        tap(app.otherElements[AccessibilityIdentifiers.RecoveryPhraseScreen.phraseCard])
        return self
    }
    
}
