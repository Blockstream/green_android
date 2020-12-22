import UIKit

class CsvTimeCell: UITableViewCell {

    @IBOutlet weak var csvTimeLabel: UILabel!
    var csvType: Settings.CsvTime!

    weak var delegate: CsvTypeInfoDelegate?

    override func selectable(_ selectable: Bool) {
        super.selectable(selectable)
        isUserInteractionEnabled = true
    }

    func configure(csvType: Settings.CsvTime, delegate: CsvTypeInfoDelegate) {
        self.delegate = delegate
        self.csvType = csvType
        csvTimeLabel.text = csvType.label()
    }

    @IBAction func infoTapped(_ sender: Any) {
        if let delegate = self.delegate {
            delegate.didTapInfo(csv: self.csvType)
        }
    }
}

protocol CsvTypeInfoDelegate: class {
    func didTapInfo(csv: Settings.CsvTime)
}
