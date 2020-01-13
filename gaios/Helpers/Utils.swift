import Foundation

extension String {
    func hexadecimal() -> Data? {
        var data = Data(capacity: self.count / 2)
        let regex = try! NSRegularExpression(pattern: "[0-9a-f]{1,2}", options: .caseInsensitive)
        regex.enumerateMatches(in: self, options: .withoutAnchoringBounds, range: NSRange(location: 0, length: utf16.count), using: ({ match, _, _ in
            let byteString = (self as NSString).substring(with: match!.range)
            var num = UInt8(byteString, radix: 16)!
            data.append(&num, count: 1)
        }))
        return data.count > 0 ? data : nil
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
