import UIKit

extension UITapGestureRecognizer {

   func didTapAttributedTextInLabel(label: UILabel, inRange targetRange: NSRange) -> Bool {
       guard let attributedText = label.attributedText else { return false }

       let mutableStr = NSMutableAttributedString.init(attributedString: attributedText)
       mutableStr.addAttributes([NSAttributedString.Key.font: label.font!], range: NSRange.init(location: 0, length: attributedText.length))
       let paragraphStyle = NSMutableParagraphStyle()
       paragraphStyle.alignment = .center
       mutableStr.addAttributes([NSAttributedString.Key.paragraphStyle: paragraphStyle], range: NSRange(location: 0, length: attributedText.length))

       let layoutManager = NSLayoutManager()
       let textContainer = NSTextContainer(size: CGSize.zero)
       let textStorage = NSTextStorage(attributedString: mutableStr)

       layoutManager.addTextContainer(textContainer)
       textStorage.addLayoutManager(layoutManager)

       textContainer.lineFragmentPadding = 0.0
       textContainer.lineBreakMode = label.lineBreakMode
       textContainer.maximumNumberOfLines = label.numberOfLines
       let labelSize = label.bounds.size
       textContainer.size = labelSize

       let locationOfTouchInLabel = self.location(in: label)
       let textBoundingBox = layoutManager.usedRect(for: textContainer)
       let textContainerOffset = CGPoint(x: (labelSize.width - textBoundingBox.size.width) * 0.5 - textBoundingBox.origin.x, y: (labelSize.height - textBoundingBox.size.height) * 0.5 - textBoundingBox.origin.y)
       let locationOfTouchInTextContainer = CGPoint(x: locationOfTouchInLabel.x - textContainerOffset.x, y: locationOfTouchInLabel.y - textContainerOffset.y)
       let indexOfCharacter = layoutManager.characterIndex(for: locationOfTouchInTextContainer, in: textContainer, fractionOfDistanceBetweenInsertionPoints: nil)
       return NSLocationInRange(indexOfCharacter, targetRange)
   }
}
