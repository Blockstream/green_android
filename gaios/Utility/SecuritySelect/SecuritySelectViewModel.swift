import Foundation
import UIKit
import PromiseKit

class SecuritySelectViewModel {

    var assetSelectCellModels: [AssetSelectCellModel] = []
    var selectedAsset: AssetSelectCellModel?

    init(assetSelectCellModels: [AssetSelectCellModel]) {
        self.assetSelectCellModels = assetSelectCellModels

        /// is this correct?
        selectedAsset = self.assetSelectCellModels.first
    }

    /// reload by section with animation
    var reloadSections: (([SecuritySelectSection], Bool) -> Void)?

    var showAll = false {
        didSet {
            reloadSections?([SecuritySelectSection.policy], false)
        }
    }

    /// cell models
    func getPolicyCellModels() -> [PolicyCellModel] {

        let data = [
            PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / LEGACY SEGWIT", name: "Standard", hint: "Simple, portable, standard account, secured by your key, the recovery phrase."),
            PolicyCellModel(isSS: false, isLight: true, type: "Lightning", name: "Instant", hint: "Fast transactions on the Lightning Network, powered by Greenlight."),
            PolicyCellModel(isSS: false, isLight: false, type: "MULTISIG / 2OF2", name: "2FA Protected", hint: "Quick setup 2FA account, ideal for active spenders (2FA expires if you don't move funds every 6 months)."),
            PolicyCellModel(isSS: false, isLight: false, type: "MULTISIG / 2OF3", name: "2of3 with 2FA", hint: "Permanent 2FA account, ideal for long term hodling, optionally with 3rd emergency key on hardware wallet."),
            PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / NATIVE SEGWIT", name: "Native Segwit", hint: "Cheaper singlesig option. Addresses are Native SegWit Bech32."),
            PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / TAPROOT", name: "Taproot", hint: "Cheaper and more private singlesig option. Addresses are Bech32m.")
        ]
        return showAll ? data : Array(data[0...2])
    }
}
