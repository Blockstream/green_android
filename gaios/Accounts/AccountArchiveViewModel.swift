import Foundation
import UIKit

import gdk

class AccountArchiveViewModel {

    var wm: WalletManager { WalletManager.current! }

    /// load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { $0.hidden }
    }

    /// cell models
    var accountCellModels = [AccountCellModel]()

    func loadSubaccounts() async throws {
        let subaccounts = try await wm.subaccounts().filter { $0.hidden }
        let res = try? await wm.balances(subaccounts: subaccounts)
        self.accountCellModels = subaccounts.map { AccountCellModel(subaccount: $0, satoshi: $0.btc) }
    }

    func unarchiveSubaccount(_ subaccount: WalletItem) async throws {
        guard let session = WalletManager.current?.sessions[subaccount.gdkNetwork.network] else { return }
        try? await session.updateSubaccount(subaccount: subaccount.pointer, hidden: false)
        try? await loadSubaccounts()
    }
}
