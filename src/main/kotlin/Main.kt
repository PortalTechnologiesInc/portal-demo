package cc.getportal.demo

import cc.getportal.PortalSDK
import cc.getportal.command.notification.RequestSinglePaymentNotification
import cc.getportal.command.request.AuthenticateKeyRequest
import cc.getportal.command.request.BurnCashuRequest
import cc.getportal.command.request.CalculateNextOccurrenceRequest
import cc.getportal.command.request.CloseRecurringPaymentRequest
import cc.getportal.command.request.FetchNip05ProfileRequest
import cc.getportal.command.request.FetchProfileRequest
import cc.getportal.command.request.KeyHandshakeUrlRequest
import cc.getportal.command.request.ListenClosedRecurringPaymentRequest
import cc.getportal.command.request.MintCashuRequest
import cc.getportal.command.request.RequestCashuRequest
import cc.getportal.command.request.RequestRecurringPaymentRequest
import cc.getportal.command.request.RequestSinglePaymentRequest
import cc.getportal.command.request.SendCashuDirectRequest
import cc.getportal.command.response.AuthenticateKeyResponse
import cc.getportal.command.response.RequestRecurringPaymentResponse
import cc.getportal.model.CashuResponseStatus
import cc.getportal.model.Currency
import cc.getportal.model.RecurrenceInfo
import cc.getportal.model.RecurringPaymentRequestContent
import cc.getportal.model.SinglePaymentRequestContent
import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.log
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Bootstrap")
var recurringPaymentThread: ScheduledFuture<*>? = null
var javalinApp: Javalin? = null
var portalSdk: PortalSDK? = null

fun main() {
    // val healthEndpoint = System.getenv("REST_HEALTH_ENDPOINT")
    // if(healthEndpoint == null) {
    //     logger.error("missing REST_HEALTH_ENDPOINT env variable")
    //     return
    // }
    logger.info("Starting portal demo backend...")

    val wsEndpoint = System.getenv("REST_WS_ENDPOINT")
    if(wsEndpoint == null) {
        logger.error("missing REST_WS_ENDPOINT env variable")
        return
    }

    val token = System.getenv("REST_TOKEN")
    if(token == null) {
        logger.error("missing REST_TOKEN env variable")
        return
    }

    // build frontend
    if(System.getenv("DEV_MODE") == "true") {
        buildFrontend()
    }

    val dbPath = System.getenv("DB_PATH")
    if(dbPath == null) {
        logger.error("missing DB_PATH env variable")
        return
    }


    logger.info("Connecting to database...")

    // connect DB
    DB.connect(dbPath, "data.db")

    logger.info("Connecting to Portal...")
    // connect to Portal
    val sdk = PortalSDK(wsEndpoint)
    portalSdk = sdk
    sdk.connect()
    sdk.authenticate(token)

    // start web app after 5 seconds
    logger.info("Starting webserver in a few seconds...")
    Thread.sleep(1000 * 5)
    startWebApp(sdk)


    try {
        listenClosedRecurringPayments(sdk)
    } catch (e : Exception) {
        logger.error("Error in listenClosedRecurringPayments", e)
    }

    try {
        startRecurringPaymentThread(sdk)
    } catch (e: Exception) {
        logger.error("Error in startRecurringPaymentThread", e)
    }

}

fun listenClosedRecurringPayments(sdk: PortalSDK) {
    sdk.sendCommand(ListenClosedRecurringPaymentRequest { notification ->

        DB.getSubscriptionByPortalId(notification.subscription_id)?.let { subscription ->
            DB.updateSubscriptionStatus(subscription.data.id, SubscriptionStatus.CANCELLED)

            logger.info("Subscription cancelled by user: {}", notification.subscription_id)

        }


    }) { _, err ->
        if (err != null) {
            logger.error("Error listening closed subscriptions: {}", err)
            exitProcess(1)
        }

        logger.info("Listening closed subscriptions...")

    }
}


fun startRecurringPaymentThread(sdk: PortalSDK) {
    val scheduler = Executors.newScheduledThreadPool(1)
    recurringPaymentThread = scheduler.scheduleAtFixedRate({
        val now = Instant.now()


        val subscriptions = DB.getDueSubscriptions(now)
        for (subscription in subscriptions) {

            val recentPayments = DB.getSubscriptionRecentPayments(subscription.user, subscription.data.portalSubscriptionId, limit = 3)
            if(recentPayments.size >= 3 && recentPayments.all { it.paid == null || !it.paid }) {

                logger.warn("Cancelling subscription {} due to 3 consecutive payment failures", subscription.data.portalSubscriptionId);
                DB.updateSubscriptionStatus(subscription.data.id, SubscriptionStatus.FAILED)

                sdk.sendCommand(CloseRecurringPaymentRequest(subscription.user, emptyList(), subscription.data.portalSubscriptionId), {
                        res, err ->

                    if(err != null) {
                        logger.warn("Error closing recurring payment: {}", err)
                        return@sendCommand
                    }

                })
                continue
            }

            // REQUESTING SUBSCRIPTION INVOICE

            val description = "Payment for subscription ${subscription.data.portalSubscriptionId}"

            var currency = Currency.MILLISATS
            if(subscription.data.currency != "Millisats") {
                currency = Currency.FIAT(subscription.data.currency)
            }
            val paymentId = DB.registerPayment(subscription.user, currency, subscription.data.amount, description, subscription.data.portalSubscriptionId)
//            ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))

            val req = SinglePaymentRequestContent(description, subscription.data.amount, currency, subscription.data.portalSubscriptionId, null)
            sdk.sendCommand(RequestSinglePaymentRequest(subscription.user, emptyList(), req) { not ->
                val status = not.status.status;
                when(status) {
                    RequestSinglePaymentNotification.InvoiceStatusType.PAID -> {
                        DB.updatePaymentStatus(paymentId, paid = true)
//                        ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))

                        sdk.sendCommand(CalculateNextOccurrenceRequest(subscription.data.frequency, now.epochSecond), { res, err ->
                            if(err != null || res.next_occurrence == null) {

                                logger.error("Error calculating next occurrence for subscription {}", subscription.data.portalSubscriptionId)
                                return@sendCommand
                            }
                            val nextOccurrence = Instant.ofEpochSecond(res.next_occurrence!!)
                            DB.updateSubscriptionLastPayment(subscription.data.id, now, nextOccurrence)

                            logger.info("User paid invoice of subscription {}", subscription.data.portalSubscriptionId)

                        })
                    }
                    RequestSinglePaymentNotification.InvoiceStatusType.USER_APPROVED, RequestSinglePaymentNotification.InvoiceStatusType.USER_SUCCESS
                        -> {}
                    else -> {
                        DB.updatePaymentStatus(paymentId, paid = false)
//                        ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                        logger.info("User did not pay invoice of subscription {}", subscription.data.portalSubscriptionId)
                    }
                }
            }) { res, err ->
                if (err != null) {
                    logger.info("Error requesting invoice of subscription {}, {}", subscription.data.portalSubscriptionId, err)
                    return@sendCommand
                }
            }
        }
    }, 0, 1, TimeUnit.MINUTES)
}

fun startWebApp(sdk: PortalSDK) {
    val app = Javalin.create { config ->
        run {

            // dev
            config.bundledPlugins.enableCors { cors ->
                cors.addRule {
                    it.anyHost()
                }
            }

            config.spaRoot.addFile("/", "/static/index.html")
            config.staticFiles.add { staticFiles ->
                staticFiles.hostedPath = "/"                    // change to host files on a subpath, like '/assets'
                staticFiles.directory = "/static"               // the directory where your files are located
                staticFiles.location = Location.CLASSPATH       // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
//                staticFiles.precompress = false                 // if the files should be pre-compressed and cached in memory (optimization)
//                staticFiles.aliasCheck = null                   // you can configure this to enable symlinks (= ContextHandler.ApproveAliases())
//                staticFiles.headers = mapOf(...)                // headers that will be set for the files
//                staticFiles.skipFileFunction = { req -> false } // you can use this to skip certain files in the dir, based on the HttpServletRequest
//                staticFiles.mimeTypes.add(mimeType, ext)        // you can add custom mimetypes for extensions
            }
        }
    }
    javalinApp = app
        .get("/healthcheck") { ctx -> ctx.status(HttpStatus.OK).result("OK") }
        .start(7070)

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutdown hook: closing resources...")
        try {
            recurringPaymentThread?.cancel(true)
            portalSdk?.disconnect()
            DB.disconnect()
            app.stop()
        } catch (e: Exception) {
            logger.warn("Error during shutdown", e)
        }
        logger.info("Shutdown complete.")
    })

//    app.events { event ->
//        event.serverStopped {
//            sdk.disconnect()
//        }
//    }

    sdk.onClose {
        logger.error("SDK closed")

        logger.info("Closing dn connection...")
        DB.disconnect()

        logger.info("Closing recurringPaymentThread...")
        recurringPaymentThread?.cancel(true)

        logger.info("Closing webserver")
        app.stop()

        exitProcess(1)
    }

    app.exception(Exception::class.java) { e, ctx ->
        logger.error("Server unexpected error", e)
    }

    app.wsException(Exception::class.java) { e, ctx ->
        logger.error("Server ws unexpected error", e)
    }

    app.ws("ws", { ws ->
        ws.onConnect { ctx ->
            logger.debug("New connection ${ctx.host()}")
//            generateQR(sdk, ctx, staticToken = null)
        }
        ws.onClose { ctx ->
        }

        ws.onMessage { ctx ->
            if(ctx.message() == "PING") {
                ctx.send("PONG")
                return@onMessage
            }

            val command = ctx.message().split(",")
            val cmd = command.first()
            when(cmd) {
                "GenerateQRCode" -> {
                    var staticToken: String? = command.getOrNull(1)
                    if (staticToken.isNullOrEmpty()) {
                        staticToken = null
                    }
                    generateQR(sdk, ctx, staticToken)
                }
                "LoginWithNip05" -> {
                    if (command.size < 2) {
                        ctx.sendErr("Malformed message: LoginWithNip05 requires nip05 address")
                        return@onMessage
                    }
                    val nip05Address = command[1]
                    if(nip05Address.isEmpty()) {
                        ctx.sendErr("Nip05 address can not be empty")
                        return@onMessage
                    }

                    sdk.sendCommand(FetchNip05ProfileRequest(nip05Address)) { res, err ->
                        if (err != null) {
                            ctx.sendErr(err)
                            return@sendCommand
                        }

                        sendAuthRequest(sdk, ctx, pub = res.profile.public_key)
                    }

                }
                "RequestPaymentsHistory" -> {
                    if (command.size < 2) {
                        ctx.sendErr("Malformed message: RequestPaymentsHistory requires session token")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }
                    ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                }
                "RequestSubscriptionsHistory" -> {
                    if (command.size < 2) {
                        ctx.sendErr("Malformed message: RequestSubscriptionsHistory requires session token")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }
                    ctx.sendSuccess("SubscriptionsHistory", mapOf("history" to DB.getSubscriptionsHistory(userState.key)))
                }
                "CashuMintAndSend" -> {
                    if (command.size < 7) {
                        ctx.sendErr("Malformed message: CashuMintAndSend requires sessionToken,mintUrl,authToken,unit,amount,description")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }

                    val mintUrl = command[2]
                    val staticAuthToken = command[3]
                    val unit = command[4]
                    val amount = command[5].toLongOrNull()
                    if(amount == null) {
                        ctx.sendErr("Amount not a valid number")
                        return@onMessage
                    }
                    val description = command[6]

                    sdk.sendCommand(MintCashuRequest(mintUrl, staticAuthToken, unit, amount, description), { res, err ->
                        if (err != null) {
                            ctx.sendErr(err)
                            return@sendCommand
                        }
                        sdk.sendCommand(SendCashuDirectRequest(userState.key, emptyList(), res.token), { res, err ->
                            if (err != null) {
                                ctx.sendErr(err)
                                return@sendCommand
                            }
                            ctx.sendSuccess("CashuSent", mapOf())
                        })
                    })
                }
                "BurnToken" -> {
                    if (command.size < 6) {
                        ctx.sendErr("Malformed message: BurnToken requires sessionToken,mintUrl,authToken,unit,amount")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }

                    val mintUrl = command[2]
                    val staticAuthToken = command[3]
                    val unit = command[4]
                    val amount = command[5].toLongOrNull()
                    if(amount == null) {
                        ctx.sendErr("Amount not a valid number")
                        return@onMessage
                    }
                    sdk.sendCommand(RequestCashuRequest(mintUrl, unit, amount, userState.key, emptyList()), { res, err ->
                        if (err != null) {
                            ctx.sendErr(err)
                            return@sendCommand
                        }

                        when(res.status.status) {
                            CashuResponseStatus.Status.SUCCESS -> {
                                sdk.sendCommand(BurnCashuRequest(mintUrl, staticAuthToken, unit, res.status.token), { res, err ->
                                    if (err != null) {
                                        ctx.sendErr(err)
                                        return@sendCommand
                                    }
                                    ctx.sendSuccess("BurnToken", mapOf("amount" to res.amount))
                                })
                            }
                            CashuResponseStatus.Status.INSUFFICIENT_FUNDS -> {
                                ctx.sendErr("Insufficient cashu tokens")
                            }
                            CashuResponseStatus.Status.REJECTED -> {
                                ctx.sendErr("Rejected cashu token request")
                            }
                        }
                    })
                }
                "RequestSinglePayment" -> {
                    if (command.size < 5) {
                        ctx.sendErr("Malformed message: RequestSinglePayment requires sessionToken,currency,amount,description")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }

                    val currencyStr = command[2]
                    var currency = Currency.MILLISATS
                    if(currencyStr != "Millisats") {
                        currency = Currency.FIAT(currencyStr)
                    }

                    val amount = if(currency == Currency.MILLISATS) {
                        val tmp = command[3].toLongOrNull()
                        if(tmp == null) {
                            ctx.sendErr("Amount not a valid number")
                            return@onMessage
                        }
                        tmp
                    } else {
                        val tmp = command[3].replace(",", ".").toDoubleOrNull()
                        if(tmp == null) {
                            ctx.sendErr("Amount not a valid floating number")
                            return@onMessage
                        }
                        (tmp * 100.0).toLong()
                    }

                    val description = command[4]

                    // Register payment before request so notification callback always has a valid paymentId (avoids race)
                    val paymentId = DB.registerPayment(userState.key, currency, amount, description, portalSubscriptionId = null)
                    val req = SinglePaymentRequestContent(description, amount, currency, null, null)
                    sdk.sendCommand(RequestSinglePaymentRequest(userState.key, emptyList(), req) { not ->
                        val status = not.status.status
                        when(status) {
                            RequestSinglePaymentNotification.InvoiceStatusType.PAID -> {
                                DB.updatePaymentStatus(paymentId, paid = true)
                                ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                            }
                            RequestSinglePaymentNotification.InvoiceStatusType.USER_APPROVED, RequestSinglePaymentNotification.InvoiceStatusType.USER_SUCCESS
                                -> {}
                            else -> {
                                DB.updatePaymentStatus(paymentId, paid = false)
                                ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                            }
                        }
                    }) { res, err ->
                        if (err != null) {
                            DB.updatePaymentStatus(paymentId, paid = false)  // request failed: mark as not paid
                            ctx.sendErr(err)
                            return@sendCommand
                        }
                        ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                        ctx.sendSuccess("RequestSinglePayment", mapOf())
                    }
                }
                "GetSubscriptionPayments" -> {
                    if (command.size < 3) {
                        ctx.sendErr("Malformed message: GetSubscriptionPayments requires sessionToken,subscriptionId")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }
                    val subscription = command[2]

                    ctx.sendSuccess("SubscriptionPayments", mapOf("history" to DB.getSubscriptionAllPayments(userState.key, subscription)))
                }
                "RequestRecurringPayment" -> {
                    if (command.size < 6) {
                        ctx.sendErr("Malformed message: RequestRecurringPayment requires sessionToken,currency,amount,description,frequency")
                        return@onMessage
                    }
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }

                    val currencyStr = command[2]
                    var currency = Currency.MILLISATS
                    if(currencyStr != "Millisats") {
                        currency = Currency.FIAT(currencyStr)
                    }

                    val amount = if(currency == Currency.MILLISATS) {
                        val tmp = command[3].toLongOrNull()
                        if(tmp == null) {
                            ctx.sendErr("Amount not a valid number")
                            return@onMessage
                        }
                        tmp
                    } else {
                        val tmp = command[3].replace(",", ".").toDoubleOrNull()
                        if(tmp == null) {
                            ctx.sendErr("Amount not a valid floating number")
                            return@onMessage
                        }
                        (tmp * 100.0).toLong()
                    }

                    val description = command[4]
                    val frequency = command[5]

                    val now = Instant.now()


                    val req = RecurringPaymentRequestContent(
                        description,
                        amount,
                        currency,
                        null, //auth token
                        RecurrenceInfo(null, frequency, null, now.epochSecond),
                        Instant.now().plusSeconds(3600).epochSecond,
                    )
                    sdk.sendCommand(RequestRecurringPaymentRequest(userState.key, emptyList(), req), { res, err ->
                        if (err != null) {
                            ctx.sendErr(err)
                            return@sendCommand
                        }
                        when(res.status.status.status) {
                            RequestRecurringPaymentResponse.RequestRecurringPaymentStatusType.CONFIRMED -> {
                                val subscriptionId = DB.registerSubscription(
                                    userState.key,
                                    currency,
                                    amount,
                                    frequency,
                                    description,
                                    nextPaymentAt = now,
                                    portalSubscriptionId = res.status.status.subscription_id!!
                                )
                                ctx.sendSuccess("SubscriptionsHistory", mapOf("history" to DB.getSubscriptionsHistory(userState.key)))

                            }
                            RequestRecurringPaymentResponse.RequestRecurringPaymentStatusType.REJECTED -> {
                                ctx.sendSuccess("SubscriptionsHistory", mapOf("history" to DB.getSubscriptionsHistory(userState.key)))
                            }
                        }
                    })


                }
            }
            logger.debug("OnMessage ${ctx.message()}")
        }
    })
}

fun generateQR(sdk: PortalSDK, ctx: WsContext, staticToken: String?) {
    sdk.sendCommand(KeyHandshakeUrlRequest(staticToken, null) { notification ->

        val pub = notification.mainKey
        sendAuthRequest(sdk, ctx, pub)
    }) { res, err ->
        if (err != null) {
            ctx.sendErr(err)
            return@sendCommand
        }
        ctx.sendSuccess("KeyHandshakeUrlRequest", mapOf("url" to res.url()))
    }
}

fun sendAuthRequest(sdk: PortalSDK, ctx: WsContext, pub: String) {
    ctx.sendSuccess("PendingAuthRequest", mapOf())
    sdk.sendCommand(AuthenticateKeyRequest(pub, emptyList()), { res, err ->
        if (err != null) {
            ctx.sendErr(err)
            ctx.sendSuccess("CancelledAuthRequest", mapOf())
            return@sendCommand
        }
        if (res.event().status.status() == AuthenticateKeyResponse.AuthResponseStatusType.DECLINED) {
            ctx.sendErr("Authentication failed. Reason: '${res.event().status().reason()}'")
            ctx.sendSuccess("CancelledAuthRequest", mapOf())
            return@sendCommand
        }
        sdk.sendCommand(FetchProfileRequest(pub)) { res, err ->
            if (err != null) {
                ctx.sendErr(err)
                ctx.sendSuccess("CancelledAuthRequest", mapOf())
                return@sendCommand
            }

            val sessionToken = UUID.randomUUID().toString()
            val sessionState = UserSession(pub, res.profile)
            DB.insertUserToken(sessionToken, sessionState)
            ctx.sendSuccess("AuthenticateKeyRequest", mapOf("sessionToken" to sessionToken, "state" to sessionState))
        }
    })
}

fun buildFrontend() {
    logger.info("DEV_MODE=true → building frontend...")

    val processBuilder = ProcessBuilder()
        .directory(java.io.File("/home/unldenis/IdeaProjects/portal-demo/frontend"))
        .command("npm", "run", "build")
//        .inheritIO() // show output in console

    val environment = processBuilder.environment()
    environment["VITE_BACKEND_API_WS"] = "ws://localhost:7070/ws"

    val process = processBuilder.start()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        logger.error("❌ Frontend build failed (exit code $exitCode)")
        return
    }

    // copy dist → static
    val distDir = java.nio.file.Paths.get("frontend", "dist")
    val staticDir = java.nio.file.Paths.get("/home/unldenis/IdeaProjects/portal-demo", "src", "main", "resources", "static")

    if (java.nio.file.Files.exists(staticDir)) {
        staticDir.toFile().deleteRecursively()
    }
    java.nio.file.Files.createDirectories(staticDir)

    distDir.toFile().copyRecursively(staticDir.toFile(), overwrite = true)

    logger.info("✅ Frontend build copied to resources/static")
}