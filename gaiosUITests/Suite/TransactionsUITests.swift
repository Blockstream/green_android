import XCTest
@testable import gaios

class SendFlowUITests: XCTestBase {
    
    var amountCache = "empty"

    func testTransaction() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
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
    
    func testTransactionSingleSig() {
        
        prepareWallet(walletName: Constants.walletNameSingleSig, words: Constants.mnemonicSingleSig, isSingleSig: true, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .waitIsReady()
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
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
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
            .waitIsReady()
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
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
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
            .waitIsReady()
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
    
    func testTransactionLiquid() {
        
        prepareWallet(walletName: Constants.walletNameLiquid, words: Constants.mnemonicLiquid, isSingleSig: false, isLiquid: true)
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapSend()
        
        Send()
            .pause(1)
            .pasteAddress()
            .pause(1)
            .chooseAsset()
            .pause(1)
        
        AssetsList()
            .pause(2)
            .selectLBtc()
            .pause(1)
        
        Send()
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
    
    func testSendAllAmount() {
        
        prepareWallet(walletName: Constants.walletName, words: Constants.mnemonic, isSingleSig: false, isLiquid: false)
        
        Overview()
            .pause(1)
            .waitTransactionsLoad()
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapSend()
        
        Send()
            .pause(1)
            .pasteAddress()
            .pause(1)
            .tapSendAll()
            .pause(1)
        
        amountCache = Send().getAmount()
        
        Send()
            .tapNext()

        SendConfirm()
            .pause(2)
            .checkAmount(amountCache)
            .pause(2)
    }
    
    func testSendAllAmountLiquid() {
        
        prepareWallet(walletName: Constants.walletNameLiquid, words: Constants.mnemonicLiquid, isSingleSig: false, isLiquid: true)
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapReceive()
        
        Receive()
            .pause(1)
            .tapQrCode()
            .pause(1)
            .tapBack()
        
        Overview()
            .pause(1)
            .waitIsReady()
            .tapSend()
        
        Send()
            .pause(1)
            .pasteAddress()
            .pause(1)
            .chooseAsset()
            .pause(1)
        
        AssetsList()
            .pause(2)
            .selectLBtc()
            .pause(1)
        
        Send()
            .tapSendAll()
            .pause(1)
        
        amountCache = Send().getAmount()
        
        Send()
            .tapNext()

        SendConfirm()
            .pause(2)
            .checkAmount(amountCache)
            .pause(2)
    }
}
