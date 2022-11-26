import Foundation
import PromiseKit
import RxSwift
import RxBluetoothKit
import CoreBluetooth

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

protocol BLEManagerScanDelegate: AnyObject {
    func didUpdatePeripherals(_: [Peripheral])
    func onError(_: BLEManagerError)
}

protocol BLEManagerDelegate: AnyObject {
    func onPrepare(_: Peripheral, reset: Bool)
    func onAuthenticate(_: Peripheral, network: GdkNetwork, firstInitialization: Bool)
    func onLogin(_: Peripheral, account: Account)
    func onError(_: BLEManagerError)
    func onConnectivityChange(peripheral: Peripheral, status: Bool)
    func onCheckFirmware(_: Peripheral, fmw: Firmware, currentVersion: String, needCableUpdate: Bool)
    func onUpdateFirmware(_: Peripheral, version: String, prevVersion: String)
    func onComputedHash(_ hash: String)
}

class BLEManager {

    private static var instance: BLEManager?
    static var shared: BLEManager {
        guard let instance = instance else {
            self.instance = BLEManager()
            return self.instance!
        }
        return instance
    }

    let manager: CentralManager
    let queue = DispatchQueue(label: "manager.queue")

    let timeout = RxTimeInterval.seconds(10)
    var peripherals = [Peripheral]()

    var scanningDispose: Disposable?
    var enstablishDispose: Disposable?
    var statusDispose: Disposable?

    weak var delegate: BLEManagerDelegate?
    weak var scanDelegate: BLEManagerScanDelegate?
    var fmwVersion: String?
    var boardType: String?
    private var session: SessionManager?

    init() {
        manager = CentralManager(queue: queue, options: nil)
        Jade.shared.gdkRequestDelegate = self
    }

    func start() {
        if manager.state == .poweredOff {
            let err = BLEManagerError.powerOff(txt: NSLocalizedString("id_turn_on_bluetooth_to_connect", comment: ""))
            self.scanDelegate?.onError(err)
        } else if manager.state == .unauthorized {
            let err = BLEManagerError.unauthorized(txt: NSLocalizedString("id_give_bluetooth_permissions", comment: ""))
            self.scanDelegate?.onError(err)
        }

        // wait bluetooth is ready
        let sheduler = SerialDispatchQueueScheduler(queue: queue, internalSerialQueueName: "manager.sheduler")
        scanningDispose = manager.observeState()
            .startWith(self.manager.state)
            .filter { $0 == .poweredOn }
            .subscribeOn(sheduler)
            .subscribe(onNext: { _ in
                self.scanningDispose = self.scan()
            }, onError: { err in
                let err = BLEManagerError.notReady(txt: err.localizedDescription)
                self.scanDelegate?.onError(err)
            })
    }

    func scan() -> Disposable {
        peripherals = manager.retrieveConnectedPeripherals(withServices: [JadeChannel.SERVICE_UUID, LedgerChannel.SERVICE_UUID])
            .filter { self.isJade($0) || self.isLedger($0) }
        self.scanDelegate?.didUpdatePeripherals(self.peripherals)
        return manager.scanForPeripherals(withServices: [JadeChannel.SERVICE_UUID, LedgerChannel.SERVICE_UUID])
            .filter { self.isJade($0.peripheral) || self.isLedger($0.peripheral) }
            .subscribeOn(MainScheduler.instance)
            .subscribe(onNext: { p in
                if let row = self.peripherals.firstIndex(where: { $0.name == p.advertisementData.localName }) {
                    self.peripherals[row] = p.peripheral
                } else {
                    self.peripherals += [p.peripheral]
                }
                self.scanDelegate?.didUpdatePeripherals(self.peripherals)
            }, onError: { error in
                let err = BLEManagerError.scanErr(txt: error.localizedDescription)
                self.scanDelegate?.onError(err)
            })
    }

    func dispose() {
        disposeScan()
        statusDispose?.dispose()
        enstablishDispose?.dispose()
    }

    func disposeScan() {
        peripherals = []
        scanningDispose?.dispose()
        if manager.state == .poweredOn {
            manager.manager.stopScan()
        }
    }

    func isLedger(_ p: Peripheral) -> Bool {
        p.peripheral.name?.contains("Nano") ?? false
    }

    func isJade(_ p: Peripheral) -> Bool {
        p.peripheral.name?.contains("Jade") ?? false
    }

    func connectLedger(_ p: Peripheral, network: GdkNetwork) -> Observable<Bool> {
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p in Ledger.shared.open(p) }
            .flatMap { _ in Ledger.shared.application() }
            .compactMap { res in
                let name = res["name"] as? String ?? ""
                let versionString = res["version"] as? String ?? ""
                let version = versionString.split(separator: ".").map {Int($0)}
                if name.contains("OLOS") {
                    throw DeviceError.dashboard // open app from dashboard
                } else if name != self.networkLabel(network.network) {
                    throw DeviceError.wrong_app // change app
                } else if name == "Bitcoin" && version[0] ?? 0 < 1 {
                    throw DeviceError.outdated_app
                }
                return false
            }
    }

    func connectJade(_ p: Peripheral) -> Observable<Bool> {
        var hasPin = false
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p in Jade.shared.open(p) }
            .flatMap { _ in Jade.shared.addEntropy() }
            .flatMap { _ in Jade.shared.version() }
            .flatMap { version -> Observable<Bool> in
                hasPin = version.jadeHasPin
                self.fmwVersion = version.jadeVersion
                self.boardType = version.boardType
                let networkType: NetworkSecurityCase = version.jadeNetworks == "TEST" ? .testnetSS : .bitcoinSS
                let chain = networkType.chain
                switch version.jadeState {
                case "READY":
                    return Observable.just(true)
                case "TEMP":
                    return Jade.shared.unlock(network: chain)
                default:
                    return Jade.shared.auth(network: chain)
                            .retry(3)
                }
            }.compactMap { _ in return hasPin }
    }

    func connect(_ p: Peripheral, network: GdkNetwork? = nil) {
        addStatusListener(p)
        let gdkNetwork = network ?? getGdkNetwork("electrum-mainnet")
        session = SessionManager(gdkNetwork)
        var connection = Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
        if !p.isConnected {
            connection = connection
                .flatMap { $0.establishConnection() }
        }
        enstablishDispose = connection
            .compactMap { _ in try? self.session?.connect().wait() }
            .flatMap { self.isJade(p) ? self.connectJade(p) : self.connectLedger(p, network: gdkNetwork) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { hasPin in
                self.delegate?.onAuthenticate(p, network: gdkNetwork, firstInitialization: !hasPin)
            }, onError: { err in
                self.onError(err, network: "")
            })
    }

/*
    func connectJade(_ p: Peripheral, network: GdkNetwork) {
        let session = SessionManager(network)
        self.session = session
        var hasPin = false
        var connection = Observable.just(p)
        if !p.isConnected {
            connection = p.establishConnection()
        }
        enstablishDispose = connection
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { Jade.shared.open($0) }
            .compactMap { _ in try session.connect().wait() }
            .timeoutIfNoEvent(RxTimeInterval.seconds(3))
            .flatMap { Jade.shared.addEntropy() }
            .flatMap { _ -> Observable<JadeVersionInfo> in
                Jade.shared.version()
            }.flatMap { version -> Observable<Bool> in
                hasPin = version.jadeHasPin
                self.fmwVersion = version.jadeVersion
                self.boardType = version.boardType
                let testnet = ["testnet", "testnet-liquid"].contains(network.chain)
                if version.jadeNetworks == "TEST" && !testnet {
                    throw JadeError.Abort("\(network.name) not supported in Jade \(version.jadeNetworks) mode")
                } else if version.jadeNetworks == "MAIN" && testnet {
                    throw JadeError.Abort("\(network.name) not supported in Jade \(version.jadeNetworks) mode")
                }
                // JADE_STATE => READY  (device unlocked / ready to use)
                // anything else ( LOCKED | UNSAVED | UNINIT | TEMP) will need an authUser first to unlock
                switch version.jadeState {
                case "READY":
                    return Observable.just(true)
                case "TEMP":
                    return Jade.shared.unlock(network: network.chain)
                default:
                    return Jade.shared.auth(network: network.chain)
                        .retry(3)
                }
            }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onAuthenticate(p, network: network, firstInitialization: !hasPin)
            }, onError: { err in
                self.onError(err, network: network.name)
            })
    }*/

    func checkFirmware(_ p: Peripheral) -> Observable<Peripheral> {
        var verInfo: JadeVersionInfo?
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in
                Jade.shared.version()
            }.compactMap { info in
                verInfo = info
                return try Jade.shared.firmwareData(info)
            }
            .catchErrorJustReturn(nil)
            .observeOn(MainScheduler.instance)
            .compactMap { (fmw: Firmware?) in
                self.fmwVersion = verInfo?.jadeVersion
                if let ver = verInfo?.jadeVersion, let fmw = fmw {
                    let needCableUpdate = ver == Jade.BOARD_TYPE_JADE_V1_1 && ver < "0.1.28"
                    self.delegate?.onCheckFirmware(p, fmw: fmw, currentVersion: ver, needCableUpdate: needCableUpdate)
                    throw BLEManagerError.firmwareErr(txt: "")
                }
                return p
            }
    }

    func updateFirmware(_ p: Peripheral, fmw: Firmware, currentVersion: String) {
        _ = Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Jade.shared.version() }
            .flatMap {  Jade.shared.updateFirmware($0, fmw) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.enstablishDispose?.dispose()
                self.delegate?.onUpdateFirmware(p, version: fmw.version, prevVersion: currentVersion)
            }, onError: { err in
                self.onError(err, network: nil)
            })
    }

    func onBinaryFetched(hash: String) {
        DispatchQueue.main.async {
            self.delegate?.onComputedHash(hash)
        }
    }

    func device(isJade: Bool, fmwVersion: String) -> HWDevice {
        if !isJade {
            return HWDevice(name: "Ledger",
                     supportsArbitraryScripts: true,
                     supportsLowR: false,
                     supportsLiquid: 0,
                     supportsAntiExfilProtocol: 0,
                     supportsHostUnblinding: false)
        }
        // Host Unblinding enabled by default on 0.1.27
        let supportUnblinding = fmwVersion >= "0.1.27"
        return HWDevice(name: "Jade",
                         supportsArbitraryScripts: true,
                         supportsLowR: true,
                         supportsLiquid: 1,
                         supportsAntiExfilProtocol: 1,
                         supportsHostUnblinding: supportUnblinding)
    }

    func loginJade(_ p: Peripheral) {
        _ = Observable.just(p)
            .flatMap { _ in Jade.shared.version() }
            .compactMap { $0.jadeNetworks == "TEST" ? .testnetSS : .bitcoinSS }
            .compactMap { WalletManager(prominentNetwork: $0) }
            .flatMap { wm -> Observable<WalletManager> in
                let device = self.device(isJade: self.isJade(p),
                                        fmwVersion: self.fmwVersion ?? "")
                return Observable<WalletManager>.create { observer in
                    wm.loginWithHW(device)
                        .done { _ in
                            observer.onNext(wm)
                            observer.onCompleted()
                        }.catch { err in
                            observer.onError(err)
                        }
                    return Disposables.create { }
                }
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { wm in
                let network = wm.prominentNetwork
                let account = Account(name: p.name ?? "HW",
                                     network: network.chain,
                                     isJade: true,
                                     isLedger: false,
                                     isSingleSig: network.gdkNetwork?.electrum ?? true)
                AccountsManager.shared.current = account
                WalletManager.add(for: account, wm: wm)
                self.delegate?.onLogin(p, account: account)
            }, onError: { err in
                switch err {
                case BLEManagerError.firmwareErr(_): // nothing to do
                    return
                default:
                    self.onError(err, network: nil)
                }
            })
    }

    func login(_ p: Peripheral, network: GdkNetwork, checkFirmware: Bool = true) {
        let session = SessionManager(network)
        _ = Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p -> Observable<Peripheral> in
                if checkFirmware && self.isJade(p) {
                    return self.checkFirmware(p)
                } else {
                    return Observable.just(p)
                }
            }
            .flatMap { _ in
                return Observable<String>.create { observer in
                    let device = self.device(isJade: self.isJade(p), fmwVersion: self.fmwVersion ?? "")
                    session.loginWithHW(device)
                        .done { res in
                            observer.onNext(res)
                            observer.onCompleted()
                        }.catch { err in
                            observer.onError(err)
                        }
                    return Disposables.create { }
                }
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { walletHashId in
                // update previously account if exist
                let storedAccount = AccountsManager.shared.accounts.filter {
                    $0.isHW && $0.walletHashId == walletHashId
                }.first
                var account = storedAccount ??
                    Account(name: p.name ?? "HW",
                            network: network.chain,
                            isJade: BLEManager.shared.isJade(p),
                            isLedger: BLEManager.shared.isLedger(p),
                            isSingleSig: network.network.contains("electrum"))
                let firstLogin = account.walletHashId == nil
                account.walletHashId = walletHashId
                session
                    .load(refreshSubaccounts: firstLogin)
                    .done {
                        WalletManager.delete(for: account)
                        AccountsManager.shared.current = account
                        self.delegate?.onLogin(p, account: account)
                    }.catch { _ in
                        self.onError(LoginError.connectionFailed(), network: nil)
                    }
            }, onError: { err in
                switch err {
                case BLEManagerError.firmwareErr(_): // nothing to do
                    return
                default:
                    self.onError(err, network: nil)
                }
            })
    }

    func onError(_ err: Error, network: String?) {

        var bleErr: BLEManagerError?

        switch err {
        case BluetoothError.peripheralConnectionFailed(_, let error):
            bleErr = BLEManagerError.bleErr(txt: error?.localizedDescription ?? err.localizedDescription)
        case is BluetoothError, is GaError:
            bleErr = BLEManagerError.bleErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: ""))
        case RxError.timeout:
            bleErr = BLEManagerError.timeoutErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: ""))
        case DeviceError.dashboard:
            bleErr = BLEManagerError.dashboardErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
        case DeviceError.outdated_app:
            bleErr = BLEManagerError.outdatedAppErr(txt: "Outdated Ledger app: update the bitcoin app via Ledger Manager")
        case DeviceError.wrong_app:
            bleErr = BLEManagerError.wrongAppErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.networkLabel(network ?? "mainnet")))
        case is AuthenticationTypeHandler.AuthError:
            let authErr = err as? AuthenticationTypeHandler.AuthError
            bleErr = BLEManagerError.authErr(txt: authErr?.localizedDescription ?? "")

            AnalyticsManager.shared.failedWalletLogin(account: AccountsManager.shared.current, error: err, prettyError: authErr?.localizedDescription ?? "")
        case is Ledger.SWError:
            bleErr = BLEManagerError.swErr(txt: NSLocalizedString("id_invalid_status_check_that_your", comment: ""))
        case is JadeError:
            switch err {
            case JadeError.Abort(let desc),
                 JadeError.URLError(let desc),
                 JadeError.Declined(let desc):
                bleErr = BLEManagerError.genericErr(txt: desc)
            default:
                bleErr = BLEManagerError.authErr(txt: NSLocalizedString("id_login_failed", comment: ""))

                AnalyticsManager.shared.failedWalletLogin(account: AccountsManager.shared.current, error: err, prettyError: "id_login_failed")
            }
        default:
            bleErr = BLEManagerError.genericErr(txt: err.localizedDescription)
        }

        if let bleErr = bleErr {
            self.delegate?.onError(bleErr)
        }
    }

    func prepare(_ peripheral: Peripheral) {
        if peripheral.isConnected {
            self.delegate?.onPrepare(peripheral, reset: false)
            return
        } else if isLedger(peripheral) {
            self.delegate?.onPrepare(peripheral, reset: false)
            return
        }

        // dummy 1st connection for jade
        enstablishDispose = Observable.just(peripheral)
            .timeoutIfNoEvent(RxTimeInterval.seconds(20))
            .flatMap { $0.establishConnection() }
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { _ in sleep(3) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onPrepare(peripheral, reset: true)
            }, onError: { err in
                self.onError(err, network: nil)
        })
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

    func addStatusListener(_ peripheral: Peripheral) {
        statusDispose?.dispose()
        statusDispose = peripheral.observeConnection()
            .subscribe(onNext: { status in
                self.delegate?.onConnectivityChange(peripheral: peripheral, status: status)
                if status == false {
                    DispatchQueue.main.async {
                        DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                    }
                }
            }, onError: { _ in
            })
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
