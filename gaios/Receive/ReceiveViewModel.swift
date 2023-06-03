import Foundation
import UIKit
import PromiseKit
import gdk
import greenaddress
import hw
import BreezSDK
import lightning

enum ReceiveType: Int, CaseIterable {
    case address
    case swap
    case bolt11
}

class ReceiveViewModel {
    
    var accounts: [WalletItem]
    var asset: String
    var satoshi: Int64?
    var isFiat: Bool = false
    var account: WalletItem
    var type: ReceiveType
    var description: String?
    var address: Address?
    var invoice: LnInvoice?
    var swap: SwapInfo?

    var wm: WalletManager { WalletManager.current! }
    let bgq = DispatchQueue.global(qos: .background)

    init(account: WalletItem, accounts: [WalletItem]) {
        self.account = account
        self.accounts = accounts
        self.asset = account.gdkNetwork.getFeeAsset()
        self.type = account.gdkNetwork.lightning ? .bolt11 : .address
    }

    func accountType() -> String {
        return account.localizedName
    }

    func newAddress() -> Promise<Void> {
        switch type {
        case .address:
            address = nil
            return Guarantee()
                .compactMap { self.wm.sessions[self.account.gdkNetwork.network] }
                .then(on: bgq) { $0.getReceiveAddress(subaccount: self.account.pointer) }
                .compactMap { self.address = $0 }
                .asVoid()
        case .bolt11:
            invoice = nil
            if satoshi == nil {
                return Promise().asVoid()
            }
            return Guarantee()
                .compactMap { self.wm.lightningSession }
                .compactMap(on: bgq) { try $0.createInvoice(satoshi: UInt64(self.satoshi ?? 0), description: self.description ?? "") }
                .compactMap { self.invoice = $0 }
                .asVoid()
        case .swap:
            swap = nil
            return Guarantee()
                .compactMap { self.wm.lightningSession?.lightBridge }
                .compactMap(on: bgq) { $0.receiveOnchain() }
                .compactMap { self.swap = $0 }
                .asVoid()
        }
    }

    func isBipAddress(_ addr: String) -> Bool {
        let session = wm.sessions[account.gdkNetwork.network]
        return session?.validBip21Uri(uri: addr) ?? false
    }

    func validateHw() -> Promise<Bool> {
        let hw: HWProtocol = wm.account.isLedger ? Ledger.shared : Jade.shared
        let chain = account.gdkNetwork.chain
        guard let addr = address else {
            return Promise() { $0.reject(GaError.GenericError()) }
        }
        return Guarantee()
            .then(on: bgq) { Address.validate(with: self.account, hw: hw, addr: addr, network: chain) }
            .compactMap { return self.address?.address == $0 }
    }

    func addressToUri(address: String, satoshi: Int64) -> String {
        var ntwPrefix = "bitcoin"
        if account.gdkNetwork.liquid {
            ntwPrefix = account.gdkNetwork.mainnet ? "liquidnetwork" :  "liquidtestnet"
        }
        if satoshi == 0 {
            return address
        }
        return String(format: "%@:%@?amount=%.8f", ntwPrefix, address, toBTC(satoshi))
    }

    func toBTC(_ satoshi: Int64) -> Double {
        return Double(satoshi) / 100000000
    }

    var amountCellModel: LTAmountCellModel {
        var amountCell = LTAmountCellModel()
        let nodeState = account.lightningSession?.nodeState
        let lspInfo = account.lightningSession?.lspInfo
        amountCell.isFiat = isFiat
        amountCell.satoshi = satoshi
        amountCell.maxLimit = nodeState?.maxReceivableSatoshi
        amountCell.channelFeePercent =  lspInfo?.channelFeePercent
        amountCell.channelMinFee =  lspInfo?.channelMinimumFeeSatoshi
        amountCell.inboundLiquidity =  nodeState?.inboundLiquiditySatoshi
        return amountCell
    }

    var infoReceivedAmountCellModel: LTInfoCellModel {
        if let invoice = invoice, let satoshi = invoice.amountSatoshi {
            if let balance = Balance.fromSatoshi(Int64(satoshi), assetId: "btc") {
                let (value, denom) = balance.toDenom()
                let (fiat, currency) = balance.toFiat()
                return LTInfoCellModel(title: "Amount to Receive", hint1: "\(value) \(denom)", hint2: "\(fiat) \(currency)")
            }
        }
        return LTInfoCellModel(title: "Amount to Receive", hint1: "", hint2: "")
    }

    var infoExpiredInCellModel: LTInfoCellModel {
        if let invoice = invoice {
            let numberOfDays = Calendar.current.dateComponents([.day], from: invoice.expireInAsDate, to: Date())
            return LTInfoCellModel(title: "Expiration", hint1: "In \(abs(numberOfDays.day ?? 0)) days", hint2: "")
        }
        return LTInfoCellModel(title: "Expiration", hint1: "", hint2: "")
    }

    var noteCellModel: LTNoteCellModel {
        return LTNoteCellModel(note: description ?? "Some note")
    }

    var assetCellModel: ReceiveAssetCellModel {
        return ReceiveAssetCellModel(assetId: asset, account: account)
    }

    var text: String? {
        switch type {
        case .bolt11:
            if let txt = invoice?.bolt11 {
                return "lightning:\( (txt).uppercased() )"
            }
            return nil
        case .swap:
            return swap?.bitcoinAddress
        case .address:
            var text = address?.address
            if let address = address?.address, let satoshi = satoshi {
                text = addressToUri(address: address, satoshi: satoshi)
            }
            return text
        }
    }

    var addressCellModel: ReceiveAddressCellModel {
        return ReceiveAddressCellModel(text: text, tyoe: type)
    }
}
