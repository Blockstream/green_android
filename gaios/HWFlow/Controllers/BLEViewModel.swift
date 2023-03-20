import Foundation
import RxBluetoothKit
import RxSwift

class BLEViewModel {

    static var shared = BLEViewModel()

    var scanDispose: Disposable?
    var pairDispose: Disposable?
    var connectDispose: Disposable?

    func isReady() throws {
        if BLEManager.shared.manager.state == .poweredOff {
            throw BLEManagerError.genericErr(txt: "id_turn_on_bluetooth_to_connect".localized)
        } else if BLEManager.shared.manager.state == .unauthorized {
            throw BLEManagerError.genericErr(txt: "id_give_bluetooth_permissions".localized)
        }
    }

    func dispose() {
        scanDispose?.dispose()
        pairDispose?.dispose()
        connectDispose?.dispose()
    }

    func scan(jade: Bool,
              completion: @escaping([Peripheral]) -> Void,
              error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        scanDispose = BLEManager.shared.scanning()
            .observeOn(MainScheduler.instance)
            .compactMap {
                $0.filter { ($0.isJade() && jade)  || ($0.isLedger() && !jade) }}
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }

    func login(account: Account,
               peripheral: Peripheral,
               progress: @escaping(String) -> Void,
               completion: @escaping() -> Void,
               error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        progress("id_connecting".localized)
        connectDispose = BLEManager.shared.connecting(peripheral)
            .observeOn(MainScheduler.instance)
            .do(onNext: { _ in progress("id_unlock_jade_to_continue".localized) })
            .flatMap { _ in BLEManager.shared.authenticating(peripheral) }
            .observeOn(MainScheduler.instance)
            .do(onNext: { _ in progress("id_logging_in".localized) })
            .flatMap { _ in BLEManager.shared.logging(peripheral, account: account) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in completion() },
                       onError: { error($0) })
    }

    func initialize(
        peripheral: Peripheral,
        testnet: Bool,
        progress: @escaping(String) -> Void,
        completion: @escaping(WalletManager) -> Void,
        error: @escaping(Error) -> Void) {
            scanDispose?.dispose()
            progress("id_connecting".localized)
            connectDispose = BLEManager.shared.connecting(peripheral)
                .observeOn(MainScheduler.instance)
                .do(onNext: { _ in progress("id_unlock_jade_to_continue".localized) })
                .flatMap { _ in BLEManager.shared.authenticating(peripheral, testnet: testnet) }
                .observeOn(MainScheduler.instance)
                .do(onNext: { _ in progress("id_logging_in".localized) })
                .flatMap { _ in BLEManager.shared.account(peripheral) }
                .flatMap { BLEManager.shared.logging(peripheral, account: $0) }
                .observeOn(MainScheduler.instance)
                .subscribe(onNext: { completion($0) },
                           onError: { error($0) })
        }

    func pairing(_ peripheral: Peripheral,
                 completion: @escaping(Peripheral) -> Void,
                 error: @escaping(Error) -> Void) {
        dispose()
        pairDispose = peripheral.establishConnection()
            .flatMap { _ in peripheral.isLedger() ? BLEManager.shared.connecting(peripheral) : Observable.just(true) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in completion(peripheral) },
                       onError: { error($0) })
    }

    func connecting(_ peripheral: Peripheral,
                    completion: @escaping(Bool) -> Void,
                    error: @escaping(Error) -> Void) {
        dispose()
        connectDispose = BLEManager.shared.connecting(peripheral)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }

    func checkFirmware(_ peripheral: Peripheral) -> Observable<(String?, Firmware?)> {
        return Jade.shared.version()
            .compactMap { ($0, try Jade.shared.firmwareData($0)) }
            .observeOn(MainScheduler.instance)
            .compactMap { ($0.0.jadeVersion, $0.1) }
    }

    func updateFirmware(peripheral: Peripheral,
                        firmware: Firmware,
                        progress: @escaping(String) -> Void,
                        completion: @escaping(Bool) -> Void,
                        error: @escaping(Error) -> Void) {
        Observable.just(peripheral)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Jade.shared.version() }
            .observeOn(MainScheduler.instance)
            .do(onNext: { _ in progress("id_fetching_new_firmware".localized) })
            .compactMap { ($0, firmware, try Jade.shared.getBinary($0, firmware)) }
            .do(onNext: { progress("id_updating_firmware".localized + "\n\nHash: \(self.hash($0.2))") })
            .flatMap { Jade.shared.updateFirmware(version: $0.0, firmware: $0.1, binary: $0.2) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }

    func hash(_ binary: Data) -> String {
        let hash = Jade.shared.sha256(binary)
        return "\(hash.map { String(format: "%02hhx", $0) }.joined())"
    }
}
