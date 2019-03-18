import UIKit
import PromiseKit

protocol PopupPromise {
    func show() -> Promise<String>
}

class PopupList : PopupPromise {
    let viewController: UIViewController
    let title: String
    let list: [Any]
    let selected: Any?

    init(_ view: UIViewController, title: String, list: [Any], selected: Any?) {
        self.viewController = view
        self.title = title
        self.list = list
        self.selected = selected
    }

    func show() -> Promise<String> {
        return Promise { result in
            let alert = UIAlertController(title: title, message: "", preferredStyle: .actionSheet)
            list.forEach { (item: Any) in
                let strItem = String(describing: item)
                let strSelected = selected != nil ? String(describing: selected!) : ""
                alert.addAction(UIAlertAction(title: strItem, style: strItem == strSelected ? .destructive : .default) { (action: UIAlertAction) in
                    result.fulfill(strItem)
                })
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (action: UIAlertAction) in
                result.reject(GaError.GenericError)
            })
            DispatchQueue.main.async {
                self.viewController.present(alert, animated: true, completion: nil)
            }
        }
    }
}

class PopupEditable : PopupPromise {
    let viewController: UIViewController
    let title: String
    let hint: String?
    let text: String?
    let keyboardType: UIKeyboardType?

    init(_ view: UIViewController, title: String, hint: String?, text: String?, keyboardType: UIKeyboardType?) {
        self.viewController = view
        self.title = title
        self.hint = hint
        self.text = text
        self.keyboardType = keyboardType
    }

    func show() -> Promise<String> {
        return Promise { result in
            let alert = UIAlertController(title: title, message: "", preferredStyle: .alert)
            alert.addTextField { (textField) in
                textField.placeholder = self.hint ?? ""
                textField.text = self.text ?? ""
                textField.keyboardType = self.keyboardType ?? .asciiCapable
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (action: UIAlertAction) in
                result.reject(GaError.GenericError)
            })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_save", comment: ""), style: .default) { (action: UIAlertAction) in
                let textField = alert.textFields![0]
                result.fulfill(textField.text!)
            })
            DispatchQueue.main.async {
                self.viewController.present(alert, animated: true, completion: nil)
            }
        }
    }
}

