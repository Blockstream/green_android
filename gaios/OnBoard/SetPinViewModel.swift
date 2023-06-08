import Foundation

import gdk

class SetPinViewModel {
    
    var credentials: Credentials
    var testnet: Bool
    var restoredAccount: Account?
    
    init(credentials: Credentials, testnet: Bool, restoredAccount: Account? = nil) {
        self.credentials = credentials
        self.testnet = testnet
        self.restoredAccount = restoredAccount
    }
    
    func getXpubHashId(session: SessionManager) async throws -> String? {
        try await session.connect()
        let walletId = session.walletIdentifier(credentials: self.credentials)
        return walletId?.xpubHashId
    }
    
    func restore(pin: String) async throws {
        let name = AccountsRepository.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = restoredAccount ?? Account(name: name, network: mainNetwork)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        try await wm.prominentSession?.connect()
        let xpub = try await self.getXpubHashId(session: wm.prominentSession!)
        // Avoid to restore an different wallet if restoredAccount is defined
        if let restoredAccount = self.restoredAccount,
           let xpubHashId = restoredAccount.xpubHashId, xpubHashId != xpub {
            throw LoginError.walletMismatch()
        }
        // Avoid to restore an existing wallets
        if let prevAccount = AccountsRepository.shared.find(xpubHashId: xpub ?? ""), self.restoredAccount == nil &&
            prevAccount.gdkNetwork.mainnet == account.gdkNetwork.mainnet && !prevAccount.isHW && !prevAccount.isWatchonly {
            throw LoginError.walletsJustRestored()
        }
        try await wm.login(credentials: self.credentials)
        if let network = wm.activeNetworks.first(where: { $0.multisig }) {
            wm.account.networkType = network
            wm.prominentNetwork = network
        }
        wm.account.attempts = 0
        AnalyticsManager.shared.restoreWallet(account: account)
        try await wm.account.addPin(session: wm.prominentSession!, pin: pin, mnemonic: self.credentials.mnemonic!)
    }

    func create(pin: String) async throws {
        let name = AccountsRepository.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = Account(name: name, network: mainNetwork)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        try await wm.create(credentials)
        try await wm.account.addPin(session: wm.prominentSession!, pin: pin, mnemonic: self.credentials.mnemonic!)
    }

    func setup(pin: String) async throws {
        guard let wm = WalletManager.current,
            let session = wm.prominentSession,
              let mnemonic = credentials.mnemonic
        else { throw LoginError.failed() }
        try await session.connect()
        try await wm.account.addPin(session: session, pin: pin, mnemonic: mnemonic)
        wm.account.attempts = 0
    }

}
