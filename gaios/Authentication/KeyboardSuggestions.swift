import Foundation
import UIKit

protocol SuggestionsDelegate : class {
    func suggestionWasTapped(suggestion: String)
}

class KeyboardSuggestions : UIView {

    @IBOutlet weak var suggestion0: UILabel!
    @IBOutlet weak var suggestion1: UILabel!
    @IBOutlet weak var suggestion2: UILabel!
    @IBOutlet weak var leftButton: UIButton!
    @IBOutlet weak var rightButton: UIButton!

    weak var suggestionDelegate: SuggestionsDelegate?

    var suggestions = [String]()
    var offset: Int = 0

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initialize()
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        initialize()
    }

    private func initialize() {
        let view = Bundle.main.loadNibNamed("KeyboardSuggestions", owner: self, options: nil)!.first as! UIView
        view.frame = self.bounds

        [suggestion0, suggestion1, suggestion2].forEach {
            let tap = UITapGestureRecognizer(target: self, action: #selector(suggestionTapped))
            $0!.addGestureRecognizer(tap)
        }

        updateSuggestions()

        addSubview(view)
    }

    @objc func suggestionTapped(sender: UITapGestureRecognizer) {
        let label = sender.view as! UILabel
        suggestionDelegate?.suggestionWasTapped(suggestion: label.text!)
    }

    func setSuggestions(suggestions: [String]) {
        self.suggestions = suggestions
        offset = 0
        updateSuggestions()
    }

    func updateSuggestions() {
        leftButton.setTitleColor(offset > 0 ? UIColor.black : UIColor.darkGray, for: .normal)
        rightButton.setTitleColor(suggestions.count > 3 && (offset + 2) < suggestions.count - 1 ? UIColor.black : UIColor.darkGray, for: .normal)

        suggestion0.text = suggestions.count > (offset + 0) ? suggestions[offset + 0] : ""
        suggestion1.text = suggestions.count > (offset + 1) ? suggestions[offset + 1] : ""
        suggestion2.text = suggestions.count > (offset + 2) ? suggestions[offset + 2] : ""
    }

    @IBAction func leftButton(_ sender: Any) {
        offset = offset - 1
        if offset < 0 {
            offset = 0
        }
        updateSuggestions()
    }

    @IBAction func rightButton(_ sender: Any) {
        offset = offset + 1
        if offset >= suggestions.count {
            offset = suggestions.count - 1
        }
        updateSuggestions()
    }
}
