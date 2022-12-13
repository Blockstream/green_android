import UIKit

class MultiLabelView: UIView {

    @IBOutlet weak var lblLeft: UILabel!
    @IBOutlet weak var lblRight: UILabel!

    func configure(_ model: MultiLabelViewModel) {
        lblLeft.text = model.txtLeft
        lblRight.text = model.txtRight

        switch model.style {
        case .simple:
            [lblLeft, lblRight].forEach {
                $0?.font = .systemFont(ofSize: 12)
                $0?.textColor = UIColor.gGrayTxt()
            }
        case .amountIn:
            [lblLeft, lblRight].forEach {
                $0?.font = .boldSystemFont(ofSize: 14)
            }
            lblLeft.textColor = .white
            lblRight.textColor = UIColor.gGreenMatrix()
        case .amountOut:
            [lblLeft, lblRight].forEach {
                $0?.font = .boldSystemFont(ofSize: 14)
                $0?.textColor = .white
            }
        case .unconfirmed:
            lblLeft.textColor = UIColor.warningYellow()
            lblLeft.font = .systemFont(ofSize: 12)
            [lblRight].forEach {
                $0?.font = .systemFont(ofSize: 12)
                $0?.textColor = UIColor.gGrayTxt()
            }
        case .pending:
            lblLeft.textColor = UIColor.gGrayTxt()
            lblLeft.font = .systemFont(ofSize: 12)
            [lblRight].forEach {
                $0?.font = .systemFont(ofSize: 12)
                $0?.textColor = UIColor.gGrayTxt()
            }
        }
    }
}
