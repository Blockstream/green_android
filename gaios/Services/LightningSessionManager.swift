import Foundation
import BreezSDK

import gdk
import greenaddress
import lightning
import hw

class LightningSessionManager: SessionManager {

    var lightBridge: LightningBridge?
    var nodeState: NodeState?
    var lspInfo: LspInformation?
    var accountId: String?
    var isRestoredNode: Bool?
    
    var chainNetwork: NetworkSecurityCase { gdkNetwork.mainnet ? .bitcoinSS : .testnetSS }

    override func connect() async throws {
        connected = true
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

    override func loginUser(credentials: Credentials? = nil, hw: HWDevice? = nil, restore: Bool) async throws -> LoginUserResult {
        guard let params = credentials else { throw LoginError.connectionFailed() }
        let walletId = walletIdentifier(credentials: params)
        let walletHashId = walletId!.walletHashId
        let res = LoginUserResult(xpubHashId: walletId?.xpubHashId ?? "", walletHashId: walletId?.walletHashId ?? "")
        let greenlightCredentials = LightningRepository.shared.get(for: walletHashId)
        isRestoredNode = false
        lightBridge = initLightningBridge(params)
        if connectToGreenlight(credentials: params, isRestore: restore) {
            isRestoredNode = restore
        } else if !restore {
            if !connectToGreenlight(credentials: params) {
                throw LoginError.connectionFailed()
            }
        } else {
            return res
        }
        if let greenlightCredentials = lightBridge?.appGreenlightCredentials {
            LightningRepository.shared.upsert(for: walletHashId, credentials: greenlightCredentials)
        }
        logged = true
        nodeState = lightBridge?.updateNodeInfo()
        lspInfo = lightBridge?.updateLspInformation()
        return res
    }

    override func register(credentials: Credentials? = nil, hw: HWDevice? = nil) async throws {
        _ = try await loginUser(credentials: credentials!, restore: false)
    }

    deinit {
        lightBridge?.stop()
    }

    override func disconnect() async throws {
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

    override func getBalance(subaccount: UInt32, numConfs: Int) async throws -> [String : Int64] {
        let sats = lightBridge?.balance()
        let balance = [gdkNetwork.getFeeAsset(): Int64(sats ?? 0)]
        return balance
    }

    override func subaccount(_ pointer: UInt32) async throws -> WalletItem {
        return WalletItem(name: "", pointer: 0, receivingId: "", type: .lightning, hidden: false, network: NetworkSecurityCase.lightning.network)
    }

    override func subaccounts(_ refresh: Bool = false) async throws -> [WalletItem] {
        let subaccount = try await subaccount(0)
        return [subaccount]
    }

    override func signTransaction(tx: Transaction) async throws -> Transaction {
        return tx
    }

    override func sendTransaction(tx: Transaction) async throws {
        let invoiceOrLnUrl = tx.addressees.first?.address
        let satoshi = tx.addressees.first?.satoshi ?? 0
        let comment = tx.memo ?? ""
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
            let res = try lightBridge?.payLnUrl(requestData: data, amount: Long(satoshi), comment: comment)
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
            return "Amount must be at least \(min)"
        }
        if satoshi > balance {
            return "id_insufficient_funds"
        }
        if let max = max, satoshi > max {
            return "Amount must be at most \(max)"
        }
        return nil
    }

    override func createTransaction(tx: Transaction) async throws -> Transaction {
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

    override func discovery() async throws -> Bool {
        return try await getBalance(subaccount: 0, numConfs: 0)["btc"] != 0
    }

    func createInvoice(satoshi: UInt64, description: String) async throws -> LnInvoice? {
        try lightBridge?.createInvoice(satoshi: satoshi, description: description)
    }

    override func parseTxInput(_ input: String, satoshi: Int64?, assetId: String?) async throws -> ValidateAddresseesResult {
        let inputType = parseLightningInputType(input)!
        return parseLightningTxInput(input, inputType: inputType)
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

    override func getReceiveAddress(subaccount: UInt32) async throws -> Address {
        guard let addr = lightBridge?.receiveOnchain() else {
            throw GaError.GenericError()
        }
        return Address.from(swapInfo: addr)
    }

    override func transactions(subaccount: UInt32, first: UInt32 = 0) async throws -> Transactions {
        if first > 0 {
            return Transactions(list: [])
        }
        return try await transactions_(subaccount: subaccount, first: first)
    }

    func transactions_(subaccount: UInt32, first: UInt32 = 0) async throws -> Transactions {
        let subaccount = try await subaccounts().first.hashValue
        guard let lb = self.lightBridge else {
            return Transactions(list: [])
        }
        var txs = lb.getTransactions().compactMap { Transaction.fromPayment($0, subaccount: subaccount) }
        if let swapList = lb.listRefundables() {
            txs += swapList.map { Transaction.fromSwapInfo($0, subaccount: subaccount, isRefundableSwap: true) }
        }
        if let swapProgress = lb.swapProgress() {
            txs += [ Transaction.fromSwapInfo(swapProgress, subaccount: subaccount, isRefundableSwap: false) ]
        }
        return Transactions(list: txs.sorted().reversed())
    }

    func getLightningMnemonic(credentials: Credentials) -> String? {
        return Wally.bip85FromMnemonic(mnemonic: credentials.mnemonic ?? "",
                          passphrase: credentials.bip39Passphrase,
                          isTestnet: !gdkNetwork.mainnet,
                          index: 0)
    }
    
    func closeChannels() throws {
        try lightBridge?.closeLspChannels()
    }
}

extension LightningSessionManager: LightningEventListener {
    func onLightningEvent(event: BreezSDK.BreezEvent) {
        switch event {
        case .synced:
            NSLog("onLightningEvent synced")
            DispatchQueue.main.async {
                self.post(event: .InvoicePaid)
            }
        case .newBlock(let block):
            NSLog("onLightningEvent newBlock")
            blockHeight = block
            DispatchQueue.main.async {
                self.post(event: .Block)
            }
        case .invoicePaid(let data):
            NSLog("onLightningEvent invoicePaid")
            DispatchQueue.main.async {
                self.post(event: .InvoicePaid, object: data)
                DropAlert().success(message: "Invoice Paid".localized)
            }
        case .paymentSucceed(let details):
            NSLog("onLightningEvent paymentSucceed")
            DispatchQueue.main.async {
                self.post(event: .PaymentSucceed)
                DropAlert().success(message: "Payment Successful \(details.amountSatoshi) sats".localized)
            }
        case .paymentFailed(_):
            NSLog("onLightningEvent paymentFailed")
            DispatchQueue.main.async {
                self.post(event: .PaymentFailed)
                DropAlert().error(message: "Payment Failure".localized)
            }
        default:
            NSLog("onLightningEvent others")
            break
        }
    }    
}
