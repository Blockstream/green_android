import Foundation
import UIKit

class DialogViewController: KeyboardViewController {
    
    lazy var blurredView: UIView = {
        let containerView = UIView()
        let blurEffect = UIBlurEffect(style: .dark)
        let customBlurEffectView = CustomVisualEffectView(effect: blurEffect, intensity: 0.4)
        customBlurEffectView.frame = self.view.bounds

        let dimmedView = UIView()
        dimmedView.backgroundColor = .black.withAlphaComponent(0.3)
        dimmedView.frame = self.view.bounds
        containerView.addSubview(customBlurEffectView)
        containerView.addSubview(dimmedView)
        return containerView
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)
        view.alpha = 0.0
    }
}
