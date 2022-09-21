import Foundation
import UIKit
import PromiseKit

class ACViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    // load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { !($0.hidden ?? false) }
    }

    var reloadTableView: (() -> Void)?

    var accountCellModels = [ACAccountCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    func getSubaccounts(assetId: String) {
        let subaccounts = self.subaccounts.filter {
            (assetId == "btc" && !$0.gdkNetwork.liquid) ||
            (assetId != "btc" && $0.gdkNetwork.liquid)
        }
        wm.balances(subaccounts: subaccounts)
            .done { _ in
                self.accountCellModels = subaccounts.map {
                        ACAccountCellModel(subaccount: $0,
                                           assetId: assetId,
                                           satoshi: $0.satoshi?[assetId] ?? 0)
                    }
            }.catch { err in
                print(err)
            }
    }

    func getAccountCellModels(at indexPath: IndexPath) -> ACAccountCellModel {
        return accountCellModels[indexPath.row]
    }
}
