import XCTest
@testable import gaios

class AddWalletUITests: XCTestBase {
    
    func testAAAUninstall() {
        let app = XCUIApplication()
        app.uninstall(name: "Green")
    }
    
    func testAddWallet() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        ChooseSecurity()
            .tapMultiSigCard()
        
        DialogMnemonicLenght()
            .pause(1)
            .tap12()
        
        RecoveryInstructions()
            .tapContinue()
        
        RecoveryCreate()
            .cleanWords()
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

    func testAddWallet24() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        ChooseSecurity()
            .tapMultiSigCard()
        
        DialogMnemonicLenght()
            .pause(1)
            .tap24()
        
        RecoveryInstructions()
            .tapContinue()
        
        RecoveryCreate()
            .cleanWords()
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)
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

    func testRestoreWallet() {
        let walletName = Constants.walletName
        let words = Constants.mnemonic
        
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
        
        restoreWallet(walletName: walletName, words: words, isSingleSig: false, isLiquid: false)
    }

    func testRestoreWallet24() {
        let walletName = Constants.walletName24
        let words = Constants.mnemonic24
        
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
        
        restoreWallet(walletName: walletName, words: words, isSingleSig: false, isLiquid: false)
    }

    func testWatchOnlySetUp() {
        let walletName = Constants.walletName
        let words = Constants.mnemonic
        
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet(walletName: walletName, words: words, isSingleSig: false, isLiquid: false)
        }
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapSettings()
        
        Settings()
            .pause(1)
            .tapWatchOnly()
            .pause(1)
            .typeUsername(Constants.cryptoUser)
            .pause(1)
            .typePassword(Constants.cryptoPwd)
            .tapSave()
            .pause(1)
            .tapLogOut()
        
        Home()
            .pause(1)
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapWatchOnlyWallet()
        
        WatchOnly()
            .pause(1)
            .typeUsername(Constants.cryptoUser)
            .pause(1)
            .typePassword(Constants.cryptoPwd)
            .pause(1)
            .tapTestnetSwitch()
            .pause(1)
            .tapLogin()
            
        Overview()
            .pause(1)
            .waitTransactionsLoad()
    }

    func testAddSingleSigWallet() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapTestnetCard()
    
        ChooseSecurity()
            .tapSingleSigCard()
        
        DialogMnemonicLenght()
            .pause(1)
            .tap12()
        
        RecoveryInstructions()
            .tapContinue()
        
        RecoveryCreate()
            .cleanWords()
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
    
    func testRestoreSingleSigWallet() {
        let walletName = Constants.walletNameSingleSig
        let words = Constants.mnemonicSingleSig
        
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
        
        restoreWallet(walletName: walletName, words: words, isSingleSig: true, isLiquid: false)
    }
    
    func testAddLiquidWallet() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapLiquidTestnetCard()
    
        ChooseSecurity()
            .tapMultiSigCard()
            
        DialogMnemonicLenght()
            .pause(1)
            .tap12()
        
        RecoveryInstructions()
            .tapContinue()
        
        RecoveryCreate()
            .cleanWords()
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
    
    func testRestoreLiquidWallet() {
        let walletName = Constants.walletNameLiquid
        let words = Constants.mnemonicLiquid
        
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
        
        restoreWallet(walletName: walletName, words: words, isSingleSig: false, isLiquid: true)
    }
}
