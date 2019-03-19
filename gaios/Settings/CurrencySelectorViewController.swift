import Foundation
import UIKit
import PromiseKit

class CurrencySelectorViewController: KeyboardViewController, UITableViewDelegate, UITableViewDataSource, UITextFieldDelegate {

    @IBOutlet weak var tableView: UITableView!
    var currencyList = [CurrencyItem]()
    var searchCurrencyList = [CurrencyItem]()
    @IBOutlet weak var textField: SearchTextField!
    @IBOutlet weak var currentCurrency: UILabel!
    @IBOutlet weak var currentExchange: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_reference_exchange_rate", comment: "")
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView()
        tableView.separatorColor = UIColor.customTitaniumMedium()
        textField.delegate = self
        textField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_search", comment: ""),
                                                   attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        textField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)

        tableView.rowHeight = 50
        tableView.layer.shadowColor = UIColor.white.cgColor
        tableView.layer.shadowRadius = 4
        tableView.layer.shadowOpacity = 1
        tableView.layer.shadowOffset = CGSize(width: 20, height: 50)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        getCurrentRate()
        getExchangeRate()
    }

    func getCurrentRate() {
        guard let settings = getGAService().getSettings() else { return }
        currentCurrency.text = settings.pricing["currency"]
        currentExchange.text = settings.pricing["exchange"]
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        if textField.text == nil || (textField.text?.isEmpty)! {
            searchCurrencyList = currencyList
            self.tableView.reloadData()
            return
        }
        let filteredStrings = currencyList.filter({(item: CurrencyItem) -> Bool in                let stringMatch = item.currency.lowercased().range(of: textField.text!.lowercased())
                return stringMatch != nil ? true : false
        })
        searchCurrencyList = filteredStrings
        self.tableView.reloadData()
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return searchCurrencyList.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "CurrencyCell", for: indexPath) as! CurrencyCell
        let currency = searchCurrencyList[indexPath.row]
        cell.source.text = currency.exchange
        cell.fiat.text = currency.currency
        cell.selectionStyle = .none
        cell.separatorInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 16)
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let currency = searchCurrencyList[indexPath.row]
        setExchangeRate(currency)
    }

    func setExchangeRate(_ currency: CurrencyItem) {
        guard let settings = getGAService().getSettings() else { return }
        let bgq = DispatchQueue.global(qos: .background)

        var pricing = [String: String]()
        pricing["currency"] = currency.currency
        pricing["exchange"] = currency.exchange

        Guarantee().compactMap {
            settings.pricing = pricing
        }.compactMap(on: bgq) {
            try getGAService().getSession().changeSettings(details: try JSONSerialization.jsonObject(with: JSONEncoder().encode(settings), options: .allowFragments) as! [String: Any])
        }.then(on: bgq) { call in
            call.resolve(self)
        }.done { _ in
            self.navigationController?.popViewController(animated: true)
        }.catch {_ in

        }
    }

    func getExchangeRate() {
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap(on: bgq) {
            try getSession().getAvailableCurrencies()
        }.done { (data: [String: Any]?) in
            guard let json = data else { return }
            let perExchange = json["per_exchange"] as! [String: Any]
            self.currencyList.removeAll()
            for (exchange, array) in perExchange {
                for currency in array as! NSArray {
                    self.currencyList.append(CurrencyItem(exchange: exchange, currency: currency as! String))
                }
            }
            self.searchCurrencyList = self.currencyList
            self.tableView.reloadData()
        }.catch {_ in

        }
    }
}

class CurrencyItem: Codable {
    var exchange: String
    var currency: String

    init(exchange: String, currency: String) {
        self.currency = currency
        self.exchange = exchange
    }
}
