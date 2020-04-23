import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth

// Communication class with a Ledger device connected using Bluetooth Low Energy (Nano X)

class LedgerDeviceBLE: HWDeviceProtocol {

    var peripheral: Peripheral!
    var characteristicWrite: Characteristic?
    var characteristicNotify: Characteristic?

    let SERVICE_UUID = CBUUID(string: "13D63400-2C97-0004-0000-4C6564676572")
    let WRITE_CHARACTERISTIC_UUID = CBUUID(string: "13D63400-2C97-0004-0002-4C6564676572")
    let NOTIFY_CHARACTERISTIC_UUID = CBUUID(string: "13D63400-2C97-0004-0001-4C6564676572")
    let CLIENT_CHARACTERISTIC_CONFIG = CBUUID(string: "00002902-0000-1000-8000-00805f9b34fb")
    var MTU: UInt8 = 128
    var TIMEOUT: Int = 30

    // Status codes
    enum SWCode: UInt16 {
        case SW_OK = 0x9000
        case SW_INS_NOT_SUPPORTED = 0x6D00
        case SW_WRONG_P1_P2 = 0x6B00
        case SW_INCORRECT_P1_P2 = 0x6A86
        case SW_RECONNECT = 0x6FAA
        case SW_INVALID_STATUS = 0x6700
        case SW_REJECTED = 0x6985
        case SW_INVALID_PKG = 0x6982
        case SW_ABORT = 0x0000
    }

    // Status error
    class SWError: Error {
        let code: SWCode
        init(_ code: SWCode) {
            self.code = code
        }
    }

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

    /**
     * Prepare an APDU receiving data, exchange it with a device, return the response data and Status Word
     * @param device device to exchange the APDU with
     * @param cla APDU CLA
     * @param ins APDU INS
     * @param p1 APDU P1
     * @param p2 APDU P2
     * @param length length of the data to receive
     * @returns APDU data
     */
    func exchangeAdpu(cla: UInt8, ins: UInt8, p1: UInt8, p2: UInt8, length: UInt8 = 0) -> Observable<Data> {
        var buffer = Data([cla, ins, p1, p2])
        if length != 0 {
            buffer.append(contentsOf: [length])
        }
        return exchange(buffer)
    }

    /**
     * Prepare an APDU sending data, exchange it with a device, return the response data and Status Word
     * @param device device to exchange the APDU with
     * @param cla APDU CLA
     * @param ins APDU INS
     * @param p1 APDU P1
     * @param p2 APDU P2
     * @param data data to exchange
     * @returns APDU data
     */
    func exchangeAdpu(cla: UInt8, ins: UInt8, p1: UInt8, p2: UInt8, data: Data) -> Observable<Data> {
        var buffer = Data([cla, ins, p1, p2, UInt8(data.count)])
        buffer.append(contentsOf: data)
        return exchange(buffer)
    }

    func exchange(_ data: Data) -> Observable<Data> {
        #if DEBUG
        print("=> " + data.map { String(format: "%02hhx", $0) }.joined())
        #endif
        let buf = try? LedgerWrapper.wrapCommandAPDUInternal(channel: 0, command: data, packetSize: MTU, hasChannel: false)
        guard characteristicWrite != nil, let buffer = buf else {
            return Observable.error(SWError(SWCode.SW_ABORT))
        }
        return self.characteristicWrite!.writeValue(buffer, type: .withResponse).asObservable()
            .subscribeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in return self.characteristicNotify!.observeValueUpdate() }
            .timeoutIfNoEvent(RxTimeInterval.seconds(TIMEOUT))
            .take(1)
            .flatMap { characteristic -> Observable<Data> in
                guard let buffer = characteristic.value else {
                    return Observable.error(SWError(SWCode.SW_ABORT))
                }
                return self.response(buffer)
        }
    }

    private func response(_ buffer: Data) -> Observable<Data> {
        do {
            let res = try LedgerWrapper.unwrapResponseAPDUInternal(channel: 0, data: buffer, packetSize: UInt8(buffer.count), hasChannel: false)
            #if DEBUG
            print("<= " + res.map { String(format: "%02hhx", $0) }.joined())
            #endif
            let command = res[0..<res.count-2]
            let lastSW = (UInt16(res[res.count - 2]) << 8) + UInt16(res[res.count - 1])
            if lastSW != SWCode.SW_OK.rawValue {
                return Observable.error(SWError(SWCode.init(rawValue: lastSW) ?? SWCode.SW_ABORT))
            }
            return Observable.just(command)
        } catch {
            return Observable.error(error)
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
            .flatMap { $0.discoverCharacteristics([self.NOTIFY_CHARACTERISTIC_UUID]) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { self.peripheral.discoverDescriptors(for: $0) }.asObservable()
            .flatMap { Observable.from($0) }
            .flatMap { descriptor -> Observable<Characteristic> in
                // Descriptor
                print(descriptor.uuid)
                if descriptor.uuid == self.CLIENT_CHARACTERISTIC_CONFIG {
                    print("descriptor CLIENT_CHARACTERISTIC_CONFIG")
                }
                self.characteristicNotify = descriptor.characteristic
                // Notification
                self.peripheral.peripheral.setNotifyValue(true, for: self.characteristicNotify!.characteristic)
                return Observable.just(descriptor.characteristic)
        }
    }
}
