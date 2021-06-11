import Foundation
import UIKit
import PromiseKit

protocol SubaccountDelegate: AnyObject {
    func onChange(_ wallet: WalletItem)
}

class AccountsViewController: UICollectionViewController, UICollectionViewDelegateFlowLayout {

    var isSweep: Bool = false
    weak var subaccountDelegate: SubaccountDelegate?
    private var wallets = [WalletItem]()
    private var wallet: WalletItem!
    private let cellId = "cell"
    private let miniId = "mini"
    private let headerId = "header"
    private let footerId = "footer"

    private let network = AccountsManager.shared.current?.gdkNetwork
    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        guard let collectionView = self.collectionView else { return }
        let cellNib = UINib(nibName: "AccountCardView", bundle: nil)
        let headerNib = UINib(nibName: "AccountsHeaderCollection", bundle: nil)
        let footerNib = UINib(nibName: "AccountsFooterCollection", bundle: nil)
        let walletMiniNib = UINib(nibName: "AccountMiniCollection", bundle: nil)
        collectionView.register(cellNib, forCellWithReuseIdentifier: cellId)
        collectionView.register(walletMiniNib, forCellWithReuseIdentifier: miniId)
        collectionView.register(headerNib, forSupplementaryViewOfKind:
            UICollectionView.elementKindSectionHeader, withReuseIdentifier: headerId)
        collectionView.register(footerNib, forSupplementaryViewOfKind:
            UICollectionView.elementKindSectionFooter, withReuseIdentifier: footerId)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
//        navigationController?.setNavigationBarHidden(!isSweep, animated: true)
        title = ""
        reloadData()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
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
            guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: miniId, for: indexPath) as? AccountMiniCollection else { fatalError("Fail to dequeue reusable cell") }
            let wallet = wallets[indexPath.row]
            cell.nameLabel.text = wallet.localizedName()
            return cell
        } else {
            guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: cellId, for: indexPath) as? AccountCardView else { fatalError("Fail to dequeue reusable cell") }
            let wallet = wallets[indexPath.row]

            if let balance = Balance.convert(details: ["satoshi": wallet.btc]) {
                let (amount, denom) = balance.get(tag: btc)
                cell.balance.text = amount
                cell.unit.text = denom
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                cell.balanceFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency) "
            }
            cell.walletName.text = wallet.localizedName()
            cell.networkImage.image = UIImage(named: network?.icon ?? "")
            let accountType: AccountType? = AccountType(rawValue: wallet.type)
            cell.accountTypeLbl.text = accountType?.name ?? ""
            return cell
        }
    }

    override func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        switch kind {
        case UICollectionView.elementKindSectionHeader:
            guard let header = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier:
                headerId, for: indexPath) as? AccountsHeaderCollection else { fatalError("Fail to dequeue reusable cell") }
            let satoshi = wallets.map { $0.btc }.reduce(0) { (accumulation: UInt64, nextValue: UInt64) -> UInt64 in
                return accumulation + nextValue
            }
            header.title.text = isSweep ? NSLocalizedString("id_where_would_you_like_to", comment: ""): NSLocalizedString("id_total_balance", comment: "")
            if let balance = Balance.convert(details: ["satoshi": satoshi]) {
                let (amount, denom) = balance.get(tag: btc)
                header.btcLabel.text = isSweep ? "" : "\(amount ?? "") \(denom)"
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                header.fiatLabel.text = isSweep ? "" : "\(fiat ?? "N.A.") \(fiatCurrency)"
            }
            header.equalsLabel.isHidden = isSweep
            header.bringSubviewToFront(header.stackView)
            return header
        case UICollectionView.elementKindSectionFooter:
            guard let footer = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier:
            footerId, for: indexPath) as? AccountsFooterCollection else { fatalError("Fail to dequeue reusable cell") }
            let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(self.addWallet))
            footer.addGestureRecognizer(tapGestureRecognizer)
            footer.isUserInteractionEnabled = true
            footer.networkImage.image = UIImage(named: network?.icon ?? "")
            return footer
        default:
            return UICollectionReusableView()
        }
    }

    @objc func dismiss() {
        navigationController?.popViewController(animated: true)
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        return CGSize(width: collectionView.frame.size.width, height: isSweep ? 95 : 130)
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
        subaccountDelegate?.onChange(wallet)
        if isSweep {
            performSegue(withIdentifier: "sweep", sender: nil)
        } else {
            dismiss()
        }
    }

    @objc func addWallet(_ sender: Any?) {
//        performSegue(withIdentifier: "create", sender: nil)
        performSegue(withIdentifier: "select-type", sender: nil)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let controller = segue.destination as? SendBtcViewController {
            controller.isSweep = true
            controller.wallet = wallet
        } else if let controller = segue.destination as? AccountCreateViewController {
            controller.subaccountDelegate = subaccountDelegate
            controller.presentationController?.delegate = self
        }
    }
}

extension AccountsViewController: UIAdaptivePresentationControllerDelegate {

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        reloadData()
    }
}
