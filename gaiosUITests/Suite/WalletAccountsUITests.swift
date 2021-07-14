import XCTest
@testable import gaios

class WalletAccountUITests: XCTestBase {
    
    func testSingleSigSegwitWallet() {
        
        Home()
            .tapAddWalletView()

        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()

        ChooseNetwork()
            .tapTestnetCard()

        ChooseSecurity()
            .tapSingleSigCard()

        RecoveryInstructions()
            .tapContinue()

        RecoveryCreate()
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)
            .readWords()
            .pause(1)
            .tapNext()
            .pause(1)

        RecoveryVerify()
            .pause(1)
            .chooseWord()
            .pause(1)
            .chooseWord()
            .pause(1)
            .chooseWord()
            .pause(1)

        RecoverySuccess()
            .pause(1)
            .tapNext()

        WalletName()
            .pause(1)
            .typeName(nil)
            .pause(1)
            .closeKey()
            .pause(1)
            .tapNext()
            .pause(1)

        SetPin()
            .pause(1)
            .setPin()
            .pause(1)
            .setPin()
            .tapNext()

        WalletSuccess()
            .pause(1)
            .tapNext()
        
        Transactions()
            .pause(1)
            .tapStack()
            .pause(1)
        
        Accounts()
            .pause(1)
            .checkAccountMain()
            .pause(1)
            .checkAccountSegwit()
    }
}
