import XCTest
@testable import gaios

class XCTestBase: XCTestCase {
    
    override func setUp() {
        super.setUp()

        continueAfterFailure = false
        XCUIApplication().launch()
        
        Home()
            .tapAppSettings()
        
        WalletSettings()
            .pause(1)

        if WalletSettings().isTestnetSetTo(true) {

            WalletSettings()
                .pause(1)
                .tapCancel()
        } else {

            WalletSettings()
                .tapTestnetSwitch()
                .pause(1)
                .tapSave()
        }
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func restoreWallet(walletName: String, words: [String], isSingleSig: Bool, isLiquid: Bool) {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapRestoreWallet()

        if isLiquid {
            ChooseNetwork()
                .tapLiquidTestnetCard()
        } else {
            ChooseNetwork()
                .tapTestnetCard()
        }
        
        RecoveryPhrase()
            .tapPhraseCard()
        
        Mnemonic()
            .pause(1)
            .selectLenght(words.count)
            .typeWords(words)
            .closeKey()
            .pause(1)
            .tapDone()

        if isSingleSig {
            ExistingWallets()
                .pause(2)
                .checkWalletsExist()
                .pause(1)
                .tapManualRestore()
            
            ManualRestore()
                .pause(1)
                .tapSingleSigCard()
            
        } else {
            ExistingWallets()
                .pause(2)
                .checkWalletsExist()
                .tapMultisig()
        }
        
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

        Overview()
            .pause(1)
    }
    
    func setTor(_ value: Bool) {
        
        Home()
            .tapAppSettings()
        
        WalletSettings()
            .pause(1)

        if WalletSettings().isTorSetTo(value) {

            WalletSettings()
                .pause(1)
                .tapCancel()
        } else {

            WalletSettings()
                .tapTorSwitch()
                .pause(1)
                .tapSave()
        }
        
    }
    
    func prepareWallet(walletName: String, words: [String], isSingleSig: Bool, isLiquid: Bool) {
        
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet(walletName: walletName, words: words, isSingleSig: isSingleSig, isLiquid: isLiquid)
        }
        
    }
}
