import Foundation
import UIKit

struct WOCellModel {
    let img: UIImage
    let title: String
    let hint: String
}

class WOSelectViewModel {

    var types: [WOCellModel]

    init() {

        self.types = [
            WOCellModel(img: UIImage(named: "ic_key_ss")!,
                        title: "id_singlesig".localized,
                        hint: "id_enter_your_xpub_to_add_a".localized),
            WOCellModel(img: UIImage(named: "ic_key_ms")!,
                        title: "id_multisig_shield".localized,
                        hint: "id_log_in_to_your_multisig_shield".localized)
        ]
    }
}
