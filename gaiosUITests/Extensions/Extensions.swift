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

extension XCUIApplication {
    func uninstall(name: String? = nil) {
        self.terminate()

        let timeout = TimeInterval(5)
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

        let appName: String
        if let name = name {
            appName = name
        } else {
            let uiTestRunnerName = Bundle.main.infoDictionary?["CFBundleName"] as! String
            appName = uiTestRunnerName.replacingOccurrences(of: "UITests-Runner", with: "")
        }

        /// use `firstMatch` because icon may appear in iPad dock
        let appIcon = springboard.icons[appName].firstMatch
        if appIcon.waitForExistence(timeout: timeout) {
            appIcon.press(forDuration: 2)
        } else {
            XCTFail("Failed to find app icon named \(appName)")
        }

        let removeAppButton = springboard.buttons["Remove App"]
        if removeAppButton.waitForExistence(timeout: timeout) {
            removeAppButton.tap()
        } else {
            XCTFail("Failed to find 'Remove App'")
        }

        let deleteAppButton = springboard.alerts.buttons["Delete App"]
        if deleteAppButton.waitForExistence(timeout: timeout) {
            deleteAppButton.tap()
        } else {
            XCTFail("Failed to find 'Delete App'")
        }

        let finalDeleteButton = springboard.alerts.buttons["Delete"]
        if finalDeleteButton.waitForExistence(timeout: timeout) {
            finalDeleteButton.tap()
        } else {
            XCTFail("Failed to find 'Delete'")
        }
    }
}
