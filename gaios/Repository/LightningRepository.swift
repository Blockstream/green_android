import Foundation
import lightning

class LightningRepository {

    static let shared = LightningRepository()
    
    func get(for id: String) -> AppGreenlightCredentials? {
        return try? AuthenticationTypeHandler.getAuthLightning(forNetwork: id)
    }

    func add(for id: String, credentials: AppGreenlightCredentials) {
        try? AuthenticationTypeHandler.addAuthLightning(forNetwork: id, credentials: credentials)
    }

    func upsert(for id: String, credentials: AppGreenlightCredentials) {
        if get(for: id) != nil {
            remove(for: id)
        }
        add(for: id, credentials: credentials)
    }

    func remove(for id: String) {
        _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyLightning, forNetwork: id)
    }
}
