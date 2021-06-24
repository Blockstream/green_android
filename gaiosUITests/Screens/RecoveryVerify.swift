import XCTest

class RecoveryVerify: Screen {
    
    let question = "  ______   "
    
    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.RecoveryVerifyScreen.view]
    }
    
    @discardableResult
    func tapNext() -> Self {
        tap(button: AccessibilityIdentifiers.RecoveryCreateScreen.nextBtn)
        return self
    }
    
    func chooseWord() -> Self {
        let mnemonic = RecoveryCreate.mnemonic
        print(mnemonic)
        
        let quiz = app.staticTexts[AccessibilityIdentifiers.RecoveryVerifyScreen.quizLbl].label
        var word = ""
        
        if quiz.hasPrefix(question) {
            word = mnemonic.first!
        } else if quiz.hasSuffix(question) {
            word = mnemonic.last!
        } else {
            let comp: [String] = quiz.components(separatedBy: " " + question + " ")
            let index = mnemonic.firstIndex(where: {$0 == comp.first!})
            word = mnemonic[index! + 1]
        }

         let btnsIdents = [AccessibilityIdentifiers.RecoveryVerifyScreen.word0btn,
                         AccessibilityIdentifiers.RecoveryVerifyScreen.word1btn,
                         AccessibilityIdentifiers.RecoveryVerifyScreen.word2btn,
                         AccessibilityIdentifiers.RecoveryVerifyScreen.word3btn]
        
        for i in 0..<btnsIdents.count {
            if app.buttons[btnsIdents[i]].label == word {
                tap(app.buttons[btnsIdents[i]])
                break
            }
        }

        return self
    }
}
