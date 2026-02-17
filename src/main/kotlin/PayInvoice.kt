package cc.getportal.command.request

import cc.getportal.command.PortalRequest
import cc.getportal.command.PortalResponse
import cc.getportal.command.notification.UnitNotification

class PayInvoiceRequest(
    private val invoice: String
) : PortalRequest<PayInvoiceResponse, UnitNotification>() {

    override fun name(): String = "PayInvoice"

    override fun responseType(): Class<PayInvoiceResponse> = PayInvoiceResponse::class.java
}

data class PayInvoiceResponse(
    val preimage: String
) : PortalResponse
