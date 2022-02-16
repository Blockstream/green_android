import Foundation
import UIKit

class ShowMnemonicsViewController: UIViewController {

    @IBOutlet weak var collectionView: UICollectionView!
    var items: [String] = []
    var mnemonic: String?

    override func viewDidLoad() {
        super.viewDidLoad()

        mnemonic = try? SessionsManager.current.getMnemonicPassphrase(password: "")
        if let words = mnemonic?.split(separator: " ").map(String.init) {
            items = words
        }
        collectionView.reloadData()
        title = NSLocalizedString("id_recovery_phrase", comment: "")
    }
}

extension ShowMnemonicsViewController: UICollectionViewDelegate, UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return items.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "WordCell", for: indexPath) as? WordCell {
            cell.configure(num: indexPath.item, word: items[indexPath.item])
            return cell
        }
        return UICollectionViewCell()
    }

    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
      switch kind {
      case UICollectionView.elementKindSectionFooter:
          if let fView = collectionView.dequeueReusableSupplementaryView(ofKind: kind,
                                                                         withReuseIdentifier: "FooterQrCell",
                                                                         for: indexPath) as? FooterQrCell {
              fView.configure(mnemonic: self.mnemonic)
              return fView
          }
          return UICollectionReusableView()
      default:
          return UICollectionReusableView()
      }
    }
}

extension ShowMnemonicsViewController: UICollectionViewDelegateFlowLayout {
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = (collectionView.bounds.width - 20.0) / 3.0
        let height = 70.0

        return CGSize(width: width, height: height)
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
        return UIEdgeInsets.zero
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumInteritemSpacingForSectionAt section: Int) -> CGFloat {
        return 0
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumLineSpacingForSectionAt section: Int) -> CGFloat {
        return 0
    }
}
