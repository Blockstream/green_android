import Foundation

struct AccessibilityIdentifiers {

    typealias Screen = String
    public typealias TextField = String
    typealias SecureTextField = TextField
    typealias Button = String

    struct HomeScreen {
        static let view = "view"
        static let addWalletView = "add_wallet_view"
    }

    struct LandingScreen {
        static let view = "view"
        static let acceptTermsBtn = "accept_terms_btn"
        static let newWalletBtn = "new_wallet_btn"
    }

    struct ChooseNetworkScreen {
        static let view = "view"
        static let testnetCard = "testnet_card"
    }
    
    struct ChooseSecurityScreen {
        static let view = "view"
        static let multiSigCard = "multi_sig_card"
    }
    
    struct RecoveryInstructionsScreen {
        static let view = "view"
        static let continueBtn = "continue_btn"
    }

    struct RecoveryCreateScreen {
        static let view = "view"
        static let word1Lbl = "word1_lbl"
        static let word2Lbl = "word2_lbl"
        static let word3Lbl = "word3_lbl"
        static let word4Lbl = "word4_lbl"
        static let word5Lbl = "word5_lbl"
        static let word6Lbl = "word6_lbl"
        static let nextBtn = "next_btn"
    }
    
    struct RecoveryVerifyScreen {
        static let view = "view"
        static let word0btn = "word0_btn"
        static let word1btn = "word1_btn"
        static let word2btn = "word2_btn"
        static let word3btn = "word3_btn"
        static let quizLbl = "quiz_lbl"
    }
    
    struct RecoverySuccessScreen {
        static let view = "view"
        static let nextBtn = "next_btn"
    }
    
    struct WalletNameScreen {
        static let view = "view"
        static let nameField = "name_field"
        static let nextBtn = "next_btn"
    }
    
    struct SetPinScreen {
        static let view = "view"
        static let btn1 = "btn_1"
        static let nextBtn = "next_btn"
    }
    
    struct WalletSuccessScreen {
        static let view = "view"
        static let nextBtn = "next_btn"
    }
    
    struct ContainerScreen {
        static let view = "view"
    }
    
    struct TransactionsScreen {
        static let view = "view"
    }
}
