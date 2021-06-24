import Foundation

class Utils {
    
    static func sanitize(_ word: String) -> String {
        let comp: [String] = word.components(separatedBy: " ")
        return comp.last!
    }
    
    static func randomString(length: Int) -> String {
      let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
      return String((0..<length).map{ _ in letters.randomElement()! })
    }
}
