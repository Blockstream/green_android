import XCTest
@testable import gaios

class ChangePinUITests: XCTestBase {
    
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
    }
    
    func prepareWallet() {
        
        
        if Home().existsWallet(named: walletName) {
            
            Home()
                .selectWallet(named: walletName)

            Login()
                .pause(1)
                .digitPin()
            
        } else {
            restoreWallet()
        }
        
    }
}
