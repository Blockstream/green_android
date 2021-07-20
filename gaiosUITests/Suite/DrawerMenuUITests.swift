import XCTest
@testable import gaios

class DrawerMenuUITests: XCTestBase {
    
    let walletName = Constants.walletName
    
    func testSwitchWallet() {

        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
  
        Transactions()
            .pause(1)
            .tapSettings()
        
        Settings()
            .tapLogOut()
        
        prepareWallet(walletName: Constants.walletNameSingleSig, words: Constants.mnemonic, isSingleSig: true)
        
        
        Transactions()
            .pause(1)
            .tapDrawerBtn(Constants.walletNameSingleSig)
            .pause(1)

        Drawer()
            .pause(1)
        
        if Drawer().existsWallet(named: walletName) {
            
            Drawer()
                .selectWallet(named: walletName)
                .pause(1)
            
            Login()
                .pause(1)
                .digitPin()
            
            Transactions()
                .pause(1)
            
        } else {
            XCTFail("Can't find the wallet in the drawer menu list")
        }
        
    }
    
    func testAddWalletBtnFromDrawerMenu() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Transactions()
            .pause(1)
            .tapDrawerBtn(Constants.walletName)
        
        Drawer()
            .pause(1)
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(1)
    }
    
    func testJadeBtnFromDrawerMenu() {
        let name = "Blockstream Jade"
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Transactions()
            .pause(1)
            .tapDrawerBtn(Constants.walletName)
        
        Drawer()
            .pause(1)
            .tapHWWBtn(name)
            .pause(1)
        
        HWWScan()
            .pause(1)
            .checkTitle(name)
    }
    
    func testLedgerBtnFromDrawerMenu() {
        let name = "Ledger Nano X"
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Transactions()
            .pause(1)
            .tapDrawerBtn(Constants.walletName)
        
        Drawer()
            .pause(1)
            .tapHWWBtn(name)
            .pause(1)
        
        HWWScan()
            .pause(1)
            .checkTitle(name)
    }
    
    func prepareWallet(walletName: String, words: [String], isSingleSig: Bool) {
        
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet(walletName: walletName, words: words, isSingleSig: isSingleSig)
        }
        
    }
}
