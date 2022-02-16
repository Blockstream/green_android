import UIKit

class WordCell: UICollectionViewCell {

    @IBOutlet weak var lblNum: UILabel!
    @IBOutlet weak var lblWord: UILabel!

    func configure(num: Int, word: String) {
        self.lblWord.text = word
        self.lblNum.text = "\(num + 1)"
    }
}
