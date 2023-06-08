import Foundation
import UIKit


enum OnBoardInfoCellType: String, CaseIterable {
    case environment
    case sensitive
    case safety
}

class OnBoardInfoViewModel {

    var items: [OnBoardInfoCellModel] {
        return [
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_home")!, title: "id_safe_environment".localized, hint: "id_make_sure_you_are_alone_and_no".localized),
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_warn")!, title: "id_sensitive_information".localized, hint: "id_whomever_can_access_your".localized),
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_shield")!, title: "id_safely_stored".localized, hint: "id_if_you_forget_it_or_lose_it".localized)
        ]
    }
}
