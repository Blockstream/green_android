import Foundation
import AsyncBluetooth
import Combine
import SwiftCBOR

public class BleLedgerCommands: BleLedgerConnection {
    
    let CLA_BOLOS: UInt8 = 0xE0
    let INS_GET_VERSION: UInt8 = 0x01
    let INS_RUN_APP: UInt8 = 0xD8
    let INS_QUIT_APP: UInt8 = 0xA7
    let CLA_COMMON_SDK: UInt8 = 0xB0

    let INS_GET_FIRMWARE_VERSION: UInt8 = 0xc4
    let INS_GET_APP_NAME_AND_VERSION: UInt8 = 0x01
    let INS_GET_WALLET_PUBLIC_KEY: UInt8 = 0x40
    let INS_SIGN_MESSAGE: UInt8 = 0x4e
    let INS_GET_TRUSTED_INPUT: UInt8 = 0x42
    let INS_HASH_INPUT_START: UInt8 = 0x44
    let INS_HASH_SIGN: UInt8 = 0x48
    let INS_HASH_INPUT_FINALIZE_FULL: UInt8 = 0x4a
    let INS_EXIT: UInt8 = 0xA7
    

    func finalizeInputFull(data: Data) async throws -> [String: Any] {
        let res = try await exchangeAdpu(cla: self.CLA_BOLOS, ins: self.INS_HASH_INPUT_FINALIZE_FULL, p1: 0xFF, p2: 0x00, data: [0x00].data!)
        
        var result = Data()
        var offset = 0
        while offset < data.count {
            let blockLength = (data.count - offset) > 255 ? 255 : data.count - offset
            let p1: UInt8 = offset + blockLength == data.count ? 0x80 : 0x00
            let buffer = data[offset..<offset+blockLength]
            result = try await exchangeAdpu(cla: self.CLA_BOLOS, ins: self.INS_HASH_INPUT_FINALIZE_FULL, p1: p1, p2: 0x00, data: buffer)
            offset += blockLength
        }
        return convertResponseToOutput(result)
    }

    func convertResponseToOutput(_ buffer: Data) -> [String: Any] {
        let len = buffer[0] & 0xff
        let value = buffer[1..<len+1]
        let userConfirmationValue = buffer[1 + value.count]
        switch userConfirmationValue {
        case 0x00, 0x01: // NONE, KEYBOARD
            return ["value": value, "user_confirmation": userConfirmationValue]
        case 0x03: // KEYCARD_SCREEN
            let keycardIndexesLength = Int(buffer[2 + value.count])
            let keycardIndexes = buffer[(3 + value.count)..<(3 + value.count + keycardIndexesLength)]
            let screenInfoLength = buffer.count - 3 - value.count - keycardIndexes.count
            let screenInfo = buffer[(3 + value.count + keycardIndexes.count)..<(3 + value.count + keycardIndexesLength + screenInfoLength)]
            return ["value": value, "user_confirmation": userConfirmationValue, "keycard_indexes": keycardIndexes, "screen_info": screenInfo]
        case 0x04, 0x05: // KEYCARD, KEYCARD_NFC
            let keycardIndexesLength = Int(buffer[2 + value.count])
            let keycardIndexes = buffer[(3 + value.count)..<(3 + value.count + keycardIndexesLength)]
            return ["value": value, "user_confirmation": userConfirmationValue, "keycard_indexes": keycardIndexes]
        default:
            return [:]
        }
    }

    func inputBytes(_ input: InputOutput, isSegwit: Bool) -> Data? {
        let satoshi = isSegwit ? input.satoshi?.uint64LE() : nil
        return Data(input.getTxid ?? [] + input.ptIdx.uint32LE() + (satoshi ?? []))
    }

    func outputBytes(_ outputs: [InputOutput]) -> Data? {
        var buffer = VarintUtils.write(outputs.count)
        for out in outputs {
            let satoshi = (out.satoshi ?? 0).uint64LE()
            let script = out.scriptpubkey!.hexToData()
            buffer += satoshi + VarintUtils.write(script.count) + script
        }
        return Data(buffer)
    }

    func untrustedHashSign(privateKeyPath: [Int], pin: String, lockTime: UInt32, sigHashType: UInt8) async throws -> Data {
        let path: [UInt8] = try! pathToData(privateKeyPath)
        let data = Array(path) + [UInt8(pin.count)] + Array(pin.utf8) + lockTime.uint32BE() + [sigHashType]
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_SIGN, p1: 0, p2: 0, data: Data(data))
        let res = [0x30] + buffer[1..<buffer.count]
        return Data(res)
    }

    // swiftlint:disable function_parameter_count
    func startUntrustedTransaction(txVersion: UInt32, newTransaction: Bool, inputIndex: Int64, usedInputList: [[String: Any]], redeemScript: Data, segwit: Bool) async throws {
        // Start building a fake transaction with the passed inputs
        let buffer = txVersion.uint32LE() + UInt(usedInputList.count).varint()
        let p2: UInt8 = newTransaction ? (segwit ? 0x02 : 0x00) : 0x80
        _ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_INPUT_START, p1: 0x00, p2: p2, data: Data(buffer))
        
        for input in usedInputList.enumerated() {
            let script = input.offset == inputIndex ? redeemScript : Data()
            try await hashInput(input.element, script: script)
        }
    }

    func hashInput(_ input: [String: Any], script: Data) async throws {
        let isTrusted = input["trusted"] as? Bool ?? false
        let isSegwit = input["segwit"] as? Bool ?? true
        let first: UInt8 = isTrusted ? 0x01 : isSegwit ? 0x02 : 0x00
        let value: [UInt8] = Array((input["value"] as? Data)!)
        let len: [UInt8] = UInt(script.count).varint()
        let data: [UInt8] = [first] + (isTrusted ? [UInt8(value.count)] : []) + value + len
        _ = try await  exchangeAdpu(cla: CLA_BOLOS, ins: INS_HASH_INPUT_START, p1: 0x80, p2: 0x00, data: Data(data))
        let buffer: [UInt8] = script.bytes + (input["sequence"] as? [UInt8] ?? [])
        _ = try await exchangeApduSplit(cla: CLA_BOLOS, ins: INS_HASH_INPUT_START, p1: 0x80, p2: 0x00, data: Data(buffer))
    }
    
    func getTrustedInput(transaction: HWTransactionLedger, index: Int, sequence: Int, segwit: Bool) async throws -> [String: Any] {
        var buffer = Data()
        // Header
        buffer += index.uint32BE()
        buffer += transaction.version
        buffer += VarintUtils.write(transaction.inputs.count)
        _ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x00, p2: 0x00, data: buffer)
        // Each input
        for input in transaction.inputs {
            buffer = input.prevOut
            buffer += VarintUtils.write(input.script.count)
            _ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: buffer)
            //_ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: buffer + input.sequence)
            _ = try await exchangeApduSplit2(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data1: input.script, data2: input.sequence)
        }
        //  Number of outputs
        buffer = VarintUtils.write(transaction.outputs.count)
        _ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: buffer)
        // Each output
        for output in transaction.outputs {
            buffer = output.amount
            buffer += VarintUtils.write(output.script.count)
            _ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: buffer)
            //_ = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: buffer)
            _ = try await exchangeApduSplit(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: output.script)
        }
        // Locktime
        let res = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_TRUSTED_INPUT, p1: 0x80, p2: 0x00, data: transaction.locktime)
        let sequence = sequence.uint32LE()
        return ["value": res, "sequence": sequence, "trusted": true, "segwit": segwit]
    }

    func signMessagePrepare(path: [Int], message: Data) async throws -> Bool {
        let pathData = try! pathToData(path)
        let data = pathData + [UInt8(0)] + [UInt8(message.count)] + Array(message)
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_SIGN_MESSAGE, p1: 0, p2: 1, data: Data(data))
        return buffer[0] == 1
    }

    func signMessageSign(pin: [UInt8]) async throws -> [String: Any] {
        let data = pin.isEmpty ? [UInt8(0)] : [UInt8(pin.count)] + Array(pin)
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_SIGN_MESSAGE, p1: 0x80, p2: 1, data: Data(data))
        let yParity = buffer[0] & 0x0f
        let response = [UInt8(0x30)] + buffer[1..<buffer.count]
        return ["signature": response, "yParity": yParity]
    }

    public func pubKey(path: [Int]) async throws -> [String: Any] {
        let data = try! pathToData(path)
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_WALLET_PUBLIC_KEY, p1: UInt8(0), p2: UInt8(0), data: Data(data))
        var offset: Int = 0
        let publicKey = buffer[1..<Int(buffer[offset]&0xff)+1]
        offset += publicKey.count + 1
        let address = buffer[offset+1..<offset+Int(buffer[offset]&0xff)+1]
        offset += address.count + 1
        let chainCode = buffer[offset..<offset+32]
        return ["publicKey": publicKey, "address": address, "chainCode": chainCode, "path": path]
    }

    public func pathToData(_ path: [Int]) throws -> [UInt8] {
        if path.count == 0 {
            return [0]
        } else if path.count > 10 {
            throw HWError.Abort("Path too long")
        }
        var buffer = [UInt8(path.count)]
        for p in path {
            buffer += UInt(p).uint32BE()
        }
        return buffer
    }

    public func version() async throws -> [String: Any] {
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_VERSION, p1: 0, p2: 0)
        let targetID = buffer[0..<4]
        let versionSize = Int(buffer[4])
        let version = String(bytes: buffer[5..<(5 + versionSize)], encoding: .utf8)
        let flagSize = Int(buffer[5 + versionSize])
        let osFlags = buffer[(6 + versionSize)..<(6 + versionSize + flagSize)]
        let mcuSize = Int(buffer[6 + versionSize + flagSize])
        let mcuVersion = String(bytes: buffer[(7 + versionSize + flagSize)..<(7 + versionSize + flagSize + mcuSize)], encoding: .utf8)
        return ["targetID": targetID, "version": version!,
                "osFlags": osFlags, "mcuVersion": mcuVersion!]
    }

    public func deviceInfo() async throws -> [String: Any] {
        let data = try await version()
        let isOSU = (data["version"] as? String)!.contains("-osu")
        let isBootloader = (data["targetId"] as? Data)![0] != 0x30
        let managerAllowed = (data["osFlags"] as? Data)![0] & 0x08
        let pinValidated = (data["osFlags"] as? Data)![0] & 0x80
        return ["isOSU": isOSU, "isBootloader": isBootloader, "managerAllowed": managerAllowed, "pinValidated": pinValidated]
    }

    public func firmware() async throws -> Bool {
        let buffer = try await exchangeAdpu(cla: CLA_BOLOS, ins: INS_GET_FIRMWARE_VERSION, p1: 0, p2: 0)
        let compressedKeys = buffer[0] == 0x01
        let major = Int(((buffer[1] & 0xff) << 8) | buffer[2] & 0xff)
        let minor = Int(buffer[3] & 0xff)
        let patch = Int(buffer[4] & 0xff)
        let isFirmwareOutdated = major < 0x3001 ||
        (major == 0x3001 && minor < 3) ||
        (major == 0x3001 && minor == 3 && patch < 7)
        print("compressedKeys \(compressedKeys) - major \(major) - minor \(minor) - patch \(patch)")
        return isFirmwareOutdated
    }

    public func application() async throws -> [String: Any] {
        let APP_DETAILS_FORMAT_VERSION = 1
        let buffer = try await exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_GET_APP_NAME_AND_VERSION, p1: 0, p2: 0)
        if buffer[0] != APP_DETAILS_FORMAT_VERSION {
            throw SWError(SWCode.SW_INVALID_STATUS)
        }
        let nameLength = buffer[1] & 0xff
        let name = String(bytes: buffer[2..<nameLength+2], encoding: .utf8)
        let offset = nameLength + 2
        let versionLength = buffer[Int(offset)] & 0xff
        let version = String(bytes: buffer[offset+1..<offset+versionLength+1], encoding: .utf8)
        return ["name": name!, "version": version!]
    }

    public func openApp(name: String) async throws {
        let buffer = Array(name.utf8)
        _ = try await exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_RUN_APP, p1: 0, p2: 0, data: Data(buffer))
    }

    public func quitApp() async throws {
        _ = try await exchangeAdpu(cla: CLA_COMMON_SDK, ins: INS_QUIT_APP, p1: 0, p2: 0)
    }
}
