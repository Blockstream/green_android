import XCTest
@testable import gaios

class TransactionstUITests: XCTestBase {
    
    func testTransaction() {

        prepareWallet()
        
        Overview()
            .pause(1)
            .tapReceive()
        
        ReceiveBtc()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()

        Overview()
            .pause(1)
            .tapSend()
        
        SendBtc()
            .pause(1)
            .copyClipboard()
            .pause(1)
            .closeKey()
            .tapNext()
        
        SendBtcDetails()
            .pause(1)
            .typeAmount("0.00001")
            .pause(1)
            .closeKey()
            .pause(1)
            .tapReview()
        
        SendBtcConfirmation()
            .pause(1)
            .drag()

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
