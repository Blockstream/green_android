import Foundation
import UIKit
import PromiseKit

class WalletsViewController: UICollectionViewController, UICollectionViewDelegateFlowLayout {

    var wallets = [WalletItem]()
    var isSweep: Bool = false
    var wallet: WalletItem!
    weak var subaccountDelegate: SubaccountDelegate?
    private let cellId = "cell"
    private let miniId = "mini"
    private let headerId = "header"
    private let footerId = "footer"

    override func viewDidLoad() {
        super.viewDidLoad()
        guard let collectionView = self.collectionView else { return }
        let cellNib = UINib(nibName: "WalletCardView", bundle: nil)
        let headerNib = UINib(nibName: "HeaderWalletsCollection", bundle: nil)
        let footerNib = UINib(nibName: "FooterWalletsCollection", bundle: nil)
        let walletMiniNib = UINib(nibName: "WalletMiniCollection", bundle: nil)
        collectionView.register(cellNib, forCellWithReuseIdentifier: cellId)
        collectionView.register(walletMiniNib, forCellWithReuseIdentifier: miniId)
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
        reloadData()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: true)
    }

    func reloadData() {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            getSubaccounts()
        }.ensure {
            self.stopAnimating()
        }.done { wallets in
            self.wallets = wallets
            self.collectionView?.reloadData()
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    override func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }

    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return wallets.count
    }

    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if isSweep {
            guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: miniId, for: indexPath) as? WalletMiniCollection else { fatalError("Fail to dequeue reusable cell") }
            let wallet = wallets[indexPath.row]
            cell.nameLabel.text = wallet.localizedName()
            return cell
        } else {
            guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: cellId, for: indexPath) as? WalletCardView else { fatalError("Fail to dequeue reusable cell") }
            let wallet = wallets[indexPath.row]

            let balance = Balance.convert(details: ["satoshi": wallet.btc.satoshi])!
            let (amount, denom) = balance.get(tag: "btc")
            cell.balance.text = amount
            cell.unit.text = denom
            cell.balanceFiat.text = "≈ \(balance.fiat) \(balance.fiatCurrency) "
            cell.walletName.text = wallet.localizedName()
            let network = getGdkNetwork(getNetwork())
            cell.networkImage.image = UIImage(named: network.icon!)
            return cell
        }
    }

    override func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        switch kind {
        case UICollectionView.elementKindSectionHeader:
            guard let header = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier:
                headerId, for: indexPath) as? HeaderWalletsCollection else { fatalError("Fail to dequeue reusable cell") }
            let satoshi = wallets.map { $0.btc.satoshi }.reduce(0) { (accumulation: UInt64, nextValue: UInt64) -> UInt64 in
                return accumulation + nextValue
            }
            header.title.text = isSweep ? NSLocalizedString("id_where_would_you_like_to", comment: ""): NSLocalizedString("id_total_balance", comment: "")
            let balance = Balance.convert(details: ["satoshi": satoshi])!
            let (amount, denom) = balance.get(tag: "btc")
            header.btcLabel.text = isSweep ? "" : "\(amount) \(denom)"
            header.fiatLabel.text = isSweep ? "" : "\(balance.fiat) \(balance.fiatCurrency)"
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
        return CGSize(width: width, height: isSweep ? 52 : 184)
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
        let alert = UIAlertController(title: NSLocalizedString("id_add_simple_account", comment: ""), message: NSLocalizedString("id_simple_accounts_allow_you_to", comment: ""), preferredStyle: .alert)
        alert.addTextField { textField in
            textField.placeholder = NSLocalizedString("id_name", comment: "")
            textField.keyboardType = .asciiCapable
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_create", comment: ""), style: .default) { _ in
            let textField = alert.textFields!.first
            self.createWallet(name: textField!.text!)
        })
        self.present(alert, animated: true, completion: nil)
    }

    func createWallet(name: String) {
        let bgq = DispatchQueue.global(qos: .background)
        let session = getGAService().getSession()
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try session.createSubaccount(details: ["name": name, "type": "2of2"])
        }.compactMap(on: bgq) { call in
            call.resolve(self)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
        }.catch { e in
            Toast.show(e.localizedDescription)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let controller = segue.destination as? SendBtcViewController {
            controller.isSweep = true
            controller.wallet = wallet
        }
    }
}
