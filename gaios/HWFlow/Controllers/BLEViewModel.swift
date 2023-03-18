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

    func pairing(_ peripheral: Peripheral,
                 completion: @escaping(Peripheral) -> Void,
                 error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        pairDispose = peripheral.establishConnection()
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in completion(peripheral) },
                       onError: { error($0) })
    }

    func connecting(_ peripheral: Peripheral,
                    completion: @escaping(Bool) -> Void,
                    error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        pairDispose?.dispose()
        connectDispose = BLEManager.shared.connecting(peripheral)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }
}
