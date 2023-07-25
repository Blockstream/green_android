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
    public var appGreenlightCredentials: AppGreenlightCredentials?
    var breezSdk: BlockingBreezServices?
    var eventListener: LightningEventListener
    var workingDir: URL
    private var network: Network { testnet ? .testnet : .bitcoin }
    private var environment: EnvironmentType { testnet ? .staging : .production }

    static public var BREEZ_API_KEY: String? {
        let content = Bundle.main.infoDictionary?["BREEZ_API_KEY"] as? String
        // print("BREEZ_API_KEY: \(content)")
        if content == nil { print("BREEZ_API_KEY: UNDEFINED") }
        return content
    }
    static public var GREENLIGHT_DEVICE_CERT: Data? {
        let path = Bundle.main.path(forResource: "green", ofType: "crt") ?? ""
        let content = try? String(contentsOf: URL(fileURLWithPath: path), encoding: .utf8)
        if let content = content?.filter({ !$0.isWhitespace }) {
            // print("GREENLIGHT_DEVICE_CERT: \(content)")
            return Data(base64Encoded: content)
        } else {
            print("GREENLIGHT_DEVICE_CERT: UNDEFINED")
        }
        return nil
    }
    static public var GREENLIGHT_DEVICE_KEY: Data? {
        let path = Bundle.main.path(forResource: "green", ofType: "pem") ?? ""
        let content = try? String(contentsOf: URL(fileURLWithPath: path), encoding: .utf8)
        if let content = content?.filter({ !$0.isWhitespace }) {
            // print("GREENLIGHT_DEVICE_KEY: \(content)")
            return Data(base64Encoded: content)
        } else {
            print("GREENLIGHT_DEVICE_KEY: UNDEFINED")
        }
        return nil
    }
    static var CREDENTIALS: GreenlightCredentials? {
        if let cert = LightningBridge.GREENLIGHT_DEVICE_CERT,
           let key = LightningBridge.GREENLIGHT_DEVICE_KEY {
            return GreenlightCredentials(deviceKey: [UInt8](key), deviceCert: [UInt8](cert))
        }
        return nil
    }

    public init(testnet: Bool,
                workingDir: URL,
                eventListener: LightningEventListener) {
        self.testnet = testnet
        self.eventListener = eventListener
        self.workingDir = workingDir
    }

    public static func configure() {
        try? setLogStream(logStream: LogStreamListener())
    }

    public func connectToGreenlight(mnemonic: String, isRestore: Bool) -> Bool {
        let partnerCredentials = isRestore ? nil : LightningBridge.CREDENTIALS
        return start(mnemonic: mnemonic, partnerCredentials: partnerCredentials)
    }

    private func createConfig(_ partnerCredentials: GreenlightCredentials?) -> Config {
        let greenlightConfig = GreenlightNodeConfig(partnerCredentials: partnerCredentials, inviteCode: nil)
        let nodeConfig = NodeConfig.greenlight(config: greenlightConfig)
        var config = defaultConfig(envType: environment,
                      apiKey: LightningBridge.BREEZ_API_KEY ?? "",
                      nodeConfig: nodeConfig)
        config.workingDir = workingDir.path
        try? FileManager.default.createDirectory(atPath: workingDir.path, withIntermediateDirectories: true)
        return config
    }

    private func start(mnemonic: String, partnerCredentials: GreenlightCredentials?) -> Bool {
        if breezSdk != nil {
            return true
        }
        breezSdk = try? connect(config: createConfig(partnerCredentials),
                seed: mnemonicToSeed(phrase: mnemonic),
                listener: self)
        if breezSdk == nil {
            return false
        }
        
        if let credentials = LightningBridge.CREDENTIALS {
            appGreenlightCredentials = AppGreenlightCredentials(gc: credentials)
        }
        _ = updateNodeInfo()
        _ = updateLspInformation()
        return true
    }
    
    public func stop(){
        try? breezSdk?.disconnect()
        breezSdk = nil
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
        let refund = try? breezSdk?.refund(swapAddress: swapAddress, toAddress: toAddress, satPerVbyte: satPerVbyte ?? UInt32(recommendedFees()?.economyFee ?? 0))
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

