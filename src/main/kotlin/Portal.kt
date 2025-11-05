package cc.getportal.demo

import cc.getportal.PortalSDK
import cc.getportal.command.notification.KeyHandshakeUrlNotification
import cc.getportal.command.request.KeyHandshakeUrlRequest
import java.util.function.Consumer

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