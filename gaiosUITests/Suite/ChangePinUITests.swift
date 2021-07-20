import XCTest
@testable import gaios

class PinUITests: XCTestBase {
    
    let walletName = Constants.walletName
    
    func testChangePin() {

        prepareWallet()
        
        Transactions()
            .pause(1)
            .tapSettings()
        
        Settings()
            .pause(1)
            .tapSetupPin()
        
        ScreenLock()
            .pause(1)
            .tapPinLbl()
            .pause(1)
        
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
        
        Transactions()
            .pause(1)
            .tapSettings()
        
        Settings()
            .pause(1)
            .tapSetupPin()
        
        ScreenLock()
            .pause(1)
            .tapPinLbl()
            .pause(1)
        
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
        
        Transactions()
            .pause(1)
    }
    
    func testWrongPin() {

        prepareWallet()
        
        Transactions()
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
        
        Transactions()
            .pause(1)
    }
    
    func prepareWallet() {
        let walletName = Constants.walletName
        let words = Constants.mnemonic
        
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet(walletName: walletName, words: words, isSingleSig: false)
        }
        
    }
}
