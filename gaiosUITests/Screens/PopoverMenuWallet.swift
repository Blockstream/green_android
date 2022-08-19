import XCTest

class PopoverMenuWallet: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.PopoverMenuWalletScreen.view]
    }
    
    @discardableResult
    func tapRenameWallet(connectionTimeout: TimeInterval = 25) -> Self {

        rootElement.tables.cells.element(boundBy: 1).tap()
        return self
    }

    @discardableResult
    func tapRemoveWallet(connectionTimeout: TimeInterval = 25) -> Self {

        rootElement.tables.cells.element(boundBy: 2).tap()
        return self
    }
    
//    @discardableResult
//    func tapMenu() -> Self {
//        tap(button: AccessibilityIdentifiers.LoginScreen.menuBtn)
//        return self
//    }
//    
//    @discardableResult
//    func tapDelete() -> Self {
//        tap(button: AccessibilityIdentifiers.LoginScreen.deleteBtn)
//        return self
//    }
    
}
