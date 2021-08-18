import Foundation
import PromiseKit

class CsvViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var nextButton: UIButton!

    var csvTypes = Settings.CsvTime.all()
    var csvValues = Settings.CsvTime.values()
    var newCsv: Int?
    var currentCsv: Int?

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        nextButton.backgroundColor = UIColor.customMatrixGreen()
        nextButton.isUserInteractionEnabled = true
        nextButton.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        tableView.reloadData()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.estimatedRowHeight = 80
        tableView.rowHeight = UITableView.automaticDimension
        tableView.dataSource = self
        tableView.delegate = self
        currentCsv = Settings.shared?.csvtime
    }

    func setCsvTimeLock(csv: Settings.CsvTime) {
        var details = [String: Any]()
        details["value"] = csv.value()
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try SessionManager.shared.setCSVTime(details: details)
        }.then(on: bgq) { call in
            call.resolve()
        }.map(on: bgq) { _ in
            if let data = try? SessionManager.shared.getSettings() {
                Settings.shared = Settings.from(data)
            }
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            // update values when change successful
            self.newCsv = nil
            self.currentCsv = csv.value()
            DropAlert().success(message: String(format: "%@: %@", NSLocalizedString("id_twofactor_authentication_expiry", comment: ""), csv.label()))
            self.navigationController?.popViewController(animated: true)
        }.catch { _ in
            DropAlert().error(message: "Error changing csv time")
        }
    }

    @IBAction func nextTapped(_ sender: Any) {
        if let csv = newCsv, let index = csvValues? .firstIndex(of: csv) {
            if csv != currentCsv {
                setCsvTimeLock(csv: csvTypes[index])
            } else {
                self.showAlert(title: NSLocalizedString("Error", comment: ""), message: "Select a new value to change csv")
            }
        }
    }
}

extension CsvViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return csvTypes.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "CsvTimeCell") as? CsvTimeCell {
            let selected = csvTypes[indexPath.row]
            cell.configure(csvType: selected, delegate: self)
            cell.accessoryType = currentCsv == selected.value() ? .checkmark : .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let selected = csvTypes[indexPath.row]
        let rows = [0, 1, 2]
        let notSelected = rows.filter { $0 != indexPath.row }
        tableView.deselectRow(at: IndexPath(row: notSelected[0], section: 0), animated: true)
        tableView.deselectRow(at: IndexPath(row: notSelected[1], section: 0), animated: true)
        tableView.selectRow(at: IndexPath(row: indexPath.row, section: 0), animated: true, scrollPosition: .none)
        newCsv = selected.value()
    }
}

extension CsvViewController: CsvTypeInfoDelegate {
func didTapInfo(csv: Settings.CsvTime) {
    self.showAlert(title: NSLocalizedString("Info", comment: ""), message: csv.description())
    }
}
