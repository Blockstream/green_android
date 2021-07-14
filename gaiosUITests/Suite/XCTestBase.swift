import XCTest
@testable import gaios

class XCTestBase: XCTestCase {
    
    override func setUp() {
        super.setUp()

        continueAfterFailure = false
        XCUIApplication().launch()
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func restoreWallet(walletName: String, words: [String], isSingleSig: Bool) {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapRestoreWallet()
        
        RestoreWallet()
            .tapRestoreCard()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        if isSingleSig {
            ChooseSecurity()
                .tapSingleSigCard()
        } else {
            ChooseSecurity()
                .tapMultiSigCard()
        }

        
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
            .pause(1)
            .setPin()
            .pause(1)
            .setPin()
            .tapNext()
        
        WalletSuccess()
            .pause(1)
            .tapNext()
            
        Transactions()
            .pause(1)
    }
}
