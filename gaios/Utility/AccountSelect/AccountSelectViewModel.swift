import Foundation
import UIKit
import PromiseKit

class AccountSelectViewModel {

    var accounts: [WalletItem]

    var accountSelectCellModels: [AccountSelectCellModel] = []
    var accountSelectCellModelsFilter: [AccountSelectCellModel] = []

    init(accounts: [WalletItem]) {
        self.accounts = accounts
        self.accountSelectCellModels = accounts.map { AccountSelectCellModel(account: $0) }
        self.accountSelectCellModelsFilter = accountSelectCellModels
    }

    /// reload by section with animation
    var reloadSections: (([AccountSelectSection], Bool) -> Void)?
}
