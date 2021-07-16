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
            .cleanWords()
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
        
        Accounts()
            .pause(1)
            .checkAccountMain()
            .pause(1)
            .checkAccountSegwit()
    }
    
    func testMultisigWalletCreateAccount() {
        
        Home()
            .tapAddWalletView()

        Landing()
            .tapAcceptTerms()
            .pause(1)
            .tapNewWallet()

        ChooseNetwork()
            .tapTestnetCard()

        ChooseSecurity()
            .tapMultiSigCard()

        RecoveryInstructions()
            .tapContinue()

        RecoveryCreate()
            .cleanWords()
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
        
        Accounts()
            .pause(1)
            .tapFooter()
       
        AccountCreateSelectType()
            .pause(1)
            .tapStandardCard()
            .pause(1)
        
        AccountCreateSetName()
            .pause(1)
            .typeName("standard account")
            .pause(1)
            .closeKey()
            .pause(1)
            .tapNext()
            .pause(1)
        
        Accounts()
            .pause(1)
            .checkSubAccountName("standard account")
    }
}
