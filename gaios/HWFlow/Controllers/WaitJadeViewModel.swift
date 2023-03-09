import Foundation
import UIKit

struct WaitJadeStep {
    let img: UIImage
    let titleStep: String
    let title: String
    let hint: String
}

class WaitJadeViewModel {

    var steps: [WaitJadeStep]

    init() {

        self.steps = [
            WaitJadeStep(img: UIImage(named: "il_jade_wait_1")!,
                         titleStep: "id_step".localized.uppercased() + " 1",
                         title: "id_power_on_jade".localized,
                         hint: "id_hold_the_green_button_on_the".localized),
            WaitJadeStep(img: UIImage(named: "il_jade_wait_2")!,
                         titleStep: "id_step".localized.uppercased() + " 2",
                         title: "id_follow_the_instructions_on_jade".localized,
                         hint: "id_select_initalize_to_create_a".localized),
            WaitJadeStep(img: UIImage(named: "il_jade_wait_3")!,
                         titleStep: "id_step".localized.uppercased() + " 3",
                         title: "id_connect_using_usb_or_bluetooth".localized,
                         hint: "id_choose_a_usb_or_bluetooth".localized)
        ]
    }
}
