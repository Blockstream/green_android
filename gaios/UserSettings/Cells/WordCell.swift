import UIKit

class WordCell: UICollectionViewCell {

    @IBOutlet weak var lblNum: UILabel!
    @IBOutlet weak var lblWord: UILabel!
    @IBOutlet weak var bg: UIView!

    func configure(num: Int, word: String) {
        self.lblWord.text = word
        self.lblNum.text = "\(num + 1)"
        bg.borderWidth = 1.0
        bg.borderColor = .white.withAlphaComponent(0.2)
        bg.layer.cornerRadius = 3.0
    }
}
