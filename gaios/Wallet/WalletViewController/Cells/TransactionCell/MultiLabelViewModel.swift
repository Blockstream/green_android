import UIKit

enum MultiLabelStyle: Int, CaseIterable {
    case simple
    case amountIn
    case amountOut
}

struct MultiLabelViewModel {
    let txtLeft: String?
    let txtRight: String?
    let style: MultiLabelStyle
}
