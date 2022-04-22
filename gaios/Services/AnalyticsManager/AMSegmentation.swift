extension AnalyticsManager {

    typealias Segmentation = [String: String]

    func networkSegmentation() -> [String: String] {
        let s = Segmentation()
        //add params
        return s
    }

    func onBoardingSegmentation() -> [String: String] {
        let s = networkSegmentation() // ? is extending ?
        //add params
        return s
    }

    func sessionSegmentation() -> [String: String] {
        let s = networkSegmentation()
        //add params
        return s
    }

    func subAccountSegmentation() -> [String: String] {
        let s = sessionSegmentation()
        //add params
        return s
    }

    func twoFactorSegmentation() -> [String: String] {
        let s = sessionSegmentation()
        //add params
        return s
    }
}
