import Foundation
import UIKit

struct JadeWaitStep {
    let img: UIImage
    let titleStep: String
    let title: String
    let hint: String
}

class JadeWaitViewModel {

    var steps: [JadeWaitStep]

    init() {

        self.steps = [
            JadeWaitStep(img: UIImage(named: "il_jade_wait_1")!,
                         titleStep: "id_step".localized.uppercased() + " 1",
                         title: "id_power_on_jade".localized,
                         hint: "id_hold_the_green_button_on_the".localized),
            JadeWaitStep(img: UIImage(named: "il_jade_wait_2")!,
                         titleStep: "id_step".localized.uppercased() + " 2",
                         title: "id_follow_the_instructions_on_jade".localized,
                         hint: "id_select_initalize_to_create_a".localized),
            JadeWaitStep(img: UIImage(named: "il_jade_wait_3")!,
                         titleStep: "id_step".localized.uppercased() + " 3",
                         title: "Connect using Bluetooth".localized,
                         hint: "Choose a Bluetooth connection on Jade after verifying your recovery phrase.".localized)
        ]
    }
}
