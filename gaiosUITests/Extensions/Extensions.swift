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

extension XCUIElement {
    func hasFocus() -> Bool {
        let hasKeyboardFocus = (self.value(forKey: "hasKeyboardFocus") as? Bool) ?? false
        return hasKeyboardFocus
    }
}

extension NSObject {
    func dumpProperties() {
        var outCount: UInt32 = 0

        let properties = class_copyPropertyList(type(of: self), &outCount)
        for index in 0...outCount {
            guard let property = properties?[Int(index)] else {
                continue
            }
            let propertyName = String(cString: property_getName(property))
            print("property name: \(propertyName)")
            guard let propertyAttributes = property_getAttributes(property) else {
                continue
            }
            let propertyType = String(cString: propertyAttributes)
            print("property type: \(propertyType)")
        }
    }
}
