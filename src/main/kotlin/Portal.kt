package cc.getportal.demo

import cc.getportal.PortalSDK

object Portal {

    lateinit var sdk: PortalSDK

    fun connect(healthEndpoint: String, wsEndpoint : String, token: String) {
        sdk = PortalSDK(healthEndpoint, wsEndpoint)
        sdk.connect(token)
    }

    fun disconnect() {
        sdk.disconnect()
    }
}