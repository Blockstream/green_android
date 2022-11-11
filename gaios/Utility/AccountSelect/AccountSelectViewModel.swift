import Foundation
import UIKit
import PromiseKit

class AccountSelectViewModel {

    var accounts: [WalletItem]
    var cachedBalance: [(String, Int64)]

    var accountSelectCellModels: [AccountSelectCellModel] = []
    var accountSelectCellModelsFilter: [AccountSelectCellModel] = []

    init(accounts: [WalletItem], cachedBalance: [(String, Int64)]) {
        self.accounts = accounts
        self.cachedBalance = cachedBalance
        self.accountSelectCellModels = accounts.map { AccountSelectCellModel(account: $0) }
        self.accountSelectCellModelsFilter = accountSelectCellModels
    }

    /// reload by section with animation
    var reloadSections: (([AccountSelectSection], Bool) -> Void)?
}
