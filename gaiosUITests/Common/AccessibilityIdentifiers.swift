import Foundation

struct AccessibilityIdentifiers {

    typealias Screen = String
    public typealias TextField = String
    typealias SecureTextField = TextField
    typealias Button = String

    struct HomeScreen {
        static let view = "view_home"
        static let addWalletView = "add_wallet_view"
    }

    struct LandingScreen {
        static let view = "view_loading"
        static let acceptTermsBtn = "accept_terms_btn"
        static let newWalletBtn = "new_wallet_btn"
        static let restoreWalletBtn = "restore_wallet_btn"
        static let watchOnlyWalletBtn = "watch_only_wallet_btn"
    }

    struct ChooseNetworkScreen {
        static let view = "view_choose_network"
        static let testnetCard = "testnet_card"
    }
    
    struct ChooseSecurityScreen {
        static let view = "view_choose_security"
        static let multiSigCard = "multi_sig_card"
    }
    
    struct RecoveryInstructionsScreen {
        static let view = "view_recovery_instructions"
        static let continueBtn = "continue_btn"
    }

    struct RecoveryCreateScreen {
        static let view = "view_recovery_create"
        static let word1Lbl = "word1_lbl"
        static let word2Lbl = "word2_lbl"
        static let word3Lbl = "word3_lbl"
        static let word4Lbl = "word4_lbl"
        static let word5Lbl = "word5_lbl"
        static let word6Lbl = "word6_lbl"
        static let nextBtn = "next_btn"
    }
    
    struct RecoveryVerifyScreen {
        static let view = "view_recovery_verify"
        static let word0btn = "word0_btn"
        static let word1btn = "word1_btn"
        static let word2btn = "word2_btn"
        static let word3btn = "word3_btn"
        static let quizLbl = "quiz_lbl"
    }
    
    struct RecoverySuccessScreen {
        static let view = "view_recovery_success"
        static let nextBtn = "next_btn"
    }
    
    struct WalletNameScreen {
        static let view = "view_wallet_name"
        static let nameField = "name_field"
        static let nextBtn = "next_btn"
    }
    
    struct SetPinScreen {
        static let view = "view_set_pin"
        static let btn1 = "btn_1_set_pin"
        static let btn2 = "btn_2_set_pin"
        static let nextBtn = "next_btn"
    }
    
    struct WalletSuccessScreen {
        static let view = "view_wallet_success"
        static let nextBtn = "next_btn"
    }
    
    struct ContainerScreen {
        static let view = "view_container"
    }
    
    struct TransactionsScreen {
        static let view = "view_transactions"
        static let settingsBtn = "settings_btn"
        static let sendView = "send_view"
        static let receiveView = "receive_view"
    }
    
    struct LoginScreen {
        static let view = "view_login"
        static let menuBtn = "menu_btn"
        static let btn1 = "btn_1_login"
        static let btn2 = "btn_2_login"
        static let nextBtn = "next_btn"
        static let backBtn = "back_btn"
    }
    
    struct PopoverMenuWalletScreen {
        static let view = "view_popover_menu"
        static let deleteBtn = "delete_btn"
    }
    
    struct DialogWalletRenameScreen {
        static let view = "view_dialog_wallet_rename"
        static let nameField = "name_field"
        static let saveBtn = "save_button"
    }

    struct DialogWalletDeleteScreen {
        static let view = "view_dialog_wallet_delete"
        static let deleteBtn = "delete_btn"
    }
    
    struct RestoreWalletScreen {
        static let view = "view_restore_wallet"
        static let restoreCard = "restore_card"
    }
    
    struct RecoveryPhraseScreen {
        static let view = "view_recovery_phrase"
        static let phraseCard = "phrase_card"
    }
    
    struct MnemonicScreen {
        static let view = "view_mnemonic"
        static let titleLbl = "title_lbl"
        static let doneBtn = "done_btn"
    }
    
    struct SettingsScreen {
        static let view = "view_settings"
        static let usernameField = "username_field"
        static let passwordField = "password_field"
        static let saveBtn = "save_btn"
    }
    
    struct WatchOnlyScreen {
        static let view = "view_watch_only"
        static let usernameField = "username_field"
        static let passwordField = "password_field"
        static let testnetSwitch = "testnet_switch"
        static let loginBtn = "login_btn"
    }
    
    struct ReceiveBtcScreen {
        static let view = "view_receive_btc"
        static let qrCodeView = "qr_code_view"

    }
    
    struct SendBtcScreen {
        static let view = "view_send_btc"
        static let textView = "text_view"
        static let nextBtn = "next_btn"
    }
    
    struct SendBtcDetailsScreen {
        static let view = "view_send_btc_details"
        static let amountTextField = "amount_text_field"
        static let recipientTitle = "recipient_title"
        static let reviewBtn = "review_btn"
    }
    
    struct SendBtcConfirmationScreen {
        static let view = "view_send_btc_confirmation"
        static let slidingBtn = "sliding_btn"
    }
    
    struct ScreenLockScreen {
        static let view = "view_screen_lock"
        static let pinLbl = "pin_lbl"
    }
}
