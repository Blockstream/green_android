import XCTest

extension XCUIElementQuery {
    var lastMatch: XCUIElement { return self.element(boundBy: self.count - 1) }
}

extension String {
   public func localized(for aClass: AnyClass) -> String {
      let bundle = Bundle(for: aClass)
      return NSLocalizedString(self, tableName: nil, bundle: bundle, comment: "")
   }
}

extension XCTestCase {
   var reference: AnyClass {
      return type(of: self)
   }
} 
