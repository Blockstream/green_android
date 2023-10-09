import Foundation
import SupportSDK
import ZendeskCoreSDK
import gdk

class ZendeskSdk {
    //private let zendeskSdk = ZendeskSdk().INSTANCE
    //private let support = Support.INSTANCE
    static let shared = ZendeskSdk()
    let URL = "https://blockstream.zendesk.com"
    let APPLICATION_ID = "12519480a4c4efbe883adc90777bb0f680186deece244799"
    var isAvailable: Bool { ZENDESK_CLIENT_ID != nil }
    var ZENDESK_CLIENT_ID: String? {
        Bundle.main.infoDictionary?["ZENDESK_CLIENT_ID"] as? String
    }

    init() {
        Zendesk.initialize(appId: APPLICATION_ID, clientId: ZENDESK_CLIENT_ID ?? "", zendeskUrl: URL)
        Support.initialize(withZendesk: Zendesk.instance)
    }

    func createNewTicketUrl(
        subject: String?,
        email: String?,
        message: String?,
        error: String?,
        network: NetworkSecurityCase?,
        hw: String?
    ) async -> URL? {
        
        let supportId = await SupportManager.shared.str()
        
        var components = URLComponents(string: "https://help.blockstream.com/hc/en-us/requests/new")!
        components.queryItems = [
            URLQueryItem(name: "tf_900008231623", value: "ios"),
            URLQueryItem(name: "tf_900009625166", value: Bundle.main.versionNumber),
            URLQueryItem(name: "tf_900003758323", value: "green"),
            URLQueryItem(name: "tf_21409433258649", value: error ?? ""),
            URLQueryItem(name: "tf_23833728377881", value: supportId)
        ]
        if let hw = hw {
            components.queryItems! += [URLQueryItem(name: "tf_900006375926", value: hw)]
        }
        if let network = network {
            let field = {
                if network.lightning { return "lightning__green_" }
                else if network.singlesig { return "singlesig__green_" }
                else { return "multisig_shield__green_" }
            }()
            components.queryItems! += [URLQueryItem(name: "tf_6167739898649", value: field)]
        }
        return components.url
    }

    func submitNewTicket(
        subject: String?,
        email: String?,
        message: String?,
        error: String?,
        network: NetworkSecurityCase?,
        hw: String?
    ) async {
        
        let supportId = await SupportManager.shared.str()
        
        let request = ZDKCreateRequest()
        request.tags = ["ios", "green"]
        request.subject = subject
        request.requestDescription = message ?? "{No Message}"
        var customFields = [
            CustomField(fieldId: 900003758323, value: "green"),
            CustomField(fieldId: 900009625166, value: Bundle.main.versionNumber),
            CustomField(fieldId: 900008231623, value: "ios"),
            CustomField(fieldId: 21409433258649, value: error ?? ""),
            CustomField(fieldId: 23833728377881, value: supportId)
        ]
        if let hw = hw {
            customFields += [CustomField(fieldId: 900006375926, value: hw)]
        }
        if let network = network {
            let field = {
                if network.lightning { return "lightning__green_" }
                else if network.singlesig { return "singlesig__green_" }
                else { return "multisig_shield__green_" }
            }()
            customFields += [CustomField(fieldId: 6167739898649, value: field)]
        }
        request.customFields = customFields

        var identity = Identity.createAnonymous()
        if let email = email {
            identity = Identity.createAnonymous(email: email)
        }
        Zendesk.instance?.setIdentity(identity)
        
        let provider = ZDKRequestProvider()
        provider.createRequest(request) { result, error in
            if error != nil {
                NSLog("ZendeskSdk: request error")
            } else {
                NSLog("ZendeskSdk: request success")
            }
        }
    }

}
