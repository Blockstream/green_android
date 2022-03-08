import XCTest
@testable import gaios

class DrawerMenuUITests: XCTestBase {
    
    let walletName = Constants.walletName
    
    func testSwitchWallet() {

        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
  
        Overview()
            .pause(1)
            .waitIsReady()
            .tapSettings()
        
        Settings()
            .tapLogOut()
        
        prepareWallet(walletName: Constants.walletNameSingleSig, words: Constants.mnemonic, isSingleSig: true, isLiquid: false)
        
        
        Overview()
            .pause(1)
            .waitIsReady()
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
            
            Overview()
                .pause(1)
            
        } else {
            XCTFail("Can't find the wallet in the drawer menu list")
        }
        
    }
    
    func testAddWalletBtnFromDrawerMenu() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitIsReady()
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
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitIsReady()
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
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapDrawerBtn(Constants.walletName)
        
        Drawer()
            .pause(1)
            .tapHWWBtn(name)
            .pause(1)
        
        HWWScan()
            .pause(1)
            .checkTitle(name)
    }
}
