import XCTest
@testable import gaios

class PinUITests: XCTestBase {
    
    let walletName = Constants.walletName
    
    func testChangePin() {

        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Overview()
            .pause(1)
            .tapSettings()
        
        Settings()
            .pause(1)
            .tapSetupPin()

        SetPin()
            .pause(1)
            .setPin2()
            .pause(1)
            .setPin2()
            .tapNext()
        
        Settings()
            .pause(1)
            .tapLogOut()
        
        Home()
            .pause(1)
            .selectWallet(named: walletName)
        
        Login()
            .pause(1)
            .digitPin2()
        
        Overview()
            .pause(1)
            .tapSettings()
        
        Settings()
            .pause(1)
            .tapSetupPin()

        SetPin()
            .pause(1)
            .setPin()
            .pause(1)
            .setPin()
            .tapNext()
        
        Settings()
            .pause(1)
            .tapLogOut()
        
        Home()
            .pause(1)
            .selectWallet(named: walletName)
        
        Login()
            .pause(1)
            .digitPin()
        
        Overview()
            .pause(1)
    }
    
    func testWrongPin() {

        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Overview()
            .pause(1)
            .tapSettings()
        
        Settings()
            .tapLogOut()

        Home()
            .selectWallet(named: walletName)
        
        Login()
            .pause(1)
            .digitWrongPin()
            .checkErrorWithAttempts(2)
            .pause(1)
            .digitWrongPin()
            .checkErrorLast()
            .pause(1)
            .digitPin()
        
        Overview()
            .pause(1)
    }
}
