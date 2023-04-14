 import Foundation

public struct NetworkSettings: Codable {

    enum CodingKeys: String, CodingKey {
        case name
        case useTor = "use_tor"
        case proxy
        case userAgent = "user_agent"
        case spvEnabled = "spv_enabled"
        case electrumUrl = "electrum_url"
    }

    let name: String
    let useTor: Bool?
    let proxy: String?
    let userAgent: String?
    let spvEnabled: Bool?
    let electrumUrl: String?

    public init(name: String, useTor: Bool? = nil, proxy: String? = nil, userAgent: String? = nil, spvEnabled: Bool? = nil, electrumUrl: String? = nil) {
        self.name = name
        self.useTor = useTor
        self.proxy = proxy
        self.userAgent = userAgent
        self.spvEnabled = spvEnabled
        self.electrumUrl = electrumUrl
    }
    
}
