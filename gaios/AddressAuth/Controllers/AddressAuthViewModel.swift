import Foundation
import UIKit

import gdk

class AddressAuthViewModel {

    var listCellModelsFilter: [AddressAuthCellModel] = []
    private var listCellModels: [AddressAuthCellModel] = []

    var lastPointer: Int?
    var isLoading = false

    var wallet: WalletItem
    
    init(wallet: WalletItem) {
        self.wallet = wallet
    }

    func isReady() -> Bool {
        return lastPointer == nil && listCellModels.count > 0
    }

    func fetchData(reset: Bool) async throws {
        if isLoading { return }
        if isReady() { return }
        if reset {
            listCellModelsFilter = []
            listCellModels = []
            lastPointer = nil
        }
        isLoading = true
        let params = GetPreviousAddressesParams(subaccount: Int(wallet.pointer), lastPointer: lastPointer)
        let res = try await wallet.session?.getPreviousAddresses(params)
        isLoading = false
        lastPointer = res?.lastPointer
        let newModels = res?.list.compactMap { AddressAuthCellModel(address: $0.address ?? "",
                                                                     tx: $0.txCount  ?? 0,
                                                                     canSign: canSign()) } ?? []
        listCellModels += newModels
        listCellModelsFilter = listCellModels
    }

    func search(_ txt: String?) {
        listCellModelsFilter = []
        listCellModels.forEach {
            if let txt = txt, txt.count > 0 {
                if ($0.address).lowercased().contains(txt.lowercased()) {
                    listCellModelsFilter.append($0)
                }
            } else {
                listCellModelsFilter.append($0)
            }
        }
    }

    func canSign() -> Bool {
        return wallet.gdkNetwork.electrum == true && wallet.gdkNetwork.liquid == false
    }
}
