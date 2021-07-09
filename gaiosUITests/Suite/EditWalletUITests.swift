import XCTest
@testable import gaios

class EditWalletUITests: XCTestBase {
    func testRenameWallet() {
        let walletName = Constants.walletName
        let walletNameRenamed = Constants.walletNameRenamed
        if !Home().existsWallet(named: walletName) {
            restoreWallet()

            Transactions()
                .pause(1)
                .tapSettings()

            Settings()
                .pause(1)
                .tapLogOut()
        }

        Home()
            .swipeUp()
            .selectWallet(named: walletName)

        Login()
            .pause(1)
            .tapMenu()
            .pause(1)

        PopoverMenuWallet()
            .pause(1)
            .tapRenameWallet()
            .pause(1)

        DialogWalletRename()
            .pause(1)
            .typeNewName(name: walletNameRenamed)
            .pause(1)
            .tapSave()

        Login()
            .back()
            .pause(1)

        XCTAssert(Home().existsWallet(named: walletNameRenamed))

        Home()
            .swipeUp()
            .selectWallet(named: walletNameRenamed)

        Login()
            .pause(1)
            .tapMenu()
            .pause(1)

        PopoverMenuWallet()
            .pause(1)
            .tapRenameWallet()
            .pause(1)

        DialogWalletRename()
            .pause(1)
            .typeNewName(name: walletName)
            .pause(1)
            .tapSave()

        Login()
            .back()
            .pause(1)

        XCTAssert(Home().existsWallet(named: walletName))
    }

}
