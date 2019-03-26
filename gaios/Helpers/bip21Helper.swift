import Foundation

class Bip21Helper {

    static func btcURIforAmount(address: String, amount: Double) -> String {
        let result = String(format: "bitcoin:%@?amount=%.8f", address, amount)
        return result
    }

    static func btcURIforAddress(address: String) -> String {
        let result = String(format: "bitcoin:%@", address)
        return result
    }
}
