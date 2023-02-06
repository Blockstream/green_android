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
    func onPrepared(_: Peripheral)
    func onConnected(_: Peripheral, firstInitialization: Bool)
    func onAuthenticated(_: Peripheral)
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
    var prepareDispose: Disposable?
    var connectDispose: Disposable?
    var statusDispose: Disposable?

    weak var delegate: BLEManagerDelegate?
    weak var scanDelegate: BLEManagerScanDelegate?
    var fmwVersion: String?
    var boardType: String?
    var device: HWDevice?
    private var session: SessionManager?

    init() {
        manager = CentralManager(queue: queue, options: nil)
        Jade.shared.gdkRequestDelegate = self
        RxBluetoothKitLog.setLogLevel(.debug)
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
        prepareDispose?.dispose()
        connectDispose?.dispose()
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

    func connectLedger(_ p: Peripheral) -> Observable<Bool> {
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p in Ledger.shared.open(p) }
            .flatMap { _ in self.getLedgerNetwork() }
            .compactMap { self.session = SessionManager(getGdkNetwork($0.network)) }
            .compactMap { try? self.session?.connect().wait() }
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
                let network = name.lowercased() == "bitcoin test" ? "testnet" : "mainnet"
                return NetworkSecurityCase(rawValue: network)
            }
    }

    func getMasterXpub(_ device: HWDevice, wm: WalletManager) -> Observable<String> {
        let network = wm.prominentNetwork.gdkNetwork?.chain ?? "mainnet"
        if device.isJade {
            return Jade.shared.xpubs(network: network, path: [])
        }
        return getLedgerNetwork()
            .flatMap { Ledger.shared.xpubs(network: $0.network, path: []) }
    }

    func connectJade(_ p: Peripheral) -> Observable<Bool> {
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p in Jade.shared.open(p) }
            .flatMap { _ in Jade.shared.addEntropy() }
            .flatMap { _ in Jade.shared.version() }
            .compactMap { $0.jadeHasPin }
    }

    func auth(_ p: Peripheral, testnet: Bool? = nil) {
        if isLedger(p) {
            delegate?.onAuthenticated(p)
            return
        }
        _ = Observable.just(p)
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
            }.subscribeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onAuthenticated(p)
            }, onError: { error in
                self.onError(error, network: "")
            })
    }

    func connect(_ p: Peripheral) {
        addStatusListener(p)
        var connection = Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
        if !p.isConnected {
            connection = connection
                .flatMap { $0.establishConnection() }
        }
        connectDispose = connection
            .flatMap { _ in self.isJade(p) ? self.connectJade(p) : self.connectLedger(p) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { hasPin in
                self.delegate?.onConnected(p, firstInitialization: !hasPin)
            }, onError: { err in
                self.onError(err, network: "")
            })
    }

    func checkFirmware(_ p: Peripheral) -> Observable<Peripheral> {
        var verInfo: JadeVersionInfo?
        return Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Jade.shared.version() }
            .compactMap { info in
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
                self.prepareDispose?.dispose()
                self.connectDispose?.dispose()
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

    func login(_ p: Peripheral) {
        isJade(p) ? loginJade(p) : loginLedger(p)
    }

    func loginJade(_ p: Peripheral, checkFirmware: Bool = true) {
        self.device = HWDevice.defaultJade(fmwVersion: self.fmwVersion ?? "")
        _ = Observable.just(p)
            .flatMap { checkFirmware ? self.checkFirmware($0) : Observable.just($0) }
            .flatMap { _ in Jade.shared.version() }
            .compactMap { $0.jadeNetworks == "TEST" ? .testnetSS : .bitcoinSS }
            .flatMap { self.loginDevice(network: $0, device: self.device!) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: {
                self.delegate?.onLogin(p, account: $0)
            }, onError: { err in
                switch err {
                case BLEManagerError.firmwareErr(_): // nothing to do
                    return
                default:
                    self.onError(err, network: nil)
                }
            })
    }

    private func loginDevice(network: NetworkSecurityCase, device: HWDevice) -> Observable<Account> {
        let wm = WalletManager(prominentNetwork: network)
        return getMasterXpub(device, wm: wm)
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
                let network = wm.prominentNetwork
                let account = Account(name: device.name,
                                     network: network.chain,
                                      isJade: device.isJade,
                                     isLedger: device.isLedger,
                                     isSingleSig: network.gdkNetwork?.electrum ?? true)
                AccountsManager.shared.current = account
                WalletManager.add(for: account, wm: wm)
                return account
            }
    }

    func loginLedger(_ p: Peripheral) {
        self.device = HWDevice.defaultLedger()
        _ = Observable.just(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in self.getLedgerNetwork() }
            .flatMap { self.loginDevice(network: $0, device: self.device!) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: {
                self.delegate?.onLogin(p, account: $0)
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
            self.delegate?.onPrepared(peripheral)
            return
        } else if isLedger(peripheral) {
            self.delegate?.onPrepared(peripheral)
            return
        }

        // dummy 1st connection for jade
        prepareDispose = Observable.just(peripheral)
            .timeoutIfNoEvent(RxTimeInterval.seconds(20))
            .flatMap { $0.establishConnection() }
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { _ in sleep(3) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onPrepared(peripheral)
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
