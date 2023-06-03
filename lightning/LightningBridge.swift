import Foundation
import BreezSDK

public typealias Long = UInt64

public protocol LightningEventListener {
    func onLightningEvent(event: BreezEvent)
}

class LogStreamListener: LogStream {
    func log(l: LogEntry){
      print("BREEZ: \(l.line)");
    }
}

public class LightningBridge {

    let testnet: Bool
    public var credentials: AppGreenlightCredentials?
    var breezSdk: BlockingBreezServices?
    var eventListener: LightningEventListener
    var workingDir: URL
    private var network: Network { testnet ? .testnet : .bitcoin }
    private var environment: EnvironmentType { testnet ? .staging : .production }

    static let BREEZ_API_KEY = Bundle.main.infoDictionary?["BREEZ_API_KEY"] as? String
    static let GREENLIGHT_DEVICE_CERT = Bundle.main.infoDictionary?["GREENLIGHT_DEVICE_CERT"] as? String
    static let GREENLIGHT_DEVICE_KEY = Bundle.main.infoDictionary?["GREENLIGHT_DEVICE_KEY"] as? String
    static var CREDENTIALS: GreenlightCredentials? {
        if let GREENLIGHT_DEVICE_CERT = LightningBridge.GREENLIGHT_DEVICE_CERT,
           let GREENLIGHT_DEVICE_KEY = LightningBridge.GREENLIGHT_DEVICE_KEY,
           let cert = Data(base64Encoded: GREENLIGHT_DEVICE_CERT),
           let key = Data(base64Encoded: GREENLIGHT_DEVICE_KEY) {
            return GreenlightCredentials(deviceKey: [UInt8](key), deviceCert: [UInt8](cert))
        }
        return nil
    }

    public init(testnet: Bool,
                credentials: AppGreenlightCredentials?,
                workingDir: URL,
                eventListener: LightningEventListener) {
        self.testnet = testnet
        self.credentials = credentials
        self.eventListener = eventListener
        self.workingDir = workingDir
    }

    public static func configure() {
        try? setLogStream(logStream: LogStreamListener())
    }

    public func connectToGreenlight(mnemonic: String) {
        start(mnemonic: mnemonic, greenlightCredentials: getAppLightningCredentials(mnemonic: mnemonic).greenlightCredentials)
    }

    public func connectToGreenlightIfExists(mnemonic: String) -> Bool {
        if checkIfGreenlightNodeExists(mnemonic: mnemonic) {
            connectToGreenlight(mnemonic: mnemonic)
            return true
        } else {
            return false
        }
    }

    private func checkIfGreenlightNodeExists(mnemonic: String) -> Bool {
        if credentials == nil {
            do {
                credentials = AppGreenlightCredentials(
                    gc: try recoverNode(
                        network: network,
                        seed: try mnemonicToSeed(phrase: mnemonic)
                    )
                )
            } catch { print(error) }
        }
        return credentials != nil
    }

    private func getAppLightningCredentials(mnemonic: String) -> AppGreenlightCredentials {
        if !checkIfGreenlightNodeExists(mnemonic: mnemonic) {
            do {
                credentials = AppGreenlightCredentials(
                    gc: try registerNode(
                        network: network,
                        seed: try mnemonicToSeed(phrase: mnemonic),
                        registerCredentials: LightningBridge.CREDENTIALS,
                        inviteCode: nil
                    )
                )
            } catch { print(error) }
        }
        return credentials!
    }

    private func start(mnemonic: String, greenlightCredentials: GreenlightCredentials) {
        if breezSdk != nil {
            fatalError("Service already started")
        }
        var config = defaultConfig(envType: environment)
        config.apiKey = LightningBridge.BREEZ_API_KEY
        config.workingDir = workingDir.path
        let seed = try? mnemonicToSeed(phrase: mnemonic)
        try? FileManager.default.createDirectory(atPath: workingDir.path, withIntermediateDirectories: true)
        breezSdk = try? initServices(
            config: config,
            seed: seed ?? [],
            creds: greenlightCredentials,
            listener: self
        )
        try? breezSdk?.start()
    }
    
    public func stop(){
        try? breezSdk?.stop()
    }

    public func updateLspInformation() -> LspInformation? {
        if let id = try? breezSdk?.lspId() {
            return try? breezSdk?.fetchLspInfo(lspId: id)
        }
        return nil
    }

    public func updateNodeInfo() -> NodeState? {
        let res = try? breezSdk?.nodeInfo()
        print ("NodeInfo: \(res.debugDescription)")
        return res
    }

    public func balance() -> UInt64? {
        return updateNodeInfo()?.channelsBalanceSatoshi
    }

    public func parseBolt11(bolt11: String) -> LnInvoice? {
        print ("Parse invoice: \(bolt11)")
        if bolt11.isEmpty { return nil }
        do { return try parseInvoice(invoice: bolt11) }
        catch { print ("Parse invoice: \(error.localizedDescription)"); return nil }
    }

    public func parseBoltOrLNUrl(input: String?) -> InputType? {
        print("Parse input: \(input)")
        guard let input = input else { return nil }
        return try? parseInput(s: input)
    }

    public func getTransactions() -> [Payment] {
        let list = try? breezSdk?.listPayments(filter: PaymentTypeFilter.all, fromTimestamp: nil, toTimestamp: nil)
        list?.forEach { print("Payment: \($0)") }
        return list ?? []
    }

    public func createInvoice(satoshi: Long, description: String) throws -> LnInvoice? {
        let payment = try breezSdk?.receivePayment(amountSats: satoshi, description: description)
        print("createInvoice \(payment)")
        return payment
    }
    
    public func refund(swapAddress: String, toAddress: String, satPerVbyte: UInt32?) -> String? {
        let refund = try? breezSdk?.refund(swapAddress: swapAddress, toAddress: toAddress, satPerVbyte: satPerVbyte ?? recommendedFees()?.economyFee ?? 0)
        print("refund \(refund)")
        return refund
    }

    public func swapProgress() -> SwapInfo? {
        try? breezSdk?.inProgressSwap()
    }

    public func swapList() -> [SwapInfo]? {
        try? breezSdk?.listRefundables()
    }

    public func receiveOnchain() -> SwapInfo? {
        return try? breezSdk?.receiveOnchain()
    }

    public func recommendedFees() -> RecommendedFees? {
        return try? breezSdk?.recommendedFees()
    }

    public func sendPayment(bolt11: String, satoshi: UInt64? = nil) throws -> Payment? {
        return try breezSdk?.sendPayment(bolt11: bolt11, amountSats: satoshi)
    }

    public func payLnUrl(requestData: LnUrlPayRequestData, amount: Long, comment: String) -> LnUrlPayResult? {
        return try? breezSdk?.payLnurl(reqData: requestData, amountSats: amount, comment: comment)
    }

    public func authLnUrl(requestData: LnUrlAuthRequestData) -> LnUrlCallbackStatus? {
        return try? breezSdk?.lnurlAuth(reqData: requestData)
    }

    public func withdrawLnurl(requestData: LnUrlWithdrawRequestData, amount: Long, description: String?) -> LnUrlCallbackStatus? {
        return try? breezSdk?.withdrawLnurl(reqData: requestData, amountSats: amount, description: description)
    }

    public func listLisps() -> [LspInformation]? {
        return try? breezSdk?.listLsps()
    }

    public func connectLsp(id: String) {
        try? breezSdk?.connectLsp(lspId: id)
    }

    public func lspId() -> String? {
        return try? breezSdk?.lspId()
    }

    public func fetchLspInfo(id: String) -> LspInformation? {
        return try? breezSdk?.fetchLspInfo(lspId: id)
    }
}

extension LightningBridge: EventListener {
    public func onEvent(e: BreezEvent) {
        print ("Breez onEvent: \(e)")
        eventListener.onLightningEvent(event: e)
        switch e {
        case BreezEvent.invoicePaid( _):
            break
        case BreezEvent.paymentSucceed(_):
            break
        default:
            break
        }
    }
}

