import Foundation
import UIKit
import PromiseKit

class AccountSelectViewModel {

    var accounts: [WalletItem]

    var accountSelectCellModels: [AccountSelectCellModel] = []
    var accountSelectCellModelsFilter: [AccountSelectCellModel] = []
    var ampWarn: String?

    init(accounts: [WalletItem], ampWarn: String?) {
        self.accounts = accounts
        self.accountSelectCellModels = accounts.map { AccountSelectCellModel(account: $0) }
        self.accountSelectCellModelsFilter = accountSelectCellModels
        self.ampWarn = ampWarn
    }

    /// reload by section with animation
    var reloadSections: (([AccountSelectSection], Bool) -> Void)?
}
