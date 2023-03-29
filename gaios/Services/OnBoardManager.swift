enum OnBoardingFlowType {
    case add
    case restore
    case watchonly
}

enum OnBoardingChainType {
    case mainnet
    case testnet
}

class OnBoardManager {

    static let shared = OnBoardManager()

    var flowType: OnBoardingFlowType = .add
    var chainType: OnBoardingChainType = .mainnet
}
