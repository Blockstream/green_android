import Foundation
import AsyncBluetooth
import hw
import gdk

class BleLedgerManager {
    
    let bleLedger: BleLedger
    var walletManager: WalletManager?
    
    init(bleLedger: BleLedger) {
        self.bleLedger = bleLedger
    }
    
    public func connect() async throws {
        if !bleLedger.connected {
            try await bleLedger.open()
        }
    }
    
    public func disconnect() async throws {
        try await bleLedger.close()
    }

    public func version() async throws -> [String: Any] {
        //try await bleLedger.version()
        return [:]
    }

    func getLedgerNetwork() async throws -> NetworkSecurityCase {
        let app = try await bleLedger.application()
        let name = app["name"] as? String ?? ""
        let version = app["version"] as? String ?? ""
        if name.contains("OLOS") {
            throw DeviceError.dashboard // open app from dashboard
        }
        if version >= "2.1.0" && ["Bitcoin", "Bitcoin Test"].contains(name) {
            throw DeviceError.notlegacy_app
        }
        switch name {
        case "Bitcoin", "Bitcoin Legacy":
            return .bitcoinSS
        case "Bitcoin Test", "Bitcoin Test Legacy":
            return .testnetSS
        case "Liquid":
            return .liquidMS
        case "Liquid Test":
            return .testnetLiquidMS
        default:
            throw DeviceError.wrong_app
        }
    }
    
    func getMasterXpub() async throws -> String {
        let network = try await getLedgerNetwork()
        return try await bleLedger.xpubs(network: network.chain, path: [])
    }

    func authenticating() async throws -> Bool {
        _ = try await getLedgerNetwork()
        return true
    }
    
    func login(account: Account) async throws -> Account {
        let device: HWDevice = .defaultLedger()
        let masterXpub = try await bleLedger.xpubs(network: account.gdkNetwork.chain, path: [])
        let walletId = SessionManager(account.gdkNetwork).walletIdentifier(masterXpub: masterXpub)
        var account = account
        account.xpubHashId = walletId?.xpubHashId
        account = normalizeAccount(account)
        walletManager = WalletsRepository.shared.getOrAdd(for: account)
        walletManager?.hwDevice = BLEDevice(peripheral: bleLedger.peripheral, device: device, interface: bleLedger)
        try await walletManager?.login(device: device, masterXpub: masterXpub)
        return account
    }

    func normalizeAccount(_ account: Account) -> Account {
        // check existing previous account
        let prevAccount = AccountsRepository.shared.hwAccounts.first { $0.isLedger == account.isLedger && $0.gdkNetwork == account.gdkNetwork && $0.xpubHashId == account.xpubHashId }
        if var prevAccount = prevAccount {
            prevAccount.name = account.name
            prevAccount.hidden = account.hidden
            return prevAccount
        }
        return account
    }
    
    func defaultAccount() async throws -> Account {
        let device: HWDevice = .defaultLedger()
        let network = try await getLedgerNetwork()
        return Account(name: bleLedger.peripheral.name ?? device.name,
                       network: network,
                       isJade: device.isJade,
                       isLedger: device.isLedger,
                       isSingleSig: network.gdkNetwork.electrum,
                       uuid: bleLedger.peripheral.identifier)
    }

    func validateAddress(account: WalletItem, addr: Address) async throws -> Bool {
        let network = account.gdkNetwork
        let address = try await bleLedger.newReceiveAddress(chain: network.chain,
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
}
