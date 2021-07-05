import XCTest

extension XCUIElementQuery {
    var lastMatch: XCUIElement { return self.element(boundBy: self.count - 1) }
}
