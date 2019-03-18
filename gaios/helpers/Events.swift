import Foundation

struct TransactionEvent: Codable {
    enum CodingKeys: String, CodingKey {
        case txHash = "txhash"
        case type = "type"
        case subAccounts = "subaccounts"
        case satoshi = "satoshi"
    }
    let txHash: String
    let type: String
    let subAccounts: [Int]
    let satoshi: UInt64
}

struct SystemMessage: Codable {
    let text: String
}

protocol EventProtocol {
    func title() -> String
    func description(wallets: [WalletItem], twoFactorConfig: TwoFactorConfig?) -> String
}

struct Event: EventProtocol, Equatable {
    var value: [String: Any]

    static func == (lhs: Event, rhs: Event) -> Bool {
        return NSDictionary(dictionary: lhs.value).isEqual(to: rhs.value)
    }

    func decode<T>(_ type: T.Type) -> T? where T : Decodable {
        return try? JSONDecoder().decode(T.self, from: JSONSerialization.data(withJSONObject: value, options: []))
    }

    func kindOf<T>(_ type: T.Type) -> Bool where T : Decodable {
        return decode(T.self) != nil
    }

    func title() -> String {
        if kindOf(TransactionEvent.self) {
            return NSLocalizedString("id_new_transaction", comment: "")
        } else if kindOf(TwoFactorReset.self) {
            guard let twoFactorReset = getGAService().getTwoFactorReset() else { return "" }
            if !twoFactorReset.isResetActive { return "" }
            return NSLocalizedString("id_twofactor_reset_in_progress", comment: "")
        } else if kindOf(Settings.self) {
            return NSLocalizedString("id_set_up_twofactor_authentication", comment: "")
        } else if kindOf(SystemMessage.self) {
            return NSLocalizedString("id_system_message", comment: "")
        }
        return ""
    }

    func description(wallets: [WalletItem], twoFactorConfig: TwoFactorConfig?) -> String {
        if kindOf(TransactionEvent.self) {
            guard let txEvent = decode(TransactionEvent.self) else { return "" }
            let txType = txEvent.type == "incoming" ? NSLocalizedString("id_incoming", comment: "") : NSLocalizedString("id_outgoing", comment: "")
            let txAmount = String.toBtc(satoshi: txEvent.satoshi)
            let walletsList = wallets.filter { txEvent.subAccounts.contains(Int($0.pointer)) }
            let txWalletName = wallets.isEmpty ? "" : walletsList[0].localizedName()
            let description = String(format: NSLocalizedString("id_new_s_transaction_of_s_in", comment: ""), txType, txAmount, txWalletName)
            return description
        } else if kindOf(TwoFactorReset.self) {
            return ""
        } else if kindOf(Settings.self) {
            guard let _ = twoFactorConfig else { return "" }
            if !twoFactorConfig!.anyEnabled {
                return NSLocalizedString("id_your_wallet_is_not_yet_fully", comment: "")
            } else if twoFactorConfig!.enableMethods.count == 1 {
                return NSLocalizedString("id_you_only_have_one_twofactor", comment: "")
            }
        } else if kindOf(SystemMessage.self) {
            guard let event = decode(SystemMessage.self) else { return "" }
            return event.text
        }
        return ""
    }
}
