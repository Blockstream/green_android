//
//  BLEManager.swift
//  gaios
//
//  Created by Mauro Olivo on 09/02/21.
//  Copyright © 2021 Blockstream Corporation. All rights reserved.
//

import Foundation
import PromiseKit
import RxSwift
import RxBluetoothKit

enum BLEManagerError: Error {
    case powerOff(txt: String)
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
}

enum DeviceError: Error {
    case dashboard
    case wrong_app
    case outdated_app
}

protocol BLEManagerDelegate: class {
    func didUpdatePeripherals(_: [ScannedPeripheral])
    func onConnect(_: Peripheral)
    func onPrepare(_: Peripheral)
    func onError(_: BLEManagerError)
    func onConnectivityChange(peripheral: Peripheral, status: Bool)
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

    static let manager = CentralManager(queue: .main)

    let timeout = RxTimeInterval.seconds(10)
    var peripherals = [ScannedPeripheral]()

    var scanningDispose: Disposable?
    var enstablishDispose: Disposable?
    var statusDispose: Disposable?

    weak var delegate: BLEManagerDelegate?

    func start() {
        let manager = BLEManager.manager
        switch manager.state {
        case .poweredOn:
            scanningDispose = scan()
            return
        case .poweredOff:
            let err = BLEManagerError.powerOff(txt: NSLocalizedString("id_turn_on_bluetooth_to_connect", comment: ""))
            self.delegate?.onError(err)
        default:
            break
        }

        // wait bluetooth is ready
        scanningDispose = manager.observeState()
            .filter { $0 == .poweredOn }
            .take(1)
            .subscribe(onNext: { _ in
                self.scanningDispose = self.scan()
            }, onError: { err in
                let err = BLEManagerError.notReady(txt: err.localizedDescription)
                self.delegate?.onError(err)
            })
    }

    func scan() -> Disposable {
        return BLEManager.manager.scanForPeripherals(withServices: nil)
            .filter { self.isJade($0.peripheral) || self.isLedger($0.peripheral) }
            .subscribe(onNext: { p in
                self.peripherals.removeAll { $0.rssi == p.rssi }
                self.peripherals.append(p)
                self.delegate?.didUpdatePeripherals(self.peripherals)
            }, onError: { error in
                let err = BLEManagerError.scanErr(txt: error.localizedDescription)
                self.delegate?.onError(err)
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
        BLEManager.manager.manager.stopScan()
    }

    func isLedger(_ p: Peripheral) -> Bool {
        p.peripheral.name?.contains("Nano") ?? false
    }

    func isJade(_ p: Peripheral) -> Bool {
        p.peripheral.name?.contains("Jade") ?? false
    }

    func connectLedger(_ p: Peripheral) -> Observable<Peripheral> {
        HWResolver.shared.hw = Ledger.shared
        return Ledger.shared.open(p)
            .flatMap { _ in Ledger.shared.application() }
            .compactMap { res in
                let name = res["name"] as? String ?? ""
                let versionString = res["version"] as? String ?? ""
                let version = versionString.split(separator: ".").map {Int($0)}
                if name.contains("OLOS") {
                    throw DeviceError.dashboard // open app from dashboard
                } else if name != self.network() {
                    throw DeviceError.wrong_app // change app
                } else if name == "Bitcoin" && (version[0]! < 1 || version[1]! < 4) {
                    throw DeviceError.outdated_app
                }
                return p
            }
    }

    func connectJade(_ p: Peripheral) -> Observable<Peripheral> {
        HWResolver.shared.hw = Jade.shared
        return Jade.shared.open(p)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in
                Jade.shared.version()
            }.flatMap { _ -> Observable<[String: Any]> in
                // let hasPin = version["JADE_HAS_PIN"] as? Bool
                // check genuine firmware
                return Jade.shared.addEntropy()
            }.flatMap { _ in
                Jade.shared.auth(network: getNetwork())
                    .retry(3)
            }.compactMap { _ in
                return p
            }
    }

    func connect(_ p: Peripheral) {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let session = getGAService().getSession()

        addStatusListener(p)

        enstablishDispose = p.establishConnection()
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { p in
                appDelegate?.disconnect()
                try appDelegate?.connect()
                return p
            }.flatMap { p in
                self.isJade(p) ? self.connectJade(p) : self.connectLedger(p)
            }.observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in
                return Observable<[String: Any]>.create { observer in
                    let info = HWResolver.shared.hw!.info
                    _ = try? session.registerUser(mnemonic: "", hw_device: ["device": info]).resolve()
                        .then { _ in
                            try session.login(mnemonic: "", hw_device: ["device": info]).resolve()
                        }.get { _ in
                            Registry.shared.refresh().recover { _ in Guarantee() }
                        }.done { res in
                            observer.onNext(res)
                            observer.onCompleted()
                        }.catch { err in
                            observer.onError(err)
                        }
                    return Disposables.create { }
                }
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onConnect(p)
            }, onError: { err in
                switch err {
                case is BluetoothError:
                    let bleErr = err as? BluetoothError
                    let err = BLEManagerError.bleErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: "") + ": \(bleErr?.localizedDescription ?? "")")
                    self.delegate?.onError(err)
                case RxError.timeout:
                    let err = BLEManagerError.timeoutErr(txt: NSLocalizedString("id_communication_timed_out_make", comment: ""))
                    self.delegate?.onError(err)
                case DeviceError.dashboard:
                    let err = BLEManagerError.dashboardErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                    self.delegate?.onError(err)
                case DeviceError.outdated_app:
                    let err = BLEManagerError.outdatedAppErr(txt: "Outdated Ledger app: update the bitcoin app via Ledger Manager")
                    self.delegate?.onError(err)
                case DeviceError.wrong_app:
                    let err = BLEManagerError.wrongAppErr(txt: String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                    self.delegate?.onError(err)
                case is AuthenticationTypeHandler.AuthError:
                    let authErr = err as? AuthenticationTypeHandler.AuthError
                    let err = BLEManagerError.authErr(txt: authErr?.localizedDescription ?? "")
                    self.delegate?.onError(err)
                case is Ledger.SWError:
                    let err = BLEManagerError.swErr(txt: NSLocalizedString("id_invalid_status_check_that_your", comment: ""))
                    self.delegate?.onError(err)
                case is JadeError:
                    switch err {
                    case JadeError.Abort(let txt), JadeError.Declined(let txt), JadeError.URLError(let txt):
                        let err = BLEManagerError.authErr(txt: txt)
                        self.delegate?.onError(err)
                    default:
                        let err = BLEManagerError.authErr(txt: NSLocalizedString("id_login_failed", comment: ""))
                        self.delegate?.onError(err)
                    }
                default:
                    let err = BLEManagerError.genericErr(txt: err.localizedDescription)
                    self.delegate?.onError(err)
                }
            })
    }

    func prepare(_ peripheral: Peripheral) {
        if peripheral.isConnected {
            self.delegate?.onPrepare(peripheral)
            return
        } else if isLedger(peripheral) {
            self.delegate?.onPrepare(peripheral)
            return
        }

        // dummy 1st connection for jade
        enstablishDispose = peripheral.establishConnection().subscribe()
        _ = BLEManager.manager.observeConnect(for: peripheral).take(1)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { _ in sleep(2) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.delegate?.onPrepare(peripheral)
        })
    }

    func network() -> String {
        return getGdkNetwork(getNetwork()).network.lowercased() == "testnet" ? "Bitcoin Test" : "Bitcoin"
    }

    func addStatusListener(_ peripheral: Peripheral) {
        statusDispose?.dispose()
        statusDispose = peripheral.observeConnection()
            .subscribe(onNext: { status in
                self.delegate?.onConnectivityChange(peripheral: peripheral, status: status)
                if status == false {
                    DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                }
            }, onError: { _ in
            })
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
