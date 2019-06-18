import Foundation
import UIKit
import PromiseKit

class WalletsViewController: UICollectionViewController, UICollectionViewDelegateFlowLayout {

    var wallets = [WalletItem]()
    var isSweep: Bool = false
    var wallet: WalletItem!
    weak var subaccountDelegate: SubaccountDelegate?
    private let cellId = "cell"
    private let headerId = "header"
    private let footerId = "footer"

    override func viewDidLoad() {
        super.viewDidLoad()
        guard let collectionView = self.collectionView else { return }
        let cellNib = UINib(nibName: "WalletCardView", bundle: nil)
        let headerNib = UINib(nibName: "HeaderWalletsCollection", bundle: nil)
        let footerNib = UINib(nibName: "FooterWalletsCollection", bundle: nil)
        collectionView.register(cellNib, forCellWithReuseIdentifier: cellId)
        collectionView.register(headerNib, forSupplementaryViewOfKind:
            UICollectionView.elementKindSectionHeader, withReuseIdentifier: headerId)
        collectionView.register(footerNib, forSupplementaryViewOfKind:
            UICollectionView.elementKindSectionFooter, withReuseIdentifier: footerId)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if !isSweep {
            navigationController?.setNavigationBarHidden(true, animated: true)
        }
        title = ""
        getSubaccounts().done {[unowned self] wallets in
            self.wallets = wallets
            self.collectionView?.reloadData()
        }.catch {_ in }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: true)
    }

    override func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }

    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return wallets.count
    }

    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: cellId, for: indexPath) as? WalletCardView else { fatalError("Fail to dequeue reusable cell") }
        let wallet = wallets[indexPath.row]
        guard let settings = getGAService().getSettings() else { return cell }
        cell.balance.text = String.toBtc(satoshi: wallet.btc.satoshi, showDenomination: false)
        cell.unit.text = settings.denomination.toString()
        cell.balanceFiat.text = "≈ " + String.toFiat(satoshi: wallet.btc.satoshi)
        cell.walletName.text = wallet.localizedName()
        let network = getGdkNetwork(getNetwork())
        cell.networkImage.image = UIImage(named: network.icon!)
        return cell
    }

    override func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        switch kind {
        case UICollectionView.elementKindSectionHeader:
            guard let header = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier:
                headerId, for: indexPath) as? HeaderWalletsCollection else { fatalError("Fail to dequeue reusable cell") }
            let satoshi = wallets.map { $0.btc.satoshi }.reduce(0) { (accumulation: UInt64, nextValue: UInt64) -> UInt64 in
                return accumulation + nextValue
            }
            header.title.text = isSweep ? NSLocalizedString("id_sweep_where_to_sweep", comment: ""): NSLocalizedString("id_total_balance", comment: "")
            header.btcLabel.text = isSweep ? "" : String.toBtc(satoshi: satoshi)
            header.fiatLabel.text = isSweep ? "" :String.toFiat(satoshi: satoshi)
            header.equalsLabel.isHidden = isSweep
            return header
        case UICollectionView.elementKindSectionFooter:
            guard let footer = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier:
            footerId, for: indexPath) as? FooterWalletsCollection else { fatalError("Fail to dequeue reusable cell") }
            let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(self.addWallet))
            footer.addGestureRecognizer(tapGestureRecognizer)
            footer.isUserInteractionEnabled = true
            let network = getGdkNetwork(getNetwork())
            footer.networkImage.image = UIImage(named: network.icon!)
            return footer
        default:
            return UICollectionReusableView()
        }
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        return CGSize(width: collectionView.frame.size.width, height: 95)
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForFooterInSection section: Int) -> CGSize {
        if getGAService().isWatchOnly || isSweep {
            return CGSize.zero
        }
        return CGSize(width: self.view.frame.width, height: 184)
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let padding = 33
        let width = self.view.frame.width - CGFloat(padding)
        return CGSize(width: width, height: 184)
    }

    override func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        wallet = wallets[indexPath.row]
        subaccountDelegate?.onChange(wallet.pointer)
        if isSweep {
            performSegue(withIdentifier: "sweep", sender: nil)
        } else {
            navigationController?.popViewController(animated: false)
        }
    }

    @objc func addWallet(_ sender: Any?) {
        let alert = UIAlertController(title: "", message: NSLocalizedString("id_new_accounts_functionality", comment: ""), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let controller = segue.destination as? SendBtcViewController {
            controller.isSweep = true
            controller.wallet = wallet
        }
    }
}
