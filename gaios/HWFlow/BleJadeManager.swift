import Foundation
import AsyncBluetooth
import hw
import gdk

class BleJadeManager {

    let bleJade: BleJade
    var pinServerSession: SessionManager?
    var walletManager: WalletManager?
    var version: JadeVersionInfo?

    init(bleJade: BleJade) {
        self.bleJade = bleJade
        bleJade.gdkRequestDelegate = self
    }

    public func connect() async throws {
        try await bleJade.open()
    }
    
    public func disconnect() async throws {
        try await bleJade.close()
    }

    public func version() async throws -> JadeVersionInfo {
        let version = try await bleJade.version()
        self.version = version
        return version
    }

    func authenticating(testnet: Bool? = nil) async throws -> Bool {
        _ = try await bleJade.addEntropy()
        let version = try await bleJade.version()
        let isTestnet = (testnet == true && version.jadeNetworks == "ALL") || version.jadeNetworks == "TEST"
        let networkType: NetworkSecurityCase = isTestnet ? .testnetSS : .bitcoinSS
        let chain = networkType.chain
        // connect to network pin server
        pinServerSession = SessionManager(networkType.gdkNetwork)
        try? await pinServerSession?.connect()
        switch version.jadeState {
        case "READY":
            return true
        case "TEMP":
            return try await bleJade.unlock(network: chain)
        default:
            return try await bleJade.auth(network: chain)
        }
    }
    
    func login(account: Account) async throws {
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
                       uuid: bleJade.peripheral.identifier)
    }

    func checkFirmware() async throws -> (JadeVersionInfo?, Firmware?) {
        let version = try await bleJade.version()
        let fmw = try await bleJade.firmwareData(version)
        return (version, fmw)
    }

    func updateFirmware(firmware: Firmware) async throws -> Bool {
        let version = try await bleJade.version()
        let binary = try await bleJade.getBinary(version, firmware)
        let updated = try await bleJade.updateFirmware(version: version, firmware: firmware, binary: binary)
        return updated
    }

    func hash(_ binary: Data) -> String {
        let hash = bleJade.sha256(binary)
        return "\(hash.map { String(format: "%02hhx", $0) }.joined())"
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
    func httpRequest(params: [String: Any]) -> [String: Any]? {
        return self.pinServerSession?.httpRequest(params: params)
    }
}
