import Foundation
import AsyncBluetooth
import hw
import gdk
import UIKit

class BleJadeManager {

    let bleJade: BleJade
    var pinServerSession: SessionManager?
    var walletManager: WalletManager?
    var version: JadeVersionInfo?
    var hash: String?
    var warningPinShowed = false

    var customWhitelistUrls = [String]()
    var persistCustomWhitelistUrls: [String] {
        get { UserDefaults.standard.array(forKey: "whitelist_domains") as? [String] ?? [] }
        set { UserDefaults.standard.setValue(customWhitelistUrls, forKey: "whitelist_domains") }
    }

    func domain(from url: String) -> String? {
        var url = url.starts(with: "http://") || url.starts(with: "https://") ? url : "http://\(url)"
        let urlComponents = URLComponents(string: url)
        if let host = urlComponents?.host {
            if let port = urlComponents?.port {
                return "\(host):\(port)"
            }
            return host
        }
        return nil
    }

    var name: String { bleJade.peripheral.name ?? "" }

    init(bleJade: BleJade) {
        self.bleJade = bleJade
        bleJade.gdkRequestDelegate = self
    }

    public func connect() async throws {
        try await bleJade.open()
    }
    
    public func disconnect() async throws {
        try await bleJade.close()
        customWhitelistUrls = []
    }

    public func version() async throws -> JadeVersionInfo {
        let version = try await bleJade.version()
        self.version = version
        return version
    }

    func connectPinServer(testnet: Bool? = nil) async throws {
        let version = try await bleJade.version()
        let isTestnet = (testnet == true && version.jadeNetworks == "ALL") || version.jadeNetworks == "TEST"
        let networkType: NetworkSecurityCase = isTestnet ? .testnetSS : .bitcoinSS
        if pinServerSession == nil {
            pinServerSession = SessionManager(networkType.gdkNetwork)
        }
        try await pinServerSession?.connect()
    }

    func authenticating(testnet: Bool? = nil) async throws -> Bool {
        _ = try await bleJade.addEntropy()
        let version = try await bleJade.version()
        try? await connectPinServer(testnet: testnet)
        let chain = pinServerSession?.gdkNetwork.chain ?? "mainnet"
        switch version.jadeState {
        case "READY":
            return true
        case "TEMP":
            return try await bleJade.unlock(network: chain)
        default:
            return try await bleJade.auth(network: chain)
        }
    }
    
    func login(account: Account) async throws -> Account {
        let version = try await bleJade.version()
        let device: HWDevice = .defaultJade(fmwVersion: version.jadeVersion)
        let masterXpub = try await bleJade.xpubs(network: account.gdkNetwork.chain, path: [])
        let walletId = SessionManager(account.gdkNetwork).walletIdentifier(masterXpub: masterXpub)
        var account = account
        account.xpubHashId = walletId?.xpubHashId
        account = normalizeAccount(account)
        walletManager = WalletsRepository.shared.getOrAdd(for: account)
        walletManager?.hwDevice = BLEDevice(peripheral: bleJade.peripheral, device: device, interface: bleJade)
        try await walletManager?.login(device: device, masterXpub: masterXpub)
        AccountsRepository.shared.current = walletManager?.account
        return account
    }
    
    func defaultNetwork() async throws -> NetworkSecurityCase {
        let version = try await bleJade.version()
        return version.jadeNetworks == "TEST" ? .testnetSS : .bitcoinSS
    }

    func normalizeAccount(_ account: Account) -> Account {
        // check existing previous account
        let prevAccount = AccountsRepository.shared.hwAccounts.first { $0.isJade == account.isJade && $0.gdkNetwork == account.gdkNetwork && $0.xpubHashId == account.xpubHashId }
        if var prevAccount = prevAccount {
            prevAccount.name = account.name
            prevAccount.hidden = account.hidden
            return prevAccount
        }
        return account
    }

    func defaultAccount() async throws -> Account {
        let version = try await bleJade.version()
        let device: HWDevice = .defaultJade(fmwVersion: version.jadeVersion)
        let network = try await defaultNetwork()
        return Account(name: bleJade.peripheral.name ?? device.name,
                       network: network,
                       isJade: device.isJade,
                       isLedger: device.isLedger,
                       isSingleSig: network.gdkNetwork.electrum,
                       uuid: bleJade.peripheral.identifier,
                       hidden: false)
    }

    func checkFirmware() async throws -> (JadeVersionInfo?, Firmware?) {
        let version = try await bleJade.version()
        let fmw = try await bleJade.firmwareData(version)
        return (version, fmw)
    }

    func fetchFirmware(firmware: Firmware) async throws -> Data {
        let version = try await bleJade.version()
        let binary = try await bleJade.getBinary(version, firmware)
        //hash = bleJade.sha256(binary).hex
        return binary
    }
    
    func updateFirmware(firmware: Firmware, binary: Data) async throws -> Bool {
        let version = try await bleJade.version()
        let updated = try await bleJade.updateFirmware(version: version, firmware: firmware, binary: binary)
        return updated
    }

    func validateAddress(account: WalletItem, addr: Address) async throws -> Bool {
        let network = account.gdkNetwork
        let address = try await bleJade.newReceiveAddress(chain: network.chain,
                                           mainnet: network.mainnet,
                                           multisig: !network.electrum,
                                           chaincode: account.recoveryChainCode,
                                           recoveryPubKey: account.recoveryPubKey,
                                           walletPointer: account.pointer,
                                           walletType: account.type.rawValue,
                                           path: addr.userPath ?? [],
                                           csvBlocks: addr.subtype ?? 0)
        return address == addr.address
    }

    func ping() async throws -> JadeVersionInfo {
        return try await withThrowingTaskGroup(of: JadeVersionInfo.self) { group in
            group.addTask { return try await self.version() }
            group.addTask {
                try await Task.sleep(nanoseconds: UInt64(3 * 1_000_000_000))
                try Task.checkCancellation()
                try await self.disconnect()
                throw BLEManagerError.genericErr(txt: "Re-pair your jade")
            }
            guard let success = try await group.next() else {
                throw _Concurrency.CancellationError()
            }
            group.cancelAll()
            return success
        }
    }
}

extension BleJadeManager: JadeGdkRequest {

    @MainActor
    func showUrlValidationWarning(domains: [String], completion: @escaping(UIAlertOption) -> () = { _ in }) {
        if warningPinShowed {
            completion(.continue)
            return
        }
        DispatchQueue.main.async {
            let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
            if let vc = hwFlow.instantiateViewController(withIdentifier: "PinServerWarnViewController") as? PinServerWarnViewController {
                vc.onSupport = {
                    if let url = URL(string: ExternalUrls.pinServerSupport + Common.versionNumber) {
                        SafeNavigationManager.shared.navigate( url )
                    }
                    /// navigating info center sends cancel event
                    completion(.cancel)
                }
                vc.onConnect = { [weak self] notAskAgain in
                    self?.warningPinShowed = true
                    self?.customWhitelistUrls += domains
                    if notAskAgain {
                        self?.persistCustomWhitelistUrls += self?.customWhitelistUrls ?? []
                    }
                    completion(.continue)
                }
                vc.onClose = {
                    completion(.cancel)
                }
                vc.domains = domains
                vc.modalPresentationStyle = .overFullScreen
                UIApplication.topViewController()?.present(vc, animated: false, completion: nil)
            }
        }
    }

    @MainActor
    func showUrlValidationWarning(domains: [String]) async -> UIAlertOption {
        await withCheckedContinuation { continuation in
            showUrlValidationWarning(domains: domains) { result in
                continuation.resume(with: .success(result))
            }
        }
    }

    func urlValidation(urls: [String]) async -> Bool {
        let whitelistUrls = bleJade.blockstreamUrls + customWhitelistUrls + persistCustomWhitelistUrls
        let whitelistDomains = whitelistUrls.compactMap { domain(from: $0) }
        let domains = urls.filter { !$0.isEmpty }
            .compactMap { domain(from: $0) }
        let isUrlSafe = domains.allSatisfy { domain in whitelistDomains.contains(domain) }
        if isUrlSafe {
            return true
        }
        switch await showUrlValidationWarning(domains: domains) {
        case .continue: return true
        case .cancel: return false
        }
    }

    func httpRequest(params: [String: Any]) async -> [String: Any]? {
        return self.pinServerSession?.httpRequest(params: params)
    }
}
