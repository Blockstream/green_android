import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import SwiftCBOR

class JadeChannel: HWChannelProtocol {

    var peripheral: Peripheral!
    var characteristicWrite: Characteristic?
    var characteristicNotify: Characteristic?

    static let SERVICE_UUID = CBUUID(string: "6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    static let WRITE_CHARACTERISTIC_UUID = CBUUID(string: "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    static let CLIENT_CHARACTERISTIC_CONFIG = CBUUID(string: "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    var MTU: UInt8 = 128
    var TIMEOUT: Int = 30

    func open(_ peripheral: Peripheral) -> Observable<Data> {
        self.peripheral = peripheral
        print("Max MTU supported: \(peripheral.maximumWriteValueLength(for: .withResponse))")
        return Observable.concat(setupWrite(), setupNotify()).reduce([], accumulator: { result, element in
                return result + [element]
        }).flatMap { _ in
            return Observable.just(Data())
        }
    }

    func close() -> Observable<Data> {
        return Observable.from(optional: nil)
    }

    func isOpened() -> Bool {
        return true
    }

    func exchange<T: Decodable, K: Decodable>(_ request: JadeRequest<T>) -> Observable<JadeResponse<K>> {
        return exchange(request.encoded!)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .map { (buffer: Data) -> JadeResponse<K> in
                try CodableCBORDecoder().decode(JadeResponse<K>.self, from: buffer)
            }.flatMap { res in
                return Observable<JadeResponse<K>>.create { observer in
                    if let error = res.error {
                        observer.onError(JadeError.from(code: error.code, message: error.message))
                    } else {
                        observer.onNext(res)
                        observer.onCompleted()
                    }
                    return Disposables.create { }
                }
            }
    }

    func exchange(_ data: Data) -> Observable<Data> {
        #if DEBUG
        print("=> " + data.map { String(format: "%02hhx", $0) }.joined())
        #endif
        return write(data)
            .flatMap { _ in return self.read() }
            .map { buffer -> Data in
                #if DEBUG
                print("<= " + buffer.map { String(format: "%02hhx", $0) }.joined())
                #endif
                return buffer
            }
    }

    func write(_ data: Data) -> Observable<Characteristic> {
        guard let characteristic = characteristicWrite else {
            return Observable.error(GaError.GenericError)
        }
        if data.count <= 128 {
            return characteristic.writeValue(data, type: .withResponse).asObservable()
        }
        return characteristic.writeValue(Data(data[0...127]), type: .withResponse).asObservable().flatMap {_ in
            return self.write(Data(data[128...data.count-1]))
        }
    }

    func read(_ prefix: Data? = nil) -> Observable<Data> {
        return self.characteristicNotify!.observeValueUpdate().take(1)
        .flatMap { characteristic -> Observable<Data> in
            guard let buffer = characteristic.value else {
                return Observable.error(GaError.GenericError)
            }
            let payload = (prefix ?? Data()) + buffer
            let decode = try? CBOR.decode([UInt8](payload))
            if decode != nil {
                return Observable.just(payload)
            }

            return self.read(payload).flatMap { data -> Observable<Data> in
                return Observable.just(data)
            }
        }
    }

    private func setupWrite() -> Observable<Characteristic> {
        return Observable.from(optional: peripheral)
            .flatMap { $0.discoverServices([JadeChannel.SERVICE_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { $0.discoverCharacteristics([JadeChannel.WRITE_CHARACTERISTIC_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { characteristic -> Observable<Characteristic> in
                self.characteristicWrite = characteristic
                return Observable.just(characteristic)
            }
    }

    private func setupNotify() -> Observable<Characteristic> {
        return Observable.from(optional: peripheral)
            .flatMap { $0.discoverServices([JadeChannel.SERVICE_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { $0.discoverCharacteristics([JadeChannel.CLIENT_CHARACTERISTIC_CONFIG]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { self.peripheral.discoverDescriptors(for: $0) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { descriptor -> Observable<Characteristic> in
                // Descriptor
                if descriptor.uuid == JadeChannel.CLIENT_CHARACTERISTIC_CONFIG {
                    print("descriptor CLIENT_CHARACTERISTIC_CONFIG")
                }
                self.characteristicNotify = descriptor.characteristic
                self.peripheral.peripheral.setNotifyValue(true, for: self.characteristicNotify!.characteristic)
                // Indicate
                return Observable.just(descriptor.characteristic)
        }
    }
}
