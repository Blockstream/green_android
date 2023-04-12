import Foundation
import PromiseKit
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import UIKit

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
}

enum DeviceError: Error {
    case dashboard
    case wrong_app
    case outdated_app
}

extension Peripheral {
    func isLedger() -> Bool {
        peripheral.name?.contains("Nano") ?? false
    }

    func isJade() -> Bool {
        peripheral.name?.contains("Jade") ?? false
    }
}

class BLEManager {

    static let shared = BLEManager()
    let manager: CentralManager
    private let queue = DispatchQueue(label: "manager.queue")
    private var session: SessionManager?
    private let sheduler: SerialDispatchQueueScheduler
    private let timeout = RxTimeInterval.seconds(10)
    private var peripherals = [Peripheral]()
    var fmwVersion: String?
    var boardType: String?

    init() {
        manager = CentralManager(queue: queue, options: nil)
        sheduler = SerialDispatchQueueScheduler(queue: queue, internalSerialQueueName: "manager.sheduler")
        Jade.shared.gdkRequestDelegate = self
#if DEBUG
        RxBluetoothKitLog.setLogLevel(.debug)
#endif
    }

    private func waitForBluetooth() -> Observable<BluetoothState> {
        return self.manager
            .observeState()
            .startWith(self.manager.state)
            .filter { $0 == .poweredOn }
            .take(1)
    }

    func scanning() -> Observable<[Peripheral]> {
        peripherals = manager.retrieveConnectedPeripherals(withServices: [JadeChannel.SERVICE_UUID, LedgerChannel.SERVICE_UUID])
            .filter { $0.isJade() || $0.isLedger() }
        return waitForBluetooth()
            .flatMap { _ in
                self.manager.scanForPeripherals(withServices: [JadeChannel.SERVICE_UUID, LedgerChannel.SERVICE_UUID]) }
            .filter { $0.peripheral.isJade() || $0.peripheral.isLedger() }
            .map { p in
                    if let row = self.peripherals.firstIndex(where: { $0.name == p.advertisementData.localName }) {
                    self.peripherals[row] = p.peripheral
                } else {
                    self.peripherals += [p.peripheral]
                }
            }.map { self.peripherals }
    }

    func connectLedger(_ p: Peripheral) -> Observable<Bool> {
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p in Ledger.shared.open(p) }
            .timeoutIfNoEvent(RxTimeInterval.seconds(3))
            .flatMap { _ in self.getLedgerNetwork() }
            .compactMap { _ in return true }
    }

    func getLedgerNetwork() -> Observable<NetworkSecurityCase> {
        return Observable.just(true)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Ledger.shared.application() }
            .compactMap { $0["name"] as? String ?? "" }
            .compactMap { name in
                if name.contains("OLOS") {
                    throw DeviceError.dashboard // open app from dashboard
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
    }

    func getMasterXpub(_ device: HWDevice, gdkNetwork: GdkNetwork?) -> Observable<String> {
        if device.isJade {
            return Jade.shared.xpubs(network: gdkNetwork?.chain ?? "mainnet", path: [])
        }
        return getLedgerNetwork()
            .flatMap { Ledger.shared.xpubs(network: $0.chain, path: []) }
    }

    func connectJade(_ p: Peripheral) -> Observable<Bool> {
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .timeoutIfNoEvent(RxTimeInterval.seconds(3))
            .flatMap { p in Jade.shared.open(p) }
            .timeoutIfNoEvent(RxTimeInterval.seconds(10))
            .flatMap { _ in Jade.shared.addEntropy() }
            .flatMap { _ in Jade.shared.version() }
            .compactMap { $0.jadeHasPin }
    }

    func authenticating(_ p: Peripheral, testnet: Bool? = nil) -> Observable<Bool> {
        if p.isLedger() {
            return Observable.just(p)
            .flatMap { _ in self.getLedgerNetwork() }
            .compactMap { self.session = SessionManager(getGdkNetwork($0.network)) }
            .compactMap { try? self.session?.connect().wait() }
            .compactMap { true }
        }
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Jade.shared.version() }
            .flatMap { version -> Observable<Bool> in
                self.fmwVersion = version.jadeVersion
                self.boardType = version.boardType
                let isTestnet = (testnet == true && version.jadeNetworks == "ALL") || version.jadeNetworks == "TEST"
                let networkType: NetworkSecurityCase = isTestnet ? .testnetSS : .bitcoinSS
                let chain = networkType.chain
                // connect to network pin server
                self.session = SessionManager(getGdkNetwork(networkType.network))
                try? self.session?.connect().wait()
                // JADE_STATE => READY  (device unlocked / ready to use)
                // anything else ( LOCKED | UNSAVED | UNINIT | TEMP) will need an authUser first to unlock
                switch version.jadeState {
                case "READY":
                    return Observable.just(true)
                case "TEMP":
                    return Jade.shared.unlock(network: chain)
                default:
                    return Jade.shared.auth(network: chain)
                            .retry(3)
                }
            }
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

    func getWalletIdentifier(network: GdkNetwork, xpub: String) -> String? {
        return SessionManager(network).walletIdentifier(network.network, masterXpub: xpub)?.xpubHashId
    }

    func logging(_ peripheral: Peripheral, account: Account) -> Observable<WalletManager> {
        let device: HWDevice = peripheral.isJade() ? .defaultJade(fmwVersion: self.fmwVersion) : .defaultLedger()
        let network = NetworkSecurityCase(rawValue: account.networkName)
        var account = account
        return getMasterXpub(device, gdkNetwork: network?.gdkNetwork)
            .compactMap { self.getWalletIdentifier(network: network!.gdkNetwork!, xpub: $0) }
            .compactMap { account.xpubHashId = $0; return account }
            .compactMap { self.normalizeAccount($0) }
            .compactMap { WalletsRepository.shared.getOrAdd(for: $0) }
            .flatMap { wm in
                return Observable<WalletManager>.create { observer in
                    wm.loginWithHW(device, masterXpub: wm.account.xpubHashId ?? "")
                        .done { _ in
                            observer.onNext(wm)
                            observer.onCompleted()
                        }.catch { err in observer.onError(err) }
                    return Disposables.create { }
                }
            }
    }

    func network(_ peripheral: Peripheral) -> Observable<NetworkSecurityCase> {
        if peripheral.isJade() {
            return Jade.shared.version().compactMap { $0.jadeNetworks == "TEST" ? .testnetSS : .bitcoinSS }
        } else {
            return  self.getLedgerNetwork()
        }
    }

    func account(_ peripheral: Peripheral) -> Observable<Account> {
        let device: HWDevice = peripheral.isJade() ? .defaultJade(fmwVersion: self.fmwVersion ?? "") : .defaultLedger()
        return network(peripheral)
            .compactMap { network in
                return Account(name: peripheral.name ?? device.name,
                                      network: network.chain,
                                      isJade: device.isJade,
                                      isLedger: device.isLedger,
                                      isSingleSig: network.gdkNetwork?.electrum ?? true,
                                      uuid: peripheral.identifier)
            }
    }

    private func loginDevice(peripheral: Peripheral, network: NetworkSecurityCase, device: HWDevice) -> Observable<Account> {
        let account = Account(name: peripheral.name ?? device.name,
                              network: network.chain,
                              isJade: device.isJade,
                              isLedger: device.isLedger,
                              isSingleSig: network.gdkNetwork?.electrum ?? true,
                              uuid: peripheral.identifier)
        let wm = WalletManager(account: account, prominentNetwork: network)
        return getMasterXpub(device, gdkNetwork: network.gdkNetwork)
            .flatMap { masterXpub in
                return Observable<WalletManager>.create { observer in
                    wm.loginWithHW(device, masterXpub: masterXpub)
                        .done { _ in
                            observer.onNext(wm)
                            observer.onCompleted()
                        }.catch { err in
                            observer.onError(err)
                        }
                    return Disposables.create { }
                }
            }.compactMap { wm in
                WalletsRepository.shared.add(for: account, wm: wm)
                return account
            }
    }

    func toBleError(_ err: Error, network: String?) -> BLEManagerError {
        switch err {
        case BluetoothError.peripheralConnectionFailed(_, let error):
            return BLEManagerError.bleErr(txt: error?.localizedDescription ?? err.localizedDescription)
        case is BluetoothError, is GaError:
            return BLEManagerError.bleErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: ""))
        case RxError.timeout:
            return BLEManagerError.timeoutErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: ""))
        case DeviceError.dashboard:
            return BLEManagerError.dashboardErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
        case DeviceError.outdated_app:
            return BLEManagerError.outdatedAppErr(txt: "Outdated Ledger app: update the bitcoin app via Ledger Manager")
        case DeviceError.wrong_app:
            return BLEManagerError.wrongAppErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
        case is AuthenticationTypeHandler.AuthError:
            let authErr = err as? AuthenticationTypeHandler.AuthError
            AnalyticsManager.shared.failedWalletLogin(account: AccountsRepository.shared.current, error: err, prettyError: authErr?.localizedDescription ?? "")
            return BLEManagerError.authErr(txt: authErr?.localizedDescription ?? "")
        case is Ledger.SWError:
            return BLEManagerError.swErr(txt: NSLocalizedString("id_invalid_status_check_that_your", comment: ""))
        case is JadeError:
            switch err {
            case JadeError.Abort(let desc),
                 JadeError.URLError(let desc),
                 JadeError.Declined(let desc):
                return BLEManagerError.genericErr(txt: desc)
            default:
                AnalyticsManager.shared.failedWalletLogin(account: AccountsRepository.shared.current, error: err, prettyError: "id_login_failed")
                return BLEManagerError.authErr(txt: NSLocalizedString("id_login_failed", comment: ""))
            }
        case LoginError.failed, LoginError.connectionFailed, LoginError.walletsJustRestored, LoginError.walletNotFound:
            return BLEManagerError.genericErr(txt: NSLocalizedString("id_login_failed", comment: ""))
        default:
            return BLEManagerError.genericErr(txt: err.localizedDescription)
        }
    }

    func toErrorString(_ error: BLEManagerError) -> String {
        switch error {
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
        case .firmwareErr(txt: let txt):
            return txt
        case .unauthorized(txt: let txt):
            return txt
        }
    }

    func connecting(_ peripheral: Peripheral) -> Observable<Bool> {
        return Observable.just(peripheral)
            .flatMap { $0.isConnected ? Observable.just($0) : $0.establishConnection() }
            .timeoutIfNoEvent(RxTimeInterval.seconds(10))
            .flatMap { $0.isJade() ? self.connectJade($0) : self.connectLedger($0) }
            .timeoutIfNoEvent(RxTimeInterval.seconds(10))
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

extension BLEManager: JadeGdkRequest {
    func httpRequest(params: [String: Any]) -> [String: Any]? {
        return try? self.session?.session?.httpRequest(params: params)
    }
}

extension Observable {
    func timeoutIfNoEvent(_ dueTime: RxTimeInterval) -> Observable<Element> {
        let timeout = Observable
            .never()
            .timeout(dueTime, scheduler: MainScheduler.instance)

        return self.amb(timeout)
    }
}
