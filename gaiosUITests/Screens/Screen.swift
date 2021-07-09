import XCTest

class Screen {
    static let defautlTimeOut: TimeInterval = 20
    let maxSwipeUp = 1
    let app = XCUIApplication()

    typealias Completion = (() -> Void)?
    
    @discardableResult
    required init(timeout: TimeInterval = Screen.defautlTimeOut, completion: Completion = nil) {
        XCTAssert(rootElement.waitForExistence(timeout: timeout), "Screen \(String(describing: self)) waited, but not loaded")

        completion?()
    }

    var rootElement: XCUIElement {
        fatalError("subclass should override rootElement")
    }

    @discardableResult
    func tap(_ element: XCUIElement) -> Self {
        element.tap()
        return self
    }

    @discardableResult
    func tap(button identifier: AccessibilityIdentifiers.Button) -> Self {
        let element = app.buttons[identifier]
        return tap(element)
    }

    @discardableResult
    func pause(_ time: UInt32) -> Self {
        sleep(time)
        return self
    }
    
    @discardableResult
    func type(value: String, in element: XCUIElement) -> Self {
        element.tap()
        element.typeText(value)
        return self
    }
    
    func swipeUp() -> Self {
        rootElement.tables.cells.element(boundBy: 0).swipeUp()
        return self
    }

    @discardableResult
    func existsWallet(named name: String, connectionTimeout: TimeInterval = 25) -> Bool {
        let walletExistance = NSPredicate(format: "label CONTAINS '\(name)'")
        let walletLabel = app.otherElements[AccessibilityIdentifiers.HomeScreen.view].tables
            .children(matching: .cell).staticTexts.element(matching: walletExistance)
        var existsWallet = walletLabel.waitForExistence(timeout: 3)

        var swipeUpTimes = 0
        while !existsWallet && swipeUpTimes < maxSwipeUp {
            rootElement.tables.cells.element(boundBy: 0).swipeUp()
            swipeUpTimes += 1

            existsWallet = walletLabel.exists
        }

        return existsWallet
    }
    
    
    @discardableResult
    func selectWallet(named name: String, connectionTimeout: TimeInterval = 25) -> Self {
        let walletExistance = NSPredicate(format: "label CONTAINS '\(name)'")
        let walletLabel = app.otherElements[AccessibilityIdentifiers.HomeScreen.view].tables
            .children(matching: .cell).staticTexts.element(matching: walletExistance)
        var existsWallet = walletLabel.waitForExistence(timeout: 3)

        var swipeUpTimes = 0
        while !existsWallet && swipeUpTimes < maxSwipeUp {
            rootElement.tables.cells.element(boundBy: 0).swipeUp()
            swipeUpTimes += 1

            existsWallet = walletLabel.exists
        }

        return tap(walletLabel)
    }

//    @discardableResult
//    func matchStaticText(identifier: String) -> Self {
//        XCTAssert(exists(staticText: identifier, timeout: 10))
//        return self
//    }
//
//    @discardableResult
//    func exists(staticText: String, timeout: TimeInterval = 10) -> Bool {
//        return app.staticTexts[staticText].waitForExistence(timeout: timeout)
//    }
//
//    @discardableResult
//    func waitForElement(_ element: XCUIElement, timeout: TimeInterval = 10) -> Self {
//        XCTAssertTrue(element.waitForExistence(timeout: 10))
//        return self
//    }
}
