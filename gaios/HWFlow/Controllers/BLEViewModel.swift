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

    func scan(jade: Bool,
              completion: @escaping([Peripheral]) -> Void,
              error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        scanDispose = BLEManager.shared.scanning()
            .observeOn(MainScheduler.instance)
            .compactMap { $0.filter { ($0.isJade() && jade)  || ($0.isLedger() || !jade) }}
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }

    func scan(uuid: UUID,
              completion: @escaping(Peripheral) -> Void,
              error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        scanDispose = BLEManager.shared.scanning()
            .observeOn(MainScheduler.instance)
            .compactMap { $0.filter { $0.identifier == uuid }.first }
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }

    func login(account: Account,
              peripheral: Peripheral?,
              completion: @escaping() -> Void,
              error: @escaping(Error) -> Void) {
        guard let peripheral = peripheral else {
            error(BLEManagerError.genericErr(txt: "No device found"))
            return
        }
        scanDispose?.dispose()
        connectDispose = BLEManager.shared.preparing(peripheral)
            .flatMap { _ in BLEManager.shared.connecting(peripheral) }
            .flatMap { _ in BLEManager.shared.authenticating(peripheral) }
            .flatMap { _ in BLEManager.shared.logging(peripheral, account: account) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in completion() },
                       onError: { error($0) })
    }

    func pairing(_ peripheral: Peripheral,
                 completion: @escaping(Peripheral) -> Void,
                 error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        pairDispose = BLEManager.shared.preparing(peripheral)
            .flatMap { $0.isLedger() ? BLEManager.shared.connecting(peripheral) : Observable.just(true) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in completion(peripheral) },
                       onError: { error($0) })
    }

    func connecting(_ peripheral: Peripheral,
                    completion: @escaping(Bool) -> Void,
                    error: @escaping(Error) -> Void) {
        scanDispose?.dispose()
        if peripheral.isJade() {
            pairDispose?.dispose()
        }
        connectDispose = BLEManager.shared.connecting(peripheral)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { completion($0) },
                       onError: { error($0) })
    }
}
