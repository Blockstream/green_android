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
    var accountId: String?
    
    var chainNetwork: NetworkSecurityCase { gdkNetwork.mainnet ? .bitcoinSS : .testnetSS }

    override func connect() -> Promise<Void> {
        connected = true
        return Promise().asVoid()
    }

    private func initLightningBridge(_ params: Credentials) -> LightningBridge {
        let walletHashId = walletIdentifier(credentials: params)?.walletHashId
        let workingDir = "\(GdkInit.defaults().breezSdkDir)/\(walletHashId ?? "")/0"
        return LightningBridge(testnet: !gdkNetwork.mainnet,
                               workingDir: URL(fileURLWithPath: workingDir),
                               eventListener: self)
    }

    private func connectToGreenlight(credentials: Credentials, isRestore: Bool = false) -> Bool {
        guard let mnemonic = getLightningMnemonic(credentials: credentials) else {
            fatalError("Unsupported feature")
        }
        return lightBridge?.connectToGreenlight(mnemonic: mnemonic, isRestore: isRestore) ?? false
    }

    override func loginUser(_ params: Credentials) -> Promise<LoginUserResult> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) {
                return Promise { seal in
                    seal.fulfill( try self.loginUser_(params) )
                }
            }
    }

    private func loginUser_(_ params: Credentials) throws -> LoginUserResult {
        let walletId = walletIdentifier(credentials: params)
        let walletHashId = walletId!.walletHashId
        let greenlightCredentials = LightningRepository.shared.get(for: walletHashId)
        lightBridge = initLightningBridge(params)
        if !connectToGreenlight(credentials: params, isRestore: greenlightCredentials == nil) {
            if !connectToGreenlight(credentials: params) {
                throw LoginError.connectionFailed()
            }
        }
        if let greenlightCredentials = lightBridge?.appGreenlightCredentials {
            LightningRepository.shared.upsert(for: walletHashId, credentials: greenlightCredentials)
        }
        logged = true
        nodeState = lightBridge?.updateNodeInfo()
        lspInfo = lightBridge?.updateLspInformation()
        return LoginUserResult(xpubHashId: walletId?.xpubHashId ?? "", walletHashId: walletId?.walletHashId ?? "")
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
        let workingDir = "\(GdkInit.defaults().breezSdkDir)/\(walletHashId)/0"
        return FileManager.default.fileExists(atPath: workingDir)
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
        return WalletItem(name: "", pointer: 0, receivingId: "", type: .lightning, hidden: false, network: NetworkSecurityCase.lightning.network)
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
                throw TransactionError.invalid(localizedDescription: "Invoice Expired")
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
            return "id_invalid_amount".localized
        }
        if let min = min, satoshi < min {
            return "Amount must be at least \(min)"
        }
        if satoshi > balance {
            return "id_insufficient_funds".localized
        }
        if let max = max, satoshi > max {
            return "Amount must be at most \(max)"
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
                tx.error = "Invoice Expired"
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

    override func discovery() -> Promise<Bool> {
        return Guarantee()
            .then(on: bgq) { self.getBalance(subaccount: 0, numConfs: 0) }
            .compactMap { $0["btc"] != 0 }
    }

    func createInvoice(satoshi: UInt64, description: String) throws -> LnInvoice {
        let invoice = try lightBridge?.createInvoice(satoshi: satoshi, description: description)
        if let invoice = invoice { return invoice }
        throw GaError.GenericError("Invalid invoice")
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
                DropAlert().success(message: "Payment Successful \(details.amountSatoshi) sats".localized)
            }
        case .paymentFailed(_):
            DispatchQueue.main.async {
                self.post(event: .PaymentFailed)
                DropAlert().error(message: "Payment Failure".localized)
            }
        default:
            break
        }
    }    
}
