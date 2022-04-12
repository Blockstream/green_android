import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth

/**
 * Package commands and responses to be sent over the chosen bearer
 */
final class LedgerWrapper {

    private static let TAG_APDU: UInt8 = 0x05

    enum LedgerError: Error {
        case InvalidParameter
        case IOError
    }

    /**
     * Prepare an APDU to be sent over the chosen bearer
     * @param channel dummy channel to use
     * @param command APDU to send
     * @param packetSize maximum size of a packet for this bearer
     * @param hasChannel set to true if this bearer includes channel information
     * @return list of packets to be sent over the chosen bearer
     */
    static func wrapCommandAPDUInternal(channel: UInt8, command: Data, packetSize: UInt8, hasChannel: Bool) throws -> Data? {
        if packetSize < 3 { throw LedgerError.InvalidParameter }
        var buffer = [UInt8]()

        var sequenceIdx: UInt8 = 0
        var offset: UInt8 = 0
        let headerSize: UInt8 = hasChannel ? 7 : 5
        let size = UInt8(command.count)
        buffer += hasChannel ? [channel >> 8, channel] : []
        buffer += [TAG_APDU, sequenceIdx >> 8, sequenceIdx]
        sequenceIdx += 1
        buffer += [size >> 8, size]
        var blockSize: UInt8 = command.count > packetSize - headerSize ? packetSize - headerSize : size
        buffer += command[Int(offset)..<Int(offset + blockSize)]
        offset += blockSize
        while offset != size {
            buffer += hasChannel ? [channel >> 8, channel] : []
            buffer += [TAG_APDU, sequenceIdx >> 8, sequenceIdx]
            sequenceIdx += 1
            blockSize = (size - offset > packetSize - headerSize + 2 ? packetSize - headerSize + 2 : size - offset)
            buffer += command[Int(offset)..<Int(offset + blockSize)]
            offset += blockSize
        }
        if buffer.count % Int(packetSize) != 0 {
            let len = Int(packetSize) - (buffer.count % Int(packetSize))
            let pad = [UInt8](repeating: 0, count: len)
            buffer += pad
        }
        return Data(buffer)
    }

    /**
     * Reassemble packets received over the chosen bearer
     * @param channel dummy channel to use
     * @param data binary data received so far
     * @param packetSize maximum size of a packet for this bearer
     * @param hasChannel set to true if this bearer includes channel information
     * @param reassembled response or null if not enough data is available
     */
    // swiftlint:disable cyclomatic_complexity
    static func unwrapResponseAPDUInternal(channel: UInt8, data: Data, packetSize: UInt8, hasChannel: Bool) throws -> Data {
        var buffer = [UInt8]()
        var offset = 0
        var sequenceIdx: UInt8 = 0
        let headerSize: UInt8 = hasChannel ? 7 : 5
        if data.count < headerSize { throw LedgerError.IOError }

        if hasChannel {
            if data[0] != channel >> 8 || data[1] != channel & 0xff {
                throw LedgerError.IOError
            }
            offset += 2
        }
        if data[offset] != TAG_APDU { throw LedgerError.IOError }
        if data[offset + 1] != 0x00 { throw LedgerError.IOError }
        if data[offset + 2] != 0x00 { throw LedgerError.IOError }
        var responseLength = (data[offset + 3] & 0xff) << 8
        responseLength |= data[offset + 4] & 0xff
        offset += 5
        if data.count < headerSize + responseLength { throw LedgerError.IOError }
        var blockSize: UInt8 = responseLength > packetSize - headerSize ? packetSize - headerSize : responseLength
        buffer += data[offset..<Int(UInt8(offset) + blockSize)]
        offset += Int(blockSize)

        while buffer.count != responseLength {
            sequenceIdx += 1
            if offset == data.count { throw LedgerError.IOError }
            if hasChannel {
                if data[offset] != channel >> 8 || data[offset + 1] != channel & 0xff {
                    throw LedgerError.IOError
                }
                offset += 2
            }
            if data[offset] != TAG_APDU { throw LedgerError.IOError }
            if data[offset + 1] != sequenceIdx >> 8 { throw LedgerError.IOError }
            if data[offset + 2] != sequenceIdx & 0xff { throw LedgerError.IOError }
            offset += 3
            blockSize = (responseLength - UInt8(buffer.count) > packetSize - headerSize + 2 ? packetSize - headerSize + 2 : responseLength - UInt8(buffer.count))
            if blockSize > data.count - offset { throw LedgerError.IOError }
            buffer += data[offset..<Int(UInt8(offset) + blockSize)]
            offset += Int(blockSize)
        }
        return Data(buffer)
    }

}
