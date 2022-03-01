import XCTest
@testable import gaios

class SendFlowUITests: XCTestBase {
    
    func testTransaction() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
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
    
    func testSendBipAmount() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Overview()
            .pause(1)
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapMoreOptions()
            .pause(1)

        DialogReceiveMoreOption()
            .pause(1)
            .tapRequestAmount()
            .pause(1)
        
        DialogReceiveRequestAmount()
            .pause(1)
            .typeAmount("0.00001")
            .pause(1)
            .tapConfirm()
            .pause(1)
        
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
            .tapNext()

        SendConfirm()
            .pause(2)
            .checkAmount("0.00001000")
            .pause(2)
    }
    
    func testSendBipFee() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false)
        
        Overview()
            .pause(1)
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapMoreOptions()
            .pause(1)

        DialogReceiveMoreOption()
            .pause(1)
            .tapRequestAmount()
            .pause(1)
        
        DialogReceiveRequestAmount()
            .pause(1)
            .typeAmount("0.00001")
            .pause(1)
            .tapConfirm()
            .pause(1)
        
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
            .tapCustomFee()
        
        DialogCustomFee()
            .pause(1)
            .typeAmount("1.15")
            .pause(1)
            .tapSave()
        
        Send()
            .pause(1)
            .tapNext()

        SendConfirm()
            .pause(2)
            .checkRate("( 1.15 satoshi / vbyte )")
            .pause(2)
    }
}
