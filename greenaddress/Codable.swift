import Foundation

public extension Encodable {

    func encoded() throws -> Data {
        return try JSONEncoder().encode(self)
    }

    func toDict() -> [String: Any]? {
        let data = try? encoded()
        return try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
    }

    func stringify() -> String? {
        if let data = try? JSONSerialization.data(withJSONObject: self.toDict() ?? [:], options: .fragmentsAllowed) {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
}

public extension Decodable {

    static func from(_ dict: [String: Any]) -> Decodable? {
        let data = try? JSONSerialization.data(withJSONObject: dict, options: [])
        let json = try? JSONDecoder().decode(self, from: data ?? Data())
        return json
    }
}
