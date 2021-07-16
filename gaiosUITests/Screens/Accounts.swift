import XCTest

class Accounts: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.AccountsScreen.view]
    }
    
    @discardableResult
    func checkAccountMain() -> Self {
        
        let exists = app.staticTexts["id_main_account".localized(for: Self.self)].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("id_main_account not found")
        }
        return self
    }
 
    @discardableResult
    func checkAccountSegwit() -> Self {
        
        let exists = app.staticTexts["Segwit Account"].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("SegWit Account not found")
        }
        return self
    }
    
    @discardableResult
    func tapFooter() -> Self {
        app.staticTexts[AccessibilityIdentifiers.AccountsScreen.footerMessage].tap()
        return self
    }
    
    @discardableResult
    func checkSubAccountName(_ name: String) -> Self {
        
        let exists = app.staticTexts[name].waitForExistence(timeout: 10)
                    
        if exists != true {
            XCTFail("sub account not found")
        }
        return self
    }
}
