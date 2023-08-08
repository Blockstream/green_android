import UIKit

protocol LTRecoverFundsFeeDelegate {
    func didChange(feeSliderIndex: Int)
}

class LTRecoverFundsFeeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblFeeAmount: UILabel!
    @IBOutlet weak var feeSlider: UISlider!
    @IBOutlet weak var lblLegend: UILabel!

    class var identifier: String { return String(describing: self) }
    var feeSliderMaxIndex: Int = 3
    var delegate: LTRecoverFundsFeeDelegate?

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        [lblFeeAmount].forEach {
            $0?.setStyle(.txtCard)
        }
    }

    override func prepareForReuse() {
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(feeRate: String, feeSliderIndex: Int, feeSliderMaxIndex: Int = 3) {
        lblFeeAmount.text = feeRate
        self.feeSliderMaxIndex = feeSliderMaxIndex
        setFeeSlider(feeSliderIndex)
    }
    
    func setFeeSlider(_ index: Int) {
        let legend = ["Economy", "Hour", "Half Hour", "Fast"]
        feeSlider.value = Float(index)
        lblLegend.text = legend[index]
    }
    
    @IBAction func didValueChange(_ sender: UISlider) {
        let index = Int(sender.value)
        setFeeSlider(index)
        delegate?.didChange(feeSliderIndex: index)
    }
}
