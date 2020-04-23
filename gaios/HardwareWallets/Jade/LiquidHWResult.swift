import Foundation

struct LiquidHWResult: Codable {
    let signatures: [String]
    let assetCommitments: [String]
    let valueCommitments: [String]
    let abfs: [String]
    let vbfs: [String]
}
