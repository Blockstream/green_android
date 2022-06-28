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
     * @param command APDU to send
     * @param packetSize maximum size of a packet for this bearer
     * @return list of packets to be sent over the chosen bearer
     */
    static func wrapCommandAPDU(command: Data, packetSize: Int) throws -> Data? {
        if packetSize < 3 { throw LedgerError.InvalidParameter }
        var buffer = [UInt8]()

        var sequenceIdx: UInt16 = 0
        var offset = 0
        //buffer += [channel >> 8, channel]
        buffer += [TAG_APDU,
                   UInt8(sequenceIdx >> 8),
                   UInt8(sequenceIdx),
                   UInt8(command.count >> 8),
                   UInt8(command.count)]
        sequenceIdx += 1
        var blockSize = command.count > packetSize - 5 ? packetSize - 5 : command.count
        buffer += command[offset..<offset + blockSize]
        offset += blockSize
        while offset != command.count {
            //buffer += [channel >> 8, channel]
            buffer += [TAG_APDU,
                       UInt8(sequenceIdx >> 8),
                       UInt8(sequenceIdx)]
            sequenceIdx += 1
            blockSize = (command.count - offset > packetSize - 5 - 2 ? packetSize - 5 - 2: command.count - offset)
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
     * @param data binary data received so far
     * @param packetSize maximum size of a packet for this bearer
     * @param reassembled response or null if not enough data is available
     */
    // swiftlint:disable cyclomatic_complexity
    static func unwrapResponseAPDU(data: Data, packetSize: Int) throws -> Data {
        var buffer = [UInt8]()
        var offset = 0
        var sequenceIdx: UInt8 = 0
        if data.count < 5 { throw LedgerError.IOError }
        if data[offset] != TAG_APDU { throw LedgerError.IOError }
        if data[offset + 1] != 0x00 { throw LedgerError.IOError }
        if data[offset + 2] != 0x00 { throw LedgerError.IOError }
        var responseLength = (data[offset + 3] & 0xff) << 8
        responseLength |= data[offset + 4] & 0xff
        offset += 5
        if data.count < 5 + responseLength { throw LedgerError.IOError }
        var blockSize = responseLength > packetSize - 5 ? packetSize - 5 : Int(responseLength)
        buffer += data[offset..<Int(offset + blockSize)]
        offset += blockSize

        while buffer.count != responseLength {
            sequenceIdx += 1
            if offset == data.count { throw LedgerError.IOError }
            if data[offset] != TAG_APDU { throw LedgerError.IOError }
            if data[offset + 1] != sequenceIdx >> 8 { throw LedgerError.IOError }
            if data[offset + 2] != sequenceIdx & 0xff { throw LedgerError.IOError }
            offset += 3
            blockSize = Int(responseLength) - buffer.count > packetSize - 5 + 2 ? packetSize - 5 + 2 : Int(responseLength) - buffer.count
            if blockSize > data.count - offset { throw LedgerError.IOError }
            buffer += data[offset..<Int(offset + blockSize)]
            offset += Int(blockSize)
        }
        return Data(buffer)
    }

}
