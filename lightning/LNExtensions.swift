import Foundation
import BreezSDK
import UIKit

extension LnInvoice {
    public var amountSatoshi: UInt64? { isAmountLocked ? amountMsat! / 1000 : nil }
    public var isAmountLocked: Bool { amountMsat != nil }
    public var expireIn: TimeInterval { TimeInterval(timestamp + expiry) }
    public var expireInAsDate: Date { Date(timeIntervalSince1970: expireIn) }
    public var timeUntilExpiration: Double { Date().distance(to: expireInAsDate) }
    public var isExpired: Bool { timeUntilExpiration < 0 }
    public func sendableSatoshi(userSatoshi: UInt64?) -> UInt64? {
        isAmountLocked ? amountSatoshi ?? 0 : userSatoshi
    }
}

extension LnUrlWithdrawRequestData {
    public var maxWithdrawableSatoshi: UInt64 { maxWithdrawable / 1000 }
    public var minWithdrawableSatoshi: UInt64 { minWithdrawable / 1000 }
}

extension LnUrlWithdrawRequestData {
    public var domain: String? { URL(string: callback)?.host }
}

extension LnUrlPayRequestData {
    public func sendableSatoshi(userSatoshi: UInt64?) -> UInt64? {
        isAmountLocked ? maxSendable : userSatoshi
    }
    public var isAmountLocked: Bool { minSendable == maxSendable }
    public var maxSendableSatoshi: UInt64 { maxSendable / 1000 }
    public var minSendableSatoshi: UInt64 { minSendable / 1000 }
    public var metadata: [[String]]? {
        let data = metadataStr.data(using: .utf8)
        return try? JSONSerialization.jsonObject(with: data ?? Data(), options : .allowFragments) as? [[String]]
    }
}

extension NodeState {
    public var channelsBalanceSatoshi: UInt64 { channelsBalanceMsat / 1000 }
    public var onchainBalanceSatoshi: UInt64 { onchainBalanceMsat / 1000 }
    public var maxReceivableSatoshi: UInt64 { maxReceivableMsat / 1000 }
    public var inboundLiquiditySatoshi: UInt64 { inboundLiquidityMsats / 1000 }
    public var maxPaybleSatoshi: UInt64 { maxPayableMsat / 1000 }
    public var maxSinglePaymentAmountSatoshi: UInt64 { maxSinglePaymentAmountMsat / 1000 }
    
}

extension Payment {
    public var amountSatoshi: Int64 { Int64(amountMsat / 1000) * ((paymentType == PaymentType.received) ? 1 : -1) }
}

extension LspInformation {
    public var channelMinimumFeeSatoshi: Int64 { channelMinimumFeeMsat / 1000 }
    public var channelFeePercent: Float { Float(channelFeePermyriad) / 100 }
}

extension Array<Array<String>>? {
    public var lnUrlPayDescription: String? {
        self?.first { "text/plain" == $0.first }?
            .last
    }
    public var lnUrlPayImage: UIImage? {
        guard let base64 = self?.first(where: { "image/png;base64" == $0.first })?.last else { return nil }
        return [base64]
            .compactMap { Data($0.utf8).base64EncodedData() }
            .compactMap { UIImage(data: $0) }
            .first
    }
}
