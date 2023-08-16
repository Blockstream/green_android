import Foundation
import AsyncBluetooth
import CoreBluetooth
import hw
import gdk
import greenaddress

enum BLEManagerError: Error {
    case powerOff(txt: String)
    case unauthorized(txt: String)
    case notReady(txt: String)
    case scanErr(txt: String)
    case bleErr(txt: String)
    case timeoutErr(txt: String)
    case dashboardErr(txt: String)
    case outdatedAppErr(txt: String)
    case wrongAppErr(txt: String)
    case authErr(txt: String)
    case swErr(txt: String)
    case genericErr(txt: String)
    case firmwareErr(txt: String)

    var localizedDescription: String {
        switch self {
        case .powerOff(let txt):
            return txt
        case .notReady(let txt):
            return txt
        case .scanErr(let txt):
            return txt
        case .bleErr(let txt):
            return txt
        case .timeoutErr(let txt):
            return txt
        case .dashboardErr(let txt):
            return txt
        case .outdatedAppErr(let txt):
            return txt
        case .wrongAppErr(let txt):
            return txt
        case .authErr(let txt):
            return txt
        case .swErr(let txt):
            return txt
        case .genericErr(let txt):
            return txt
        case .firmwareErr(let txt):
            return txt
        case .unauthorized(let txt):
            return txt
        }
    }
}

enum DeviceError: Error {
    case dashboard
    case wrong_app
    case outdated_app
    case notlegacy_app
}

class BleViewModel {
    static let shared = BleViewModel()
    private let centralManager: CentralManager
    var type: DeviceType = .Jade
    var jade: BleJadeManager?
    var ledger: BleLedgerManager?
    var peripheralID: UUID? {
        didSet { setup() }
    }

    init(centralManager: CentralManager = CentralManager.shared) {
        self.centralManager = centralManager
    }

    var peripheral: Peripheral? {
        if let id = peripheralID {
            return centralManager.retrievePeripherals(withIdentifiers: [id]).first
        }
        return nil
    }

    func setup() {
        guard let peripheral = peripheral else { return }
        switch type {
        case .Jade:
            let impl = BleJade(peripheral: peripheral, centralManager: centralManager)
            jade = BleJadeManager(bleJade: impl)
            ledger = nil
        case .Ledger:
            let impl = BleLedger(peripheral: peripheral, centralManager: centralManager)
            ledger = BleLedgerManager(bleLedger: impl)
            jade = nil
        }
    }

    func connect() async throws {
        if centralManager.bluetoothState == .poweredOff {
            throw BLEManagerError.powerOff(txt: "id_enable_bluetooth_from_system".localized)
        }
        switch type {
        case .Jade:
            try await jade?.connect()
        case .Ledger:
            try await ledger?.connect()
        }
    }

    func isConnected() -> Bool {
        let peripherals = centralManager.retrieveConnectedPeripherals(withServices: [
            CBUUID(string: BleJadeConnection.SERVICE_UUID.uuidString),
            CBUUID(string: BleLedger.SERVICE_UUID.uuidString)])
        return !peripherals.filter { peripheral?.identifier == $0.identifier }.isEmpty
    }
    
    func disconnect() async throws {
        switch type {
        case .Jade:
            try await jade?.disconnect()
        case .Ledger:
            try await ledger?.disconnect()
        }
    }
    
    func initialize(testnet: Bool) async throws {
        switch type {
        case .Jade:
            let res = try await jade?.authenticating(testnet: testnet)
            if res == false {
                throw HWError.Abort("Invalid initialization")
            }
        case .Ledger:
            _ = try await ledger?.authenticating()
        }
    }

    func authenticating() async throws -> Bool {
        switch type {
        case .Jade:
            return try await jade?.authenticating() ?? false
        case .Ledger:
            return try await ledger?.authenticating() ?? false
        }
    }

    func login(account: Account) async throws -> Account? {
        var res: Account? = account
        AnalyticsManager.shared.loginWalletStart()
        do {
            switch type {
            case .Jade:
                res = try await jade?.login(account: account)
            case .Ledger:
                res = try await ledger?.login(account: account)
            }
            AnalyticsManager.shared.loginWalletEnd(account: account, loginType: .hardware)
            return res
        } catch {
            let text = toBleError(error, network: nil).localizedDescription
            AnalyticsManager.shared.failedWalletLogin(account: account, error: error, prettyError: text)
            throw error
        }
    }

    func validateAddress(account: WalletItem, address: Address) async throws -> Bool {
        if !isConnected() {
            try await connect()
            _ = try await authenticating()
        }
        switch type {
        case .Jade:
            return try await jade?.validateAddress(account: account, addr: address) ?? false
        case .Ledger:
            throw HWError.Abort("Not supported")
        }
    }

    func defaultAccount() async throws -> Account? {
        switch type {
        case .Jade:
            return try await jade?.defaultAccount()
        case .Ledger:
            return try await ledger?.defaultAccount()
        }
    }

    func checkFirmware() async throws -> (JadeVersionInfo?, Firmware?) {
        guard let jade = jade else { throw HWError.Abort("No peripheral found") }
        return try await jade.checkFirmware()
    }
    
    func fetchFirmware(firmware: Firmware) async throws -> Data {
        guard let jade = jade else { throw HWError.Abort("No peripheral found") }
        return try await jade.fetchFirmware(firmware: firmware)
    }

    func updateFirmware(firmware: Firmware, binary: Data) async throws -> Bool {
        guard let jade = jade else { throw HWError.Abort("No peripheral found") }
        AnalyticsManager.shared.otaStartJade(account: AccountsRepository.shared.current, firmware: firmware)
        let updated = try await jade.updateFirmware(firmware: firmware, binary: binary)
        AnalyticsManager.shared.otaCompleteJade(account: AccountsRepository.shared.current, firmware: firmware)
        return updated
    }
    
    func ping() async throws {
        switch type {
        case .Jade:
            _ = try await jade?.ping()
        case .Ledger:
            break
        }
    }    

    func toBleError(_ err: Error, network: String?) -> BLEManagerError {
        if let err = err as? BLEManagerError {
            return err
        }
        if let err = err as? DeviceError {
            switch err {
            case DeviceError.dashboard:
                return BLEManagerError.dashboardErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
            case DeviceError.notlegacy_app:
                return BLEManagerError.dashboardErr(txt: "Install legacy companion app")
            case DeviceError.outdated_app:
                return BLEManagerError.outdatedAppErr(txt: "Outdated Ledger app: update the bitcoin app via Ledger Manager")
            case DeviceError.wrong_app:
                return BLEManagerError.wrongAppErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
            }
        }
        if let err = err as? HWError {
            switch err {
            case HWError.Abort(let desc),
                HWError.URLError(let desc),
                HWError.Declined(let desc):
                return BLEManagerError.genericErr(txt: desc)
            default:
                return BLEManagerError.authErr(txt: "id_login_failed")
            }
        }
        if let err = err as? BleLedger {
            return BLEManagerError.swErr(txt: "id_invalid_status_check_that_your")
        }
        if let err = err as? AuthenticationTypeHandler.AuthError {
            return BLEManagerError.genericErr(txt: err.localizedDescription)
        }
        if let err = err as? LoginError {
            return BLEManagerError.genericErr(txt: "id_login_failed")
        }
        return BLEManagerError.genericErr(txt: err.localizedDescription)
    }

    func networkLabel(_ network: String) -> String {
        switch network {
        case "testnet":
            return "Bitcoin Test"
        case "testnet-liquid":
            return "Liquid Test"
        default:
            return "Bitcoin"
        }
    }
}
