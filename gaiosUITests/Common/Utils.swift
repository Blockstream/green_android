import Foundation

class Utils {
    
    static func sanitize(_ word: String) -> String {
        let comp: [String] = word.components(separatedBy: " ")
        return comp.last!
    }
}
