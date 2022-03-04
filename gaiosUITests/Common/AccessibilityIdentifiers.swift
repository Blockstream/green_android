import Foundation

struct AccessibilityIdentifiers {

    typealias Screen = String
    public typealias TextField = String
    typealias SecureTextField = TextField
    typealias Button = String

    struct HomeScreen {
        static let view = "view_home"
        static let addWalletView = "add_wallet_view"
        static let appSettingsBtn = "appSettingsBtn"
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
        static let liquidTestnetCard = "liquid_testnet_card"
    }
    
    struct ChooseSecurityScreen {
        static let view = "view_choose_security"
        static let singleSigCard = "single_sig_card"
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
        static let settingsBtn = "settings_btn"
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

    struct OverviewScreen {
        static let view = "view_overview"
        static let settingsBtn = "settings_btn"
        static let sendView = "send_view"
        static let receiveView = "receive_view"
    }

    struct LoginScreen {
        static let view = "view_login"
        static let menuBtn = "menu_btn"
        static let btn1 = "btn_1_login"
        static let btn2 = "btn_2_login"
        static let btn3 = "btn_3_login"
        static let nextBtn = "next_btn"
        static let backBtn = "back_btn"
        static let attemptsLbl = "attempts_lbl"
        static let settingsBtn = "settings_btn"
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
        static let cardMultiSig = "multisig_card"
        static let cardSingleSig = "singlesig_card"
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
    
    struct ReceiveScreen {
        static let view = "view_receive"
        static let qrCodeBtn = "qr_code_btn"
        static let moreOptionsBtn = "more_options_btn"
    }
    
    struct SendScreen {
        static let view = "view_send"
        static let pasteAddressBtn = "paste_address_btn"
        static let amountField = "amount_field"
        static let nextBtn = "next_btn"
        static let setCutomFeeBtn = "set_custom_fee_btn"
        static let chooseAssetBtn = "choose_asset_btn"
        static let sendAllBtn = "send_all_btn"
    }

    struct SendConfirmScreen {
        static let view = "view_send_confirmation"
        static let nextBtn = "next_btn"
        static let amountLbl = "amount_lbl"
    }
    
    struct AccountsScreen {
        static let view = "view_accounts"
        static let footerMessage = "footer_message"
    }
    
    struct AccountCreateSelectTypeScreen {
        static let view = "view_account_create_select_type"
        static let cardLegacy = "card_legacy"
        static let cardSegWit = "card_segwit"
        static let cardStandard = "card_standard"
        static let cardAmp = "card_amp"
        static let card2of3 = "card_2of3"
    }
    
    struct AccountCreateSetNameScreen {
        static let view = "view_account_create_set_name"
        static let nameField = "name_field"
        static let nextBtn = "next_btn"
    }
    
    struct DrawerMenuScreen {
        static let view = "view_drawer_menu"
        static let addWalletView = "add_wallet_view_drawer"
    }
    
    struct HWWScanScreen {
        static let view = "view_hww_scan"
        static let titleLbl = "title_lbl"
    }
    
    struct WalletSettingsScreen {
        static let view = "view_wallet_settings"
        static let torSwitch = "view_tor_switch"
        static let saveBtn = "save_btn"
        static let cancelBtn = "cancel_btn"
        static let testnetSwitch = "view_testnet_switch"
    }
    
    struct DialogTorSingleSigScreen {
        static let view = "view_dialog_tor_singlesig"
        static let continueBtn = "continue_btn"
    }

    struct ExistingWalletsScreen {
        static let view = "view_existing_wallets"
        static let manualRestoreBtn = "manual_restore_btn"
    }

    struct ManualRestoreScreen {
        static let view = "view_manual_restore"
        static let singleSigCard = "single_sig_card"
        static let multiSigCard = "multi_sig_card"
    }

    struct DialogMnemonicLengthScreen {
        static let view = "view_dialog_mnemonic_lenght"
        static let length12Btn = "length_12_btn"
        static let length24Btn = "length_24_btn"
    }
    
    struct DialogReceiveMoreOptionsScreen {
        static let view = "view_dialog_receive_more_option"
        static let requestAmountBtn = "request_amount_btn"
    }

    struct DialogReceiveRequestAmountScreen {
        static let view = "view_dialog_receive_request_amount"
        static let amountField = "amount_field"
        static let confirmBtn = "confirm_btn"
    }

    struct DialogCustomFeeScreen {
        static let view = "view_dialog_custom_fee"
        static let feeField = "fee_field"
        static let saveBtn = "save_btn"
    }

    struct AssetsListScreen {
        static let view = "view_assets_list"
        static let table = "table_essets_list"
    }

    struct KeyboardView {
        static let done = "done_btn"
    }
}
