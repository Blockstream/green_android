import UIKit

class SetupJadeViewController: HWFlowBaseViewController {

    @IBOutlet weak var imgDevice: UIImageView!

    @IBOutlet weak var lblStep1Number: UILabel!
    @IBOutlet weak var lblStep1Title: UILabel!
    @IBOutlet weak var lblStep1Hint: UILabel!
    @IBOutlet weak var lblStep2Number: UILabel!
    @IBOutlet weak var lblStep2Title: UILabel!
    @IBOutlet weak var lblStep2Hint: UILabel!
    @IBOutlet weak var lblStep3Number: UILabel!
    @IBOutlet weak var lblStep3Title: UILabel!
    @IBOutlet weak var lblStep3Hint: UILabel!

    @IBOutlet weak var infoBox1: UIView!
    @IBOutlet weak var infoBox2: UIView!
    @IBOutlet weak var infoBox3: UIView!

    @IBOutlet weak var btnExit: UIButton!

    let viewModel = SetupJadeViewModel()
    var timer: Timer?
    var idx = 0

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        loadNavigationBtns()
        update()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        timer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(fireTimer), userInfo: nil, repeats: true)
    }

    override func viewWillDisappear(_ animated: Bool) {
        timer?.invalidate()
    }

    @objc func fireTimer() {
        refresh()
    }

    func setContent() {
        btnExit.setTitle("id_exit_guide".localized, for: .normal)
        lblStep1Number.text = viewModel.steps[0].titleStep
        lblStep2Number.text = viewModel.steps[1].titleStep
        lblStep3Number.text = viewModel.steps[2].titleStep
        lblStep1Title.text = viewModel.steps[0].title
        lblStep2Title.text = viewModel.steps[1].title
        lblStep3Title.text = viewModel.steps[2].title
        lblStep1Hint.text = viewModel.steps[0].hint
        lblStep2Hint.text = viewModel.steps[1].hint
        lblStep3Hint.text = viewModel.steps[2].hint
    }

    func refresh() {

        UIView.animate(withDuration: 0.25, animations: {
            [self.imgDevice].forEach {
                $0?.alpha = 0.0
            }}, completion: { _ in
                if self.idx < 2 { self.idx += 1 } else { self.idx = 0}
                self.update()
                UIView.animate(withDuration: 0.4, animations: {
                    [self.imgDevice].forEach {
                        $0?.alpha = 1.0
                    }
                })
            })
    }

    func update() {
        imgDevice.image = self.viewModel.steps[idx].img
        [lblStep1Hint, lblStep2Hint, lblStep3Hint].forEach {
            $0?.isHidden = true
        }
        [infoBox1, infoBox2, infoBox3].forEach {
            $0?.borderColor = UIColor.gGrayCard()
        }
        switch idx {
        case 0:
            lblStep1Hint.isHidden = false
            infoBox1.borderColor = UIColor.gGreenMatrix()
        case 1:
            lblStep2Hint.isHidden = false
            infoBox2.borderColor = UIColor.gGreenMatrix()
        case 2:
            lblStep3Hint.isHidden = false
            infoBox3.borderColor = UIColor.gGreenMatrix()
        default:
            break
        }
    }

    func loadNavigationBtns() {
        // Troubleshoot
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_troubleshoot".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(troubleshootBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    func setStyle() {
        [infoBox1, infoBox2, infoBox3].forEach {
            $0?.cornerRadius = 5.0
            $0?.borderWidth = 2.0
            $0?.borderColor = UIColor.gGrayCard()
        }
        [lblStep1Number, lblStep2Number, lblStep3Number].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .black)
            $0?.textColor = UIColor.gGreenMatrix()
        }
        [lblStep1Title, lblStep2Title, lblStep3Title].forEach {
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
            $0?.textColor = .white
        }
        [lblStep1Hint, lblStep2Hint, lblStep3Hint].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
            $0?.textColor = .white.withAlphaComponent(0.6)
        }
        btnExit.setStyle(.outlinedWhite)
    }

    @objc func troubleshootBtnTapped() {
        SafeNavigationManager.shared.navigate( ExternalUrls.jadeTroubleshoot )
    }

    @IBAction func btnExit(_ sender: Any) {
        navigationController?.popViewController(animated: true)
    }
}
