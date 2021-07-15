import XCTest
@testable import gaios

class EditWalletUITests: XCTestBase {
    func testRenameWallet() {
        let walletName = Constants.walletName
        let words = Constants.mnemonic
        let walletNameRenamed = Constants.walletNameRenamed
        
        if !Home().existsWallet(named: walletName) {
            restoreWallet(walletName: walletName, words: words, isSingleSig: false)

            Transactions()
                .pause(1)
                .tapSettings()

            Settings()
                .pause(1)
                .tapLogOut()
        }

        Home()
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
