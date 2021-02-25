import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import SwiftCBOR

class JadeDevice: HWDeviceProtocol {

    var peripheral: Peripheral!
    var characteristicWrite: Characteristic?
    var characteristicNotify: Characteristic?

    let SERVICE_UUID = CBUUID(string: "6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    let WRITE_CHARACTERISTIC_UUID = CBUUID(string: "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    let CLIENT_CHARACTERISTIC_CONFIG = CBUUID(string: "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
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

    func build(method: String, params: Any? = nil) -> Data {
        let newid = 100000 + Int.random(in: 0 ..< 899999)
        var packet: [String: Any] = [
            "method": method,
            "id": "\(newid)"
        ]
        if let p = params {
            packet["params"] = p
        }
        let encoded: [UInt8] = try! CBOR.encodeAny(packet)

        // manually fix for path array https://github.com/myfreeweb/SwiftCBOR/issues/58
        /*if let dict = params as? [String: Any], let path = dict["path"] as? [UInt8] {
            let encodedPath = CBOR.encodeArray(path)
            let wrongPath = CBOR.encodeByteString(path)
            let pathName: [UInt8] = [0x70, 0x61, 0x74, 0x68] // path is always an array
            if let pos = (0..<encoded.count - pathName.count).firstIndex(where: { ind in
                return [UInt8](encoded[ind..<ind + pathName.count]) == pathName
            }) {
                // replace wrong path field with encodedPath
                encoded = encoded[0..<pos+4] + encodedPath + encoded[pos+4+wrongPath.count..<encoded.count]
            }
        }*/
        return Data(encoded)
    }

    func exchange(method: String, params: Any? = nil) -> Observable<[String: Any]> {
        let package = build(method: method, params: params)
        return exchange(package)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .map { buffer -> [String: Any] in
                let decoded = try? CBOR.decode([UInt8](buffer))
                return CBOR.parser(decoded ?? CBOR("")) as? [String: Any] ?? [:]
            }.flatMap { res in
                return Observable<[String: Any]>.create { observer in
                    if let error = res["error"] as? [String: Any],
                       let message = error["message"] as? String {
                        observer.onError(JadeError.Abort(message))
                    } else {
                        observer.onNext(res)
                        observer.onCompleted()
                    }
                    return Disposables.create { }
                }
            }
    }

    func exchange(_ request: String) -> Observable<String> {
        return Jade.shared.exchange(request.data(using: .ascii)!)
            .flatMap { res -> Observable<String> in
                return Observable.just(String(bytes: res, encoding: .ascii)!)
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
            .flatMap { $0.discoverServices([self.SERVICE_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { $0.discoverCharacteristics([self.WRITE_CHARACTERISTIC_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { characteristic -> Observable<Characteristic> in
                self.characteristicWrite = characteristic
                return Observable.just(characteristic)
            }
    }

    private func setupNotify() -> Observable<Characteristic> {
        return Observable.from(optional: peripheral)
            .flatMap { $0.discoverServices([self.SERVICE_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { $0.discoverCharacteristics([self.CLIENT_CHARACTERISTIC_CONFIG]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { self.peripheral.discoverDescriptors(for: $0) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { descriptor -> Observable<Characteristic> in
                // Descriptor
                if descriptor.uuid == self.CLIENT_CHARACTERISTIC_CONFIG {
                    print("descriptor CLIENT_CHARACTERISTIC_CONFIG")
                }
                self.characteristicNotify = descriptor.characteristic
                self.peripheral.peripheral.setNotifyValue(true, for: self.characteristicNotify!.characteristic)
                // Indicate
                return Observable.just(descriptor.characteristic)
        }
    }
}
