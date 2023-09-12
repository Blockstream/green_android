import Foundation
import BreezSDK
import gdk
import greenaddress

struct CreateTransaction {
    var addressee: Addressee?
    var feeRate: UInt64?
    var sendAll: Bool?
    var subaccount: WalletItem?
    var error: String?
    var privateKey: String?
    var previousTransaction: [String: Any]?
    var assetId: String? {
        get { addressee?.assetId }
        set { addressee?.assetId = newValue }
    }
    var address: String? {
        get { addressee?.address }
        set { if let value = newValue  { addressee?.address = value } }
    }
    var satoshi: Int64? {
        get { if let satoshi = addressee?.satoshi { return abs(satoshi) }; return nil }
        set { addressee?.satoshi = newValue }
    }
    var tx: Transaction {
        var tx = Transaction([:], subaccount: subaccount?.hashValue)
        if var addressee = addressee {
            if let satoshi = addressee.satoshi {
                addressee.satoshi = abs(satoshi)
            }
            if sendAll ?? false {
                addressee.isGreedy = sendAll
            }
            tx.addressees = [addressee]
        }
        if let privateKey = privateKey {
            tx.privateKey = privateKey
        }
        if let error = error {
            tx.error = error
        }
        if let previousTransaction = previousTransaction {
            tx.previousTransaction = previousTransaction
        }
        return tx
    }
}

enum ParserError: Error {
    case InvalidNetwork(_ localizedDescription: String? = nil)
    case InvalidTransaction(_ localizedDescription: String? = nil)
    case InvalidInput(_ localizedDescription: String? = nil)
}

class Parser {
    // inputs
    let input: String

    // internals
    var wm: WalletManager { WalletManager.current! }
    var bitcoinAccounts: [WalletItem] { wm.subaccounts.filter { [.bitcoinSS, .bitcoinMS].contains($0.networkType) && !$0.hidden } }
    var liquidAccounts: [WalletItem] { wm.subaccounts.filter { [.liquidSS, .liquidMS].contains($0.networkType) && !$0.hidden } }
    var lightningAccount: WalletItem? { wm.lightningSubaccount }
    var isBip21: Bool { input.starts(with: "bitcoin:") || input.starts(with: "liquidnetwork:") || input.starts(with: "lightning:")}
    
    // outputs
    var lightningType: InputType?
    var createTx: CreateTransaction?
    var account: WalletItem?
    
    private static var IgnorableErrors = [
        "id_invalid_amount",
        "id_no_amount_specified",
        "id_insufficient_funds",
        "id_invalid_asset_id" ]
    
    init(input: String) {
        self.input = input
    }

    private func parseLightning() async throws -> CreateTransaction? {
        guard let session = lightningAccount?.lightningSession else {
            return nil
        }
        lightningType = session.lightBridge?.parseBoltOrLNUrl(input: input)
        if lightningType == nil {
            return nil
        }
        if txType == .transaction {
            return nil
        }
        let res = try? await session.parseTxInput(input, satoshi: nil, assetId: nil)
        account = lightningAccount
        if res?.isValid ?? false {
            return CreateTransaction(addressee: res?.addressees.first)
        } else {
            throw ParserError.InvalidTransaction(res?.errors.first ?? "id_operation_failure")
        }
    }
    
    private func parseGdkBitcoin(preferredAccount: WalletItem?) async throws -> CreateTransaction? {
        var account = preferredAccount ?? bitcoinAccounts.first
        if let network = preferredAccount?.networkType, network.lightning || network.liquid {
            account = bitcoinAccounts.first
        }
        if let session = account?.session {
            let res = try await parseGdk(for: session)
            self.account = account
            return res
        }
        return nil
    }

    private func parseGdkLiquid(preferredAccount: WalletItem?) async throws -> CreateTransaction? {
        var account = preferredAccount ?? liquidAccounts.first
        if let network = preferredAccount?.networkType, network.lightning || !network.liquid {
            account = liquidAccounts.first
        }
        if let session = account?.session {
            let res = try await parseGdk(for: session)
            self.account = account
            return res
        }
        return nil
    }
    
    private func parseGdk(for session: SessionManager) async throws -> CreateTransaction? {
        let res = try await session.parseTxInput(self.input, satoshi: nil, assetId: nil)
        if res.isValid {
            return CreateTransaction(addressee: res.addressees.first)
        } else if let error = res.errors.first {
            if Parser.IgnorableErrors.contains(error) {
                let addressee = Addressee.from(address: self.input, satoshi: nil, assetId: nil)
                return CreateTransaction(addressee: addressee)
            } else {
                throw ParserError.InvalidTransaction(error)
            }
        }
        return nil
    }
    
    func runSingleAccount(account: WalletItem) async throws {
        if account.networkType.lightning {
            createTx = try await parseLightning()
            return
        }
        if let session = account.session {
            createTx = try await parseGdk(for: session)
            return
        }
        throw ParserError.InvalidNetwork("Wrong network")
    }
    
    func runMultiAccounts(preferredAccount: WalletItem?) async throws {
        if !isBip21 || input.starts(with: "lightning:") {
            if let res = try await parseLightning() {
                createTx = res
                return
            }
        } else if !isBip21 || input.starts(with: "liquidnetwork:") {
            do {
                if let res = try await parseGdkLiquid(preferredAccount: preferredAccount) {
                    createTx = res
                    return
                }
            } catch {
                if isBip21 && input.starts(with: "liquidnetwork:") {
                    throw error
                }
                switch error {
                case ParserError.InvalidTransaction(let txt):
                    if txt != "id_invalid_address" && txt != "id_nonconfidential_addresses_not" {
                        throw error
                    }
                default:
                    break
                }
            }
        } else if !isBip21 || input.starts(with: "bitcoin:") {
            do {
                if let res = try await parseGdkBitcoin(preferredAccount: preferredAccount) {
                    createTx = res
                    return
                }
            } catch {
                if isBip21 && input.starts(with: "bitcoin:") {
                    throw error
                }
                switch error {
                case ParserError.InvalidTransaction(let txt):
                    if txt != "id_invalid_address" {
                        throw error
                    }
                default:
                    break
                }
            }
        } else {
            throw ParserError.InvalidInput("Invalid text")
        }
    }
    
    var txType: TxType {
        switch lightningType {
        case .bolt11(_): return .bolt11
        case .lnUrlPay(_): return .lnurl
        default: return .transaction
        }
    }
}
