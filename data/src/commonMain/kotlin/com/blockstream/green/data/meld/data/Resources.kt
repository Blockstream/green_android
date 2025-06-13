package com.blockstream.green.data.meld.data

import io.ktor.resources.Resource

object Resources {

    @Resource("/payments")
    class Payments() {

        @Resource("transactions")
        class Transactions(val parent: Payments = Payments(), val externalCustomerIds: String)

        @Resource("crypto")
        class Crypto(val parent: Payments = Payments()) {
            @Resource("quote")
            class Quote(val parent: Crypto = Crypto())

            @Resource("limits")
            class Limits(val parent: Crypto = Crypto(), val fiatCurrency: String)
        }
    }

    @Resource("/crypto")
    class Crypto() {
        @Resource("session")
        class Session(val parent: Crypto = Crypto()) {
            @Resource("widget")
            class Widget(val parent: Session = Session())
        }
    }

    @Resource("/service-providers")
    class ServiceProviders() {
        @Resource("properties")
        class Properties(val parent: ServiceProviders = ServiceProviders()) {
            @Resource("countries")
            class Countries(val parent: Properties = Properties(), val accountFilter: Boolean = true)
        }
    }
}