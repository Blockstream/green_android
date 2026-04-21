package com.pandulapeter.beagle.logOkHttp

// Reown's CoreNetworkModule always looks up this object, even when Green does not ship Beagle.
// Returning null disables that optional interceptor without pulling the crashing Beagle runtime in.
object BeagleOkHttpLogger {
    fun getLogger(): Any? = null
}
