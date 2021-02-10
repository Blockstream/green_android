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
    func onConnect()
    func onError(_: BLEManagerError)
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

    let timeout = RxTimeInterval.seconds(10)
    var peripherals = [ScannedPeripheral]()

    var scanningDispose: Disposable?
    var enstablishDispose: Disposable?

    weak var delegate: BLEManagerDelegate?

    func start() {
        let manager = AppDelegate.manager
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
        return AppDelegate.manager.scanForPeripherals(withServices: nil)
            .filter { $0.peripheral.name?.contains("Nano") ?? false }
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
        peripherals = []
        scanningDispose?.dispose()
        AppDelegate.manager.manager.stopScan()
        enstablishDispose?.dispose()
    }

    func connect(peripheral: Peripheral) {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let session = getGAService().getSession()

        enstablishDispose = peripheral.establishConnection()
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { Ledger.shared.open($0) }
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
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
            }.observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { _ in
                appDelegate?.disconnect()
                try appDelegate?.connect()
                _ = try session.registerUser(mnemonic: "", hw_device: ["device": (Ledger.shared.hwDevice as Any) ]).resolve().wait()
                _ = try session.login(mnemonic: "", hw_device: ["device": Ledger.shared.hwDevice]).resolve().wait()
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in

                self.delegate?.onConnect()
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
                default:
                    let err = BLEManagerError.genericErr(txt: err.localizedDescription)
                    self.delegate?.onError(err)
                }
            })
    }

    func network() -> String {
        return getGdkNetwork(getNetwork()).network.lowercased() == "testnet" ? "Bitcoin Test" : "Bitcoin"
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
