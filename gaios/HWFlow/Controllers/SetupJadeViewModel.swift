import Foundation
import UIKit

struct SetupJadeStep {
    let img: UIImage
    let titleStep: String
    let title: String
    let hint: String
}

class SetupJadeViewModel {

    var steps: [SetupJadeStep]

    init() {

        self.steps = [
            SetupJadeStep(img: UIImage(named: "il_jade_setup_1")!,
                         titleStep: "id_step".localized.uppercased() + " 1",
                         title: "id_initialize_and_create_wallet".localized,
                         hint: "id_select_initialize_and_choose_to".localized),
            SetupJadeStep(img: UIImage(named: "il_jade_setup_2")!,
                         titleStep: "id_step".localized.uppercased() + " 2",
                         title: "id_backup_recovery_phrase".localized,
                         hint: "id_write_down_your_recovery_phrase".localized),
            SetupJadeStep(img: UIImage(named: "il_jade_setup_3")!,
                         titleStep: "id_step".localized.uppercased() + " 3",
                         title: "id_verify_recovery_phrase".localized,
                         hint: "id_use_the_jogwheel_to_select_the".localized)
        ]
    }
}
