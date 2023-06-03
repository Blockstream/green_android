import Foundation
import BreezSDK
import PromiseKit
import gdk
import greenaddress
import lightning
import hw

class LightningSessionManager: SessionManager {

    var lightBridge: LightningBridge?
    var nodeState: NodeState?
    var lspInfo: LspInformation?
    var keychain: String?
    
    var chainNetwork: NetworkSecurityCase { gdkNetwork.mainnet ? .bitcoinSS : .testnetSS }

    override func connect() -> Promise<Void> {
        connected = true
        return Promise().asVoid()
    }

    private func initLightningSdk(_ params: Credentials, appGreenlightCredentials: AppGreenlightCredentials? = nil) -> LightningBridge {
        let walletHashId = walletIdentifier(credentials: params)?.walletHashId
        let workingDir = "\(GdkInit.defaults().breezSdkDir)/\(walletHashId ?? "")/0"
        return LightningBridge(testnet: !gdkNetwork.mainnet,
                               credentials: appGreenlightCredentials,
                               workingDir: URL(fileURLWithPath: workingDir),
                               eventListener: self)
    }

    private func connectToGreenlight(credentials: Credentials, create: Bool) {
        guard let mnemonic = getLightningMnemonic(credentials: credentials) else {
            fatalError("Unsupported feature")
        }
        if create {
            lightBridge?.connectToGreenlight(mnemonic: mnemonic)
        } else {
             _ = lightBridge?.connectToGreenlightIfExists(mnemonic: mnemonic)
        }
    }

    override func loginUser(_ params: Credentials) -> Promise<LoginUserResult> {
        let greenlightCredentials = LightningRepository.shared.get(for: keychain ?? "")
        let walletId = walletIdentifier(credentials: params)
        lightBridge = initLightningSdk(params, appGreenlightCredentials: greenlightCredentials)
        return Promise { seal in
            connectToGreenlight(credentials: params, create: greenlightCredentials == nil)
            if let greenlightCredentials = lightBridge?.credentials {
                LightningRepository.shared.upsert(for: keychain ?? "", credentials: greenlightCredentials)
            }
            logged = true
            walletHashId = walletId?.walletHashId
            nodeState = lightBridge?.updateNodeInfo()
            lspInfo = lightBridge?.updateLspInformation()
            let walletId = walletIdentifier(credentials: params)
            seal.fulfill(LoginUserResult(xpubHashId: walletId?.xpubHashId ?? "", walletHashId: walletId?.walletHashId ?? ""))
        }
    }

    deinit {
        lightBridge?.stop()
    }

    override func disconnect() {
        logged = false
        connected = false
        lightBridge?.stop()
    }

    override func walletIdentifier(credentials: Credentials) -> WalletIdentifier? {
        let res = try? self.session?.getWalletIdentifier(
            net_params: GdkSettings.read()?.toNetworkParams(chainNetwork.network).toDict() ?? [:],
            details: credentials.toDict() ?? [:])
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }

    override func walletIdentifier(masterXpub: String) -> WalletIdentifier? {
        let details = ["master_xpub": masterXpub]
        let res = try? self.session?.getWalletIdentifier(
            net_params: GdkSettings.read()?.toNetworkParams(chainNetwork.network).toDict() ?? [:],
            details: details)
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }

    override func existDatadir(walletHashId: String) -> Bool {
        return LightningRepository.shared.get(for: keychain ?? "") != nil
    }

    override func removeDatadir(walletHashId: String) {
        let workingDir = "\(GdkInit.defaults().breezSdkDir)/\(walletHashId)/0"
        try? FileManager.default.removeItem(atPath: workingDir)
    }

    override func enabled() -> Bool {
        return AppSettings.shared.lightningEnabled && AppSettings.shared.experimental
    }

    override func getBalance(subaccount: UInt32, numConfs: Int) -> Promise<[String : Int64]> {
        return Promise { seal in
            let sats = lightBridge?.balance()
            let balance = [gdkNetwork.getFeeAsset(): Int64(sats ?? 0)]
            seal.fulfill(balance)
        }
    }

    func subaccount_() -> WalletItem {
        return WalletItem(name: "", pointer: 0, receivingId: "", type: .lightning, hidden: false)
    }

    override func subaccount(_ pointer: UInt32) -> Promise<WalletItem> {
        return Guarantee()
            .compactMap(on: bgq) { self.subaccount_() }
    }

    override func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        return subaccount(0)
            .compactMap { [$0] }
    }

    override func signTransaction(tx: Transaction) -> Promise<Transaction> {
        return Promise.value(tx)
    }

    override func sendTransaction(tx: Transaction) -> Promise<Void> {
        return Guarantee().compactMap(on: bgq) { try self.sendTransaction_(tx: tx) }
    }

    private func sendTransaction_(tx: Transaction) throws {
        let invoiceOrLnUrl = tx.addressees.first?.address
        let satoshi = tx.addressees.first?.satoshi ?? 0
        let comment = tx.memo
        switch lightBridge?.parseBoltOrLNUrl(input: invoiceOrLnUrl) {
        case .bolt11(let invoice):
            // Check for expiration
            print ("Expire in \(invoice.expiry)")
            if invoice.isExpired {
                throw TransactionError.invalid(localizedDescription: "id_invoice_expired")
            }
            let res = try lightBridge?.sendPayment(bolt11: invoice.bolt11, satoshi: nil)
            print("res \(res)")
        case .lnUrlPay(let data):
            let res = lightBridge?.payLnUrl(requestData: data, amount: Long(satoshi), comment: comment)
            print("res \(res)")
            switch res {
            case .endpointSuccess(let data):
                print("payLnUrl success: \(data)")
            case .endpointError(let data):
                print("payLnUrl error: \(data.reason)")
                throw TransactionError.invalid(localizedDescription: data.reason)
            case .none:
                throw TransactionError.invalid(localizedDescription: "id_error")
            }
        default:
            throw TransactionError.invalid(localizedDescription: "id_error")
        }
    }

    func generateLightningError(
        account: WalletItem,
        satoshi: UInt64?,
        min: UInt64? = nil,
        max: UInt64? = nil
    ) -> String? {
        let balance = account.btc ?? 0
        guard let satoshi = satoshi, satoshi > 0 else {
            return "id_invalid_amount"
        }
        if let min = min, satoshi < min {
            return "id_amount_must_be_at_least_s"
        }
        if satoshi > balance {
            return "id_insufficient_funds"
        }
        if let max = max, satoshi > max {
            return "id_amount_must_be_at_most_s"
        }
        return nil
    }

    override func createTransaction(tx: Transaction) -> Promise<Transaction> {
        return Guarantee().compactMap(on: bgq) { try self.createTransaction_(tx: tx) }
    }

    private func createTransaction_(tx: Transaction) -> Transaction {
        let address = tx.addressees.first?.address ?? ""
        let userInputSatoshi = tx.addressees.first?.satoshi ?? 0
        switch lightBridge?.parseBoltOrLNUrl(input: address) {
        case .bolt11(let invoice):
            // Check for expiration
            print ("Expire in \(invoice.expiry)")
            let sendableSatoshi = invoice.sendableSatoshi(userSatoshi: UInt64(abs(userInputSatoshi))) ?? 0
            var tx = tx
            var addressee = Addressee.fromLnInvoice(invoice, fallbackAmount: sendableSatoshi)
            addressee.satoshi = abs(addressee.satoshi ?? 0)
            tx.error = ""
            tx.addressees = [addressee]
            tx.amounts = ["btc": Int64(sendableSatoshi)]
            tx.transactionOutputs = [TransactionOutput.fromLnInvoice(invoice, fallbackAmount: Int64(sendableSatoshi))]
            if invoice.isExpired {
                tx.error = "id_invoice_expired"
            }
            if let description = invoice.description {
                tx.memo = description
            }
            if let subaccount = tx.subaccountItem,
               let error = generateLightningError(account: subaccount, satoshi: sendableSatoshi) {
                tx.error = error
            }
            return tx
        case .lnUrlPay(let requestData):
            let sendableSatoshi = requestData.sendableSatoshi(userSatoshi: UInt64(userInputSatoshi)) ?? 0
            var tx = tx
            var addressee = Addressee.fromRequestData(requestData, input: address, satoshi: sendableSatoshi)
            addressee.satoshi = abs(addressee.satoshi ?? 0)
            tx.error = ""
            tx.addressees = [addressee]
            tx.amounts = ["btc": Int64(sendableSatoshi)]
            tx.transactionOutputs = [TransactionOutput.fromLnUrlPay(requestData, input: address, satoshi: Int64(sendableSatoshi))]
            if let subaccount = tx.subaccountItem,
               let error = generateLightningError(account: subaccount, satoshi: sendableSatoshi, min: requestData.minSendableSatoshi, max: requestData.maxSendableSatoshi) {
                tx.error = error
            }
            return tx
        default:
            return tx
        }
    }

    override func discovery(credentials: Credentials? = nil, hw: HWDevice? = nil, removeDatadir: Bool, walletHashId: String) -> Promise<Void> {
        return Guarantee()
            .then(on: bgq) { self.getBalance(subaccount: 0, numConfs: 0) }
            .compactMap(on: bgq) {
                let notFunded = $0["btc"] == 0
                if notFunded && removeDatadir {
                    self.disconnect()
                    self.removeDatadir(walletHashId: walletHashId)
                    LightningRepository.shared.remove(for: self.keychain ?? "")
                }
            }
    }

    func createInvoice(satoshi: UInt64, description: String) throws -> LnInvoice? {
        try lightBridge?.createInvoice(satoshi: satoshi, description: description)
    }

    override func parseTxInput(_ input: String, satoshi: Int64?, assetId: String?) -> Promise<ValidateAddresseesResult> {
        return Guarantee()
            .compactMap(on: bgq) { self.parseLightningInputType(input) }
            .compactMap(on: bgq) { self.parseLightningTxInput(input, inputType: $0) }
    }

    func parseLightningInputType(_ input: String) -> InputType? {
        lightBridge?.parseBoltOrLNUrl(input: input)
    }

    func parseLightningTxInput(_ input: String, inputType: InputType) -> ValidateAddresseesResult {
        switch inputType {
        case .bitcoinAddress(_):
            //let addr = Addressee.from(address: address.address, satoshi: Int64(address.amountSat ?? 0), assetId: nil)
            //return ValidateAddresseesResult(isValid: true, errors: [], addressees: [addr])
            return ValidateAddresseesResult(isValid: false, errors: ["id_invalid_address"], addressees: [])
        case .bolt11(let invoice):
            let addr = Addressee.fromLnInvoice(invoice, fallbackAmount: 0)
            return ValidateAddresseesResult(isValid: true, errors: [], addressees: [addr])
        case .lnUrlPay(let data):
            let addr = Addressee.fromRequestData(data, input: input, satoshi: 0)
            return ValidateAddresseesResult(isValid: true, errors: [], addressees: [addr])
        case .lnUrlAuth(_), .lnUrlWithdraw(_), .nodeId(_), .url(_), .lnUrlError(_):
            return ValidateAddresseesResult(isValid: false, errors: ["Unsupported"], addressees: [])
        }
    }

    override func getReceiveAddress(subaccount: UInt32) -> Promise<Address> {
        return Guarantee()
            .compactMap { self.lightBridge }
            .compactMap { $0.receiveOnchain() }
            .compactMap { Address.from(swapInfo: $0) }
    }

    override func transactions(subaccount: UInt32, first: UInt32 = 0) -> Promise<Transactions> {
        if first > 0 {
            return Promise.value(Transactions(list: []))
        }
        return Guarantee()
                .compactMap(on: bgq) { self.transactions_(subaccount: subaccount, first: first)}
    }

    func transactions_(subaccount: UInt32, first: UInt32 = 0) -> Transactions {
        let subaccount = self.subaccount_().hashValue
        guard let lb = self.lightBridge else {
            return Transactions(list: [])
        }
        var txs = lb.getTransactions().compactMap { Transaction.fromPayment($0, subaccount: subaccount) }
        if let swapList = lb.swapList() {
            txs += swapList.map { Transaction.fromSwapInfo($0, subaccount: subaccount, isRefundableSwap: false) }
        }
        if let swapProgress = lb.swapProgress() {
            txs += [ Transaction.fromSwapInfo(swapProgress, subaccount: subaccount, isRefundableSwap: true) ]
        }
        return Transactions(list: txs.sorted().reversed())
    }

    func getLightningMnemonic(credentials: Credentials) -> String? {
        return Wally.bip85FromMnemonic(mnemonic: credentials.mnemonic ?? "",
                          passphrase: credentials.bip39Passphrase,
                          isTestnet: !gdkNetwork.mainnet,
                          index: 0)
    }
}

extension LightningSessionManager: LightningEventListener {
    func onLightningEvent(event: BreezSDK.BreezEvent) {
        switch event {
        case .synced:
            //nodeState = lightBridge?.updateNodeInfo()
            //lspInfo = lightBridge?.updateLspInformation()
            break
        case .newBlock(let block):
            blockHeight = block
            DispatchQueue.main.async {
                self.post(event: .Block)
            }
        case .invoicePaid(let data):
            DispatchQueue.main.async {
                self.post(event: .InvoicePaid, object: data)
                DropAlert().success(message: "Invoice Paid".localized)
            }
        case .paymentSucceed(let details):
            DispatchQueue.main.async {
                self.post(event: .PaymentSucceed)
                DropAlert().success(message: "Payment Succeed \(details.amountSatoshi) sats".localized)
            }
        case .paymentFailed(_):
            DispatchQueue.main.async {
                self.post(event: .PaymentFailed)
                DropAlert().error(message: "Payment Failure".localized)
            }
        }
    }    
}
