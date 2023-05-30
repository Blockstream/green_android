import Foundation

extension String {

    public func hexToData() -> Data {
        precondition(self.count%2 == 0)
        var data = Data(capacity: self.count/2)
        var indexIsEven = true
        for i in self.indices {
            if indexIsEven {
                let byteRange = i...self.index(after: i)
                let byte = UInt8(self[byteRange], radix: 16)
                data.append(byte!)
            }
            indexIsEven.toggle()
        }
        return data
    }
    public func hexToBytes() -> [UInt8] {
        [UInt8](hexToData())
    }
}

extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        if let strongSelf = self, !strongSelf.isEmpty {
            return false
        }
        return true
    }
    var isNotEmpty: Bool {
        return !isNilOrEmpty
    }
}
