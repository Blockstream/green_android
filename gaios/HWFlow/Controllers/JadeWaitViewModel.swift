import Foundation
import UIKit
import RiveRuntime

struct JadeWaitStep {
    let riveModel: RiveViewModel
    let titleStep: String
    let title: String
    let hint: String
}

class JadeWaitViewModel {

    var steps: [JadeWaitStep]

    init() {

        self.steps = [
            JadeWaitStep(riveModel: RiveModel.animationJade1,
                         titleStep: "id_step".localized.uppercased() + " 1",
                         title: "id_power_on_jade".localized,
                         hint: "id_hold_the_green_button_on_the".localized),
            JadeWaitStep(riveModel: RiveModel.animationJade2,
                         titleStep: "id_step".localized.uppercased() + " 2",
                         title: "id_follow_the_instructions_on_jade".localized,
                         hint: "id_select_initalize_to_create_a".localized),
            JadeWaitStep(riveModel: RiveModel.animationJade3,
                         titleStep: "id_step".localized.uppercased() + " 3",
                         title: "id_connect_with_bluetooth".localized,
                         hint: "id_choose_bluetooth_connection_on".localized)
        ]
    }
}
