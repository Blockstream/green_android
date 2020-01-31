import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth

class LedgerCommands: LedgerDeviceBLE {

    let CLA_BOLOS: UInt8 = 0xE0
    let INS_GET_VERSION: UInt8 = 0x01
    let INS_RUN_APP: UInt8 = 0xD8
    let INS_QUIT_APP: UInt8 = 0xA7
    let CLA_COMMON_SDK: UInt8 = 0xB0

    let INS_GET_FIRMWARE_VERSION: UInt8 = 0xc4
    let INS_GET_APP_NAME_AND_VERSION: UInt8 = 0x01
    let INS_GET_WALLET_PUBLIC_KEY: UInt8 = 0x40
    let INS_SIGN_MESSAGE: UInt8 = 0x4e
    let INS_HASH_INPUT_START: UInt8 = 0x44
    let INS_HASH_SIGN: UInt8 = 0x48
    let INS_HASH_INPUT_FINALIZE_FULL: UInt8 = 0x4a
    let INS_EXIT: UInt8 = 0xA7

    func finalizeInputFull(data: Data) -> Observable<[String: Any]> {
        var datas = [Data]()
        datas.insert(Data([0]), at: 0)
        var offset = 0
        while offset < data.count {
            let blockLength = (data.count - offset) > 255 ? 255 : data.count - offset
            datas.append(data[offset..<offset+blockLength])
            offset += blockLength
        }
        let allObservables = datas
            .enumerated()
            .map { item -> Observable<Data> in
                return Observable.just(item)
                    .flatMap { item -> Observable<Data> in
                        let p1: UInt8 = item.offset == 0 ? 0xFF : item.offset == datas.count - 1 ? 0x80 : 0x00
                        return self.exchangeAdpu(cla: self.CLA_BOLOS, ins: self.INS_HASH_INPUT_FINALIZE_FULL, p1: p1, p2: 0x00, data: item.element)
                    }
                    .asObservable()
                    .timeoutIfNoEvent(RxTimeInterval.seconds(TIMEOUT))
                    .take(1)
            }
        return Observable<Data>.concat(allObservables).reduce(Data(), accumulator: { _, element in
            element
        }).flatMap { buffer -> Observable<[String: Any]> in
            return Observable.just(self.convertResponseToOutput(buffer))
        }
    }

    func convertResponseToOutput(_ buffer: Data) -> [String: Any] {
        let len = buffer[0] & 0xff
        let value = buffer[1..<len+1]
        let userConfirmationValue = buffer[1 + value.count]
        switch userConfirmationValue {
        case 0x00, 0x01: // NONE, KEYBOARD
            return ["value": value, "user_confirmation": userConfirmationValue]
        case 0x03: //KEYCARD_SCREEN
            let keycardIndexesLength = Int(buffer[2 + value.count])
            let keycardIndexes = buffer[(3 + value.count)..<(3 + value.count + keycardIndexesLength)]
            let screenInfoLength = buffer.count - 3 - value.count - keycardIndexes.count
            let screenInfo = buffer[(3 + value.count + keycardIndexes.count)..<(3 + value.count + keycardIndexesLength + screenInfoLength)]
            return ["value": value, "user_confirmation": userConfirmationValue, "keycard_indexes": keycardIndexes, "screen_info": screenInfo]
        case 0x04, 0x05: //KEYCARD, KEYCARD_NFC
            let keycardIndexesLength = Int(buffer[2 + value.count])
            let keycardIndexes = buffer[(3 + value.count)..<(3 + value.count + keycardIndexesLength)]
            return ["value": value, "user_confirmation": userConfirmationValue, "keycard_indexes": keycardIndexes]
        default:
            return [:]
        }
    }

    func inputBytes(_ input: [String: Any], isSegwit: Bool) -> Data? {
        let txHashHex = input["txhash"] as? String
        let ptIdx = input["pt_idx"] as? UInt
        let txId: [UInt8]? = hexToData(txHashHex!).reversed()
        return Data(txId! + ptIdx!.uint32LE() + (isSegwit ? (input["satoshi"] as? UInt64)!.uint64LE() : []))
    }

    func outputBytes(_ outputs: [[String: Any]]) -> Data? {
        var buffer = outputs.count.varInt()
        for out in outputs {
            let satoshi = out["satoshi"] as? UInt64
            let script = out["script"] as? String
            let hex = hexToData(script!)
            buffer += satoshi!.uint64LE() + hex.count.varInt() + hex
        }
        return Data(buffer)
    }

    func untrustedHashSign(privateKeyPath: [Int], pin: String, lockTime: UInt, sigHashType: UInt8) -> Observable<Data> {
        let path: [UInt8] = try! pathToData(privateKeyPath)
        let buffer = Array(path) + [UInt8(pin.count)] + Array(pin.utf8) + lockTime.uint32BE() + [sigHashType]
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_SIGN, p1: 0, p2: 0, data: Data(buffer))
        .flatMap { buffer -> Observable<Data> in
            let res = [0x30] + buffer[1..<buffer.count]
            return Observable.just(Data(res))
        }
    }

    //swiftlint:disable function_parameter_count
    func startUntrustedTransaction(txVersion: UInt, newTransaction: Bool, inputIndex: Int64, usedInputList: [[String: Any]], redeemScript: Data, segwit: Bool) -> Observable<Bool> {
        // Start building a fake transaction with the passed inputs
        let buffer = txVersion.uint32LE() + UInt(usedInputList.count).varint()
        let p2: UInt8 = newTransaction ? (segwit ? 0x02 : 0x00) : 0x80
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_INPUT_START, p1: 0x00, p2: p2, data: Data(buffer)).flatMap { _ -> Observable<Bool> in
            return self.hashInputs(usedInputList: usedInputList, inputIndex: inputIndex, redeemScript: redeemScript)
        }
    }

    func hashInputs(usedInputList: [[String: Any]], inputIndex: Int64, redeemScript: Data) -> Observable<Bool> {
        let allObservables = usedInputList
            .enumerated()
            .map { input -> Observable<Bool> in
                let script = input.offset == inputIndex ? redeemScript : Data()
                return Observable.just(input.element)
                    .flatMap { self.hashInput($0, script: script) }
                    .asObservable()
                    .take(1)
        }
        //swiftlint:disable reduce_boolean
        return Observable<Bool>.concat(allObservables).reduce(true, accumulator: { _, element in
            element
        })
    }

    func hashInput(_ input: [String: Any], script: Data) -> Observable<Bool> {
        let isTrusted = input["trusted"] as? Bool ?? false
        let isSegwit = input["segwit"] as? Bool ?? true
        let first: UInt8 = isSegwit ? 0x02 : isTrusted ? 0x01 : 0x00
        let value: [UInt8] = Array((input["value"] as? Data)!)
        let len: [UInt8] = UInt(script.count).varint()
        let buffer: [UInt8] = [first] + (isTrusted ? [UInt8(value.count)] : []) + value + len
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_INPUT_START, p1: 0x80, p2: 0x00, data: Data(buffer))
        .flatMap { _ -> Observable<Data> in
            let buffer = Array(script) + Array((input["sequence"] as? Data)!)
            return self.exchangeAdpu(cla: self.CLA_BOLOS, ins: self.INS_HASH_INPUT_START, p1: 0x80, p2: 0x00, data: Data(buffer))
        }.flatMap { _ -> Observable<Bool> in
            return Observable.just(true)
        }
    }

    func signMessagePrepare(path: [Int], message: Data) -> Observable<Bool> {
        let pathData = try! pathToData(path)
        let buffer = pathData + [UInt8(0)] + [UInt8(message.count)] + Array(message)
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_SIGN_MESSAGE, p1: 0, p2: 1, data: Data(buffer)).flatMap { buffer -> Observable<Bool> in
            return Observable.just(buffer[0] == 1)
        }
    }

    func signMessageSign(pin: [UInt8]) -> Observable<[String: Any]> {
        let buffer = pin.isEmpty ? [UInt8(0)] : [UInt8(pin.count)] + Array(pin)
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_SIGN_MESSAGE, p1: 0x80, p2: 1, data: Data(buffer)).flatMap { buffer -> Observable<[String: Any]> in
            let yParity = buffer[0] & 0x0f
            let response = [UInt8(0x30)] + buffer[1..<buffer.count]
            return Observable.just(["signature": response, "yParity": yParity])
        }
    }

    func pubKey(path: [Int]) -> Observable<[String: Any]> {
        let buffer = try! pathToData(path)
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_WALLET_PUBLIC_KEY, p1: UInt8(0), p2: UInt8(0), data: Data(buffer)).flatMap { buffer -> Observable<[String: Any]> in
            var offset: Int = 0
            let publicKey = buffer[1..<Int(buffer[offset]&0xff)+1]
            offset += publicKey.count + 1
            let address = buffer[offset+1..<offset+Int(buffer[offset]&0xff)+1]
            offset += address.count + 1
            let chainCode = buffer[offset..<offset+32]
            return Observable.just(["publicKey": publicKey, "address": address, "chainCode": chainCode, "path": path])
        }
    }

    func pathToData(_ path: [Int]) throws -> [UInt8] {
        if path.count == 0 {
            return [0]
        } else if path.count > 10 {
            throw GaError.GenericError
        }
        var buffer = [UInt8(path.count)]
        for p in path {
            buffer += UInt(p).uint32BE()
        }
        return buffer
    }

    func version() -> Observable<[String: Any]> {
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_VERSION, p1: 0, p2: 0)
            .flatMap { buffer -> Observable<[String: Any]> in
                let targetID = buffer[0..<4]
                let versionSize = Int(buffer[4])
                let version = String(bytes: buffer[5..<(5 + versionSize)], encoding: .utf8)
                let flagSize = Int(buffer[5 + versionSize])
                let osFlags = buffer[(6 + versionSize)..<(6 + versionSize + flagSize)]
                let mcuSize = Int(buffer[6 + versionSize + flagSize])
                let mcuVersion = String(bytes: buffer[(7 + versionSize + flagSize)..<(7 + versionSize + flagSize + mcuSize)], encoding: .utf8)
                return Observable.just(["targetID": targetID, "version": version!,
                        "osFlags": osFlags, "mcuVersion": mcuVersion!])
        }
    }

    func deviceInfo() -> Observable<[String: Any]> {
        return version().flatMap { data -> Observable<[String: Any]> in
            let isOSU = (data["version"] as? String)!.contains("-osu")
            let isBootloader = (data["targetId"] as? Data)![0] != 0x30
            let managerAllowed = (data["osFlags"] as? Data)![0] & 0x08
            let pinValidated = (data["osFlags"] as? Data)![0] & 0x80
            return Observable.just(["isOSU": isOSU, "isBootloader": isBootloader,
                    "managerAllowed": managerAllowed, "pinValidated": pinValidated])
        }
    }

    func firmware() -> Observable<Bool> {
        return exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_FIRMWARE_VERSION, p1: 0, p2: 0)
        .flatMap { buffer -> Observable<Bool> in
            let compressedKeys = buffer[0] == 0x01
            let major = Int(((buffer[1] & 0xff) << 8) | buffer[2] & 0xff)
            let minor = Int(buffer[3] & 0xff)
            let patch = Int(buffer[4] & 0xff)
            let isFirmwareOutdated = major < 0x3001 ||
                (major == 0x3001 && minor < 3) ||
                (major == 0x3001 && minor == 3 && patch < 7)
            print("compressedKeys \(compressedKeys) - major \(major) - minor \(minor) - patch \(patch)")
            return Observable.just(isFirmwareOutdated)
        }
    }

    func application() -> Observable<[String: Any]> {
        let APP_DETAILS_FORMAT_VERSION = 1
        return exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_GET_APP_NAME_AND_VERSION, p1: 0, p2: 0)
        .flatMap { buffer -> Observable<[String: Any]> in
            if buffer[0] != APP_DETAILS_FORMAT_VERSION {
                throw SWError(SWCode.SW_INVALID_STATUS)
            }
            let nameLength = buffer[1] & 0xff
            let name = String(bytes: buffer[2..<nameLength+2], encoding: .utf8)
            let offset = nameLength + 2
            let versionLength = buffer[Int(offset)] & 0xff
            let version = String(bytes: buffer[offset+1..<offset+versionLength+1], encoding: .utf8)
            return Observable.just(["name": name!, "version": version!])
        }
    }

    func openApp(name: String) -> Observable<Bool> {
        let buffer = Array(name.utf8)
        return exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_RUN_APP, p1: 0, p2: 0, data: Data(buffer))
            .flatMap { _ -> Observable<Bool> in
                return Observable.just(true)
            }
    }

    func quitApp() -> Observable<Bool> {
        return exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_QUIT_APP, p1: 0, p2: 0)
            .flatMap { _ -> Observable<Bool> in
                return Observable.just(true)
        }
    }
}
