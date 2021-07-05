import XCTest
@testable import gaios

class AddWalletUITests: XCTestBase {
    
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
    
    func testRestoreWallet() {
        let walletName = Constants.walletName
        
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
        
        restoreWallet()
    }
    
    func testWatchOnlySetUp() {
        let walletName = Constants.walletName
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet()
        }
        
        Transactions()
            .pause(1)
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
            
        Transactions()
            .pause(2)
    }

    func testRenameWallet() {
        // test fails when I have 7 or more wallets
        let walletName = Constants.walletName
        let walletNameRenamed = Constants.walletNameRenamed
        if !Home().existsWallet(named: walletName) {
            restoreWallet()

            Transactions()
                .pause(1)
                .tapSettings()

            Settings()
                .pause(1)
                .tapLogOut()
        }

        Home()
            .selectWallet(named: walletName)

        Login()
            .pause(1)
            .tapMenu()
            .pause(1)

        PopoverMenuWallet()
            .pause(1)
            .tapRenameWallet()
            .pause(1)

        DialogWalletRename()
            .pause(1)
            .typeNewName(name: walletNameRenamed)
            .pause(1)
            .tapSave()

        Login()
            .back()
            .pause(1)

        XCTAssert(Home().existsWallet(named: walletNameRenamed))
    }
}
