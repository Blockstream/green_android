import XCTest
@testable import gaios

class EditUITests: XCTestBase {
    
    func testSinglesigAlertTor() {

        setTor(false)
        
        XCUIApplication().launch()
        
        prepareWallet(walletName: Constants.walletNameSingleSig, words: Constants.mnemonicSingleSig, isSingleSig: true)

        Overview()
            .pause(1)
            .tapSettings()

        Settings()
            .pause(1)
            .tapLogOut()

        Home()
            .pause(1)
            .selectWallet(named: Constants.walletNameSingleSig)

        Login()
            .pause(1)
            .tapAppSettings()

        WalletSettings()
            .pause(1)
            .enableTor()
            .tapSave()
        
        Login()
            
        DialogTorSingleSig()
            .pause(1)
            .tapContinue()

        Login()
            .pause(1)
            .tapAppSettings()
        
        WalletSettings()
            .pause(1)
            .tapTorSwitch()
            .tapSave()
        
        Login()
            .pause(1)
            .digitPin()

        Overview()
            .pause(1)
    }
}
