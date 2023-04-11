import Foundation

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
