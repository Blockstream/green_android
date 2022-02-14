import XCTest
@testable import gaios

class TransactionstUITests: XCTestBase {
    
    func testTransaction() {
        
        prepareWallet()
        
        Overview()
            .pause(1)
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .tapSend()
        
        Send()
            .pause(1)
            .pasteAddress()
            .pause(1)
            .typeAmount("0.00001")
            .pause(1)
            .tapDone()
            .pause(2)
            .tapNext()

        SendConfirm()
            .pause(2)
            .tapNext()
        
        Overview()
            .pause(3)
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
