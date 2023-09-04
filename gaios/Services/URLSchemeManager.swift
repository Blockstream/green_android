import Foundation
import gdk

class URLSchemeManager {
    
    static let shared = URLSchemeManager()
    var sendingAppID: String? = nil
    var url: URL? = nil
    var isValid: Bool { url?.scheme != nil }
    var bip21: String? {
        url?.description
        .replacingOccurrences(of: "bitcoin://", with: "bitcoin:")
        .replacingOccurrences(of: "liquidnetwork://", with: "liquidnetwork:")
        .replacingOccurrences(of: "lightning://", with: "lightning:")
    }    
}
