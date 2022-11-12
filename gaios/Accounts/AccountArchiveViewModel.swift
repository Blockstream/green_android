import Foundation
import UIKit
import PromiseKit

class AccountArchiveViewModel {

    var wm: WalletManager { WalletManager.current! }

    /// load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { $0.hidden ?? false }
    }

    /// reload by section with animation
    var reloadSections: (([AccountArchiveSection], Bool) -> Void)?

    /// cell models
    var accountCellModels = [AccountCellModel]() {
        didSet {
            reloadSections?([AccountArchiveSection.account], false)
        }
    }

    func loadSubaccounts() {
        wm.subaccounts()
            .compactMap { $0.filter { $0.hidden ?? false } }
            .then { self.wm.balances(subaccounts: $0) }
            .done { _ in
                self.accountCellModels = self.subaccounts.map { AccountCellModel(subaccount: $0) }
            }.catch { err in
                print(err)
            }
    }

    func unarchiveSubaccount(_ subaccount: WalletItem) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = WalletManager.current?.sessions[subaccount.gdkNetwork.network] else { return }
        session.updateSubaccount(subaccount: subaccount.pointer, hidden: false)
            .done { _ in self.loadSubaccounts() }
            .catch { e in print(e.localizedDescription) }
    }

}
