import Foundation
import UIKit

import gdk

class AddressAuthViewModel {

    var listCellModelsFilter: [AddressAuthCellModel] = []
    private var listCellModels: [AddressAuthCellModel] = []
    
    init() {
        loadMock()
    }

    func loadMock() {
        for n in 1...10 {
            listCellModels.append(AddressAuthCellModel(address: "3FZbgi29cpjq2GjdwV8eyHuJJnkLtktZc5", tx: n))
        }
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
}
