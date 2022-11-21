import Foundation
import UIKit
import PromiseKit

enum OnBoardInfoCellType: String, CaseIterable {
    case environment
    case sensitive
    case safety
}

class OnBoardInfoViewModel {

    var items: [OnBoardInfoCellModel] {
        return [
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_home")!, title: "Safe Environment", hint: "Make sure you are alone and no camera is recording you or the screen."),
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_warn")!, title: "Sensitive Information", hint: "Whomever can access your recovery phrase, can steal your funds."),
            OnBoardInfoCellModel(icon: UIImage(named: "ic_info_shield")!, title: "Safely stored", hint: "If you forget it or lose it, your funds are going to be lost as well.")
        ]
    }
}
