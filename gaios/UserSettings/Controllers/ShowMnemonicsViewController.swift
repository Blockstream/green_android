import Foundation
import UIKit
import gdk


class ShowMnemonicsViewController: UIViewController {

    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var collectionView: UICollectionView!
    var items: [String] = []
    var bip39Passphrase: String?
    var showBip85: Bool = false
    var credentials: Credentials? {
        didSet {
            items = credentials?.mnemonic?.split(separator: " ").map(String.init) ?? []
        }
    }
    var lightningMnemonic: String? {
        didSet {
            items = lightningMnemonic?.split(separator: " ").map(String.init) ?? []
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_recovery_phrase", comment: "")
        lblHint.text = "id_the_recovery_phrase_can_be_used".localized
        if credentials != nil {
            self.collectionView.reloadData()
            return
        }
        Task {
            do {
                self.credentials = try await WalletManager.current?.prominentSession?.getCredentials(password: "")
                self.bip39Passphrase = credentials?.bip39Passphrase
                if showBip85, let credentials = self.credentials {
                    self.lightningMnemonic = try await WalletManager.current?.lightningSession?.getLightningMnemonic(credentials: credentials)
                }
                await MainActor.run {
                    self.collectionView.reloadData()
                }
            } catch {
                print(error)
            }
        }
    }

    func magnifyQR() {
        let stb = UIStoryboard(name: "Utility", bundle: nil)
        if let vc = stb.instantiateViewController(withIdentifier: "MagnifyQRViewController") as? MagnifyQRViewController {
            vc.qrTxt = self.items.joined(separator: " ")
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        }
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
              if showBip85 {
                  fView.configureBip85(mnemonic: self.items.joined(separator: " ")) {
                      [weak self] in self?.magnifyQR()
                  }
                  return fView
              } else {
                  fView.configure(mnemonic: self.items.joined(separator: " "),
                                  bip39Passphrase: self.bip39Passphrase) {
                      [weak self] in self?.magnifyQR()
                  }
                  return fView
              }
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
