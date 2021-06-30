import XCTest
@testable import gaios

class AddWalletUITests: XCTestCase {
    
    override func setUp() {
        super.setUp()

        continueAfterFailure = false
        XCUIApplication().launch()
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func testAddWallet() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(2)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        ChooseSecurity()
            .tapMultiSigCard()
            
        RecoveryInstructions()
            .tapContinue()
        
        RecoveryCreate()
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)
        
        RecoveryVerify()
            .pause(1)
            .chooseWord() 
            .pause(1)
            .chooseWord()
            .pause(1)
            .chooseWord()
            .pause(1)
        
        RecoverySuccess()
            .pause(1)
            .tapNext()

        WalletName()
            .pause(1)
            .typeName(nil)
            .pause(1)
            .closeKey() 
            .pause(1)
            .tapNext()
            .pause(1)
        
        SetPin()
            .pause(2)
            .setPin()
            .pause(2)
            .setPin()
            .tapNext()
        
        WalletSuccess()
            .pause(2)
            .tapNext()
            
        Transactions()
            .pause(3)
    }
    
    func testRestoreWallet() {
        let walletName = "crypto"
        let words = ["urge", "gaze", "divert", "say", "ready", "spike", "shrimp", "comfort", "tide", "impulse", "rookie", "tell"]
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .tapMenu()
                .pause(1)
            
            PopoverMenuWallet()
                .pause(1)
                .tapRemoveWallet()
                .pause(1)
            
            DialogWalletDelete()
                .pause(1)
                .tapDelete()
                .pause(1)
                .tapDelete()
        }
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(2)
            .tapRestoreWallet()
        
        RestoreWallet()
            .tapRestoreCard()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        ChooseSecurity()
            .tapMultiSigCard()
        
        RecoveryPhrase()
            .tapPhraseCard()
        
        Mnemonic()
            .pause(1)
            .typeWords(words)
            .closeKey()
            .pause(1)
            .tapDone()
        
        WalletName()
            .pause(1)
            .typeName(walletName)
            .pause(1)
            .closeKey()
            .pause(1)
            .tapNext()
            .pause(1)
        
        SetPin()
            .pause(2)
            .setPin()
            .pause(2)
            .setPin()
            .tapNext()
        
        WalletSuccess()
            .pause(2)
            .tapNext()
            
        Transactions()
            .pause(3)
    }
}
