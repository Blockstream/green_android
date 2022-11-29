import UIKit

enum Enable2faPrefs: Int, CaseIterable {
    case add

    static func getItems() -> [DialogEnable2faCellModel] {
        return [DialogEnable2faCellModel(type: .enable2fa)]
    }
}
