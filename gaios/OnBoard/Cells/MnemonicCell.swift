import UIKit

protocol MnemonicCellDelegate: AnyObject {
    func collectionView(valueChangedIn textField: UITextField, from cell: MnemonicCell)
    func collectionView(pastedIn text: String, from cell: MnemonicCell)
}

class MnemonicCell: UICollectionViewCell {

    @IBOutlet weak var wordLabel: UILabel!
    @IBOutlet weak var wordText: UITextField!

    weak var delegate: MnemonicCellDelegate?

    override func awakeFromNib() {
        super.awakeFromNib()

        wordText.delegate = self
        wordText.borderWidth = 1.0
        wordText.layer.borderColor = UIColor.white.withAlphaComponent(0.24).cgColor
    }

    @IBAction func editingChanged(_ sender: UITextField) {
        delegate?.collectionView(valueChangedIn: sender, from: self)
    }

}

extension MnemonicCell: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if string.isEmpty {
            delegate?.collectionView(pastedIn: string, from: self)
            return true
        }
        let s = string.trimmingCharacters(in: .whitespacesAndNewlines)
        if !s.isEmpty {
            delegate?.collectionView(pastedIn: s, from: self)
            return true
        }
        return false
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        endEditing(true)
        return false
    }

    func textFieldDidBeginEditing(_ textField: UITextField) {
        wordText.layer.borderColor = UIColor.customMatrixGreen().cgColor
    }

    func textFieldDidEndEditing(_ textField: UITextField) {
        wordText.layer.borderColor = UIColor.white.withAlphaComponent(0.24).cgColor
    }
}
