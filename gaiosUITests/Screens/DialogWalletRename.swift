import XCTest

class DialogWalletRename: Screen {

    override var rootElement: XCUIElement {
        return app.otherElements[AccessibilityIdentifiers.DialogWalletRenameScreen.view]
    }

    @discardableResult
    func tapSave() -> Self {
        tap(button: AccessibilityIdentifiers.DialogWalletRenameScreen.saveBtn)
        return self
    }

    @discardableResult
    func typeNewName(name: String) -> Self {
        let nameField = app.textFields[AccessibilityIdentifiers.DialogWalletRenameScreen.nameField]
        type(value: name, in: nameField)
        return self
    }

}

