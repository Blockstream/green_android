import Foundation

public struct VarintUtils {
    public static func read(_ data: Data) -> (Int, Int) {
        let data = Data(data.bytes)
        var result  = 0
        var len = 0
        let val1 = data[0] & 0xff
        if val1 < 0xfd {
            result = Int(val1)
            len = 1
        } else if val1 == 0xfd {
            result |= Int(data[1] & 0xff)
            result |= Int(data[2] & 0xff) << 8
            len = 2
        } else if val1 == 0xfe {
            result |= Int(data[1] & 0xff)
            result |= Int(data[2] & 0xff) << 8
            result |= Int(data[3] & 0xff) << 16
            result |= Int(data[4] & 0xff) << 24
            len = 4
        }
        return (result, len)
    }
    public static func write(_ value: Int) -> Data {
        var result = [UInt8]()
        if value < 0xfd {
            result = [UInt8(value)]
        } else if value <= 0xfffff {
            result += [0xfd]
            result += [UInt8(value & 0xff)]
            result += [UInt8((value >> 8) & 0xff)]
        } else {
            result += [0xfe]
            result += [UInt8(value & 0xff)]
            result += [UInt8((value >> 8) & 0xff)]
            result += [UInt8((value >> 16) & 0xff)]
            result += [UInt8((value >> 24) & 0xff)]
        }
        return Data(result)
    }
}
extension Int {
    func varInt() -> [UInt8] {
        switch varIntSize() {
        case 1:
            return [UInt8(self)]
        case 3:
            return [253, UInt8(self & 0xff), UInt8((self >> 8) & 0xff)]
        case 5:
            return [254] + UInt(self).uint32LE()
        default:
            return [255] + UInt64(self).uint64LE()
        }
    }

    func uint32LE() -> [UInt8] {
        return [UInt8(self & 0xff), UInt8((self >> 8) & 0xff),
                UInt8((self >> 16) & 0xff), UInt8((self >> 24) & 0xff)]
    }

    func uint32BE() -> [UInt8] {
        return [UInt8((self >> 24) & 0xff), UInt8((self >> 16) & 0xff),
                UInt8((self >> 8) & 0xff), UInt8(self & 0xff)]
    }

    func varIntSize() -> UInt8 {
        // if negative, it's actually a very large unsigned long value
        if self < 0 { return 9 } // 1 marker + 8 data bytes
        if self < 253 { return 1 } // 1 data byte
        if self <= 0xFFFF { return 3 } // 1 marker + 2 data bytes
        if self <= 0xFFFFFFFF { return 5 } // 1 marker + 4 data bytes
        return 9 // 1 marker + 8 data bytes
    }
}

extension UInt64 {
    func uint64LE() -> [UInt8] {
        return [UInt8(self & 0xff), UInt8((self >> 8) & 0xff),
                UInt8((self >> 16) & 0xff), UInt8((self >> 24) & 0xff),
                UInt8((self >> 32) & 0xff), UInt8((self >> 40) & 0xff),
                UInt8((self >> 48) & 0xff), UInt8((self >> 56) & 0xff)]
    }
}

extension UInt {
    func uint32BE() -> [UInt8] {
        return [UInt8((self >> 24) & 0xff), UInt8((self >> 16) & 0xff),
                UInt8((self >> 8) & 0xff), UInt8(self & 0xff)]
    }

    func uint32LE() -> [UInt8] {
        return [UInt8(self & 0xff), UInt8((self >> 8) & 0xff),
                UInt8((self >> 16) & 0xff), UInt8((self >> 24) & 0xff)]
    }

    func varint() -> [UInt8] {
        if self < 0xfd {
            return [UInt8(self & 0xff)]
        } else if self <= 0xffff {
            return [UInt8(0xfd), UInt8(self & 0xff), UInt8((self >> 8) & 0xff)]
        } else {
            return [UInt8(0xfe)] + self.uint32LE()
        }
    }
}

extension UInt32 {
    func uint32LE() -> [UInt8] {
        return [UInt8(self & 0xff), UInt8((self >> 8) & 0xff),
                UInt8((self >> 16) & 0xff), UInt8((self >> 24) & 0xff)]
    }

    func uint32BE() -> [UInt8] {
        return [UInt8((self >> 24) & 0xff), UInt8((self >> 16) & 0xff),
                UInt8((self >> 8) & 0xff), UInt8(self & 0xff)]
    }
}

extension Array where Element == UInt8 {
    var data: Data? { Data(self) }
    var hex: String? { self.data?.hex }
}

extension Data {
    var bytes: [UInt8] { [UInt8](self) }
    var hex: String { self.map { String(format: "%02hhx", $0) }.joined() }
    func chunked(into size: Int) -> [Data] {
        bytes.chunked(into: size).map { Data($0) }
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0 ..< Swift.min($0 + size, count)])
        }
    }
}
