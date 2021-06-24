import XCTest
@testable import gaios

class AddWalletUITests: XCTestCase {
    
    override func setUp() {
        super.setUp()

        continueAfterFailure = false
        XCUIApplication().launch()
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func testAddWallet24() {
        
        Home()
            .tapAddWalletView()
        
        Landing()
            .tapAcceptTerms()
            .pause(2)
            .tapNewWallet()
        
        ChooseNetwork()
            .tapTestnetCard()
            
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
            .chooseWord()
            .pause(1)
        
        RecoverySuccess()
            .pause(1)
            .tapNext()

        WalletName()
            .pause(1)
            .typeName()
            .pause(1)
            .closeKey() 
            .pause(1)
            .tapNext()
            .pause(1)
        
        SetPin()
            .pause(2)
            .setPin()
            .pause(2)
            .setPin()
            .tapNext()
        
        WalletSuccess()
            .pause(2)
            .tapNext()
            
        Transactions()
            .pause(3)
    }
    
}
