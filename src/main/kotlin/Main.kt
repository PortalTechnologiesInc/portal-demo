package cc.getportal.demo

import cc.getportal.PortalSDK
import cc.getportal.command.notification.RequestSinglePaymentNotification
import cc.getportal.command.request.AuthenticateKeyRequest
import cc.getportal.command.request.BurnCashuRequest
import cc.getportal.command.request.FetchProfileRequest
import cc.getportal.command.request.KeyHandshakeUrlRequest
import cc.getportal.command.request.MintCashuRequest
import cc.getportal.command.request.RequestCashuRequest
import cc.getportal.command.request.RequestSinglePaymentRequest
import cc.getportal.command.request.SendCashuDirectRequest
import cc.getportal.command.response.AuthenticateKeyResponse
import cc.getportal.model.CashuResponseStatus
import cc.getportal.model.Currency
import cc.getportal.model.Profile
import cc.getportal.model.SinglePaymentRequestContent
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.Random
import java.util.UUID

private val logger = LoggerFactory.getLogger("Bootstrap")
lateinit var sdk: PortalSDK

fun main() {
    val healthEndpoint = System.getenv("REST_HEALTH_ENDPOINT")
    if(healthEndpoint == null) {
        logger.error("missing REST_HEALTH_ENDPOINT env variable")
        return
    }

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

    // connect DB
    DB.connect("data", "data.db")

    // connect to Portal
    sdk = PortalSDK(healthEndpoint, wsEndpoint)
    sdk.connect(token)

    // start web app after 5 seconds
    Thread.sleep(1000 * 5)
    startWebApp()
}

fun startWebApp() {
    val app = Javalin.create { config ->
        run {

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
        .get("/api/v1") { ctx -> ctx.result("Hello World") }
        .start(7070)

    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
        app.stop()
    }))

    app.events({ event ->
        event.serverStopped({
            sdk.disconnect()
        })
    })

    app.ws("ws", { ws ->
        ws.onConnect { ctx ->
            logger.debug("New connection ${ctx.host()}")
            generateQR(ctx, staticToken = null)
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
                    val staticToken = command[1]
                    if(staticToken.isEmpty()) {
                        ctx.sendErr("Static token can not be empty")
                        return@onMessage
                    }
                    generateQR(ctx, staticToken)
                }
                "RequestPaymentsHistory" -> {
                    val sessionToken = command[1]
                    val userState = DB.getUserByToken(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }
                    ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))
                }
                "CashuMintAndSend" -> {
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

                    val paymentId = DB.registerPayment(userState.key, currency, amount, description)
                    ctx.sendSuccess("PaymentsHistory", mapOf("history" to DB.getPaymentsHistory(userState.key)))

                    val req = SinglePaymentRequestContent(description, amount, currency, null, null)
                    sdk.sendCommand(RequestSinglePaymentRequest(userState.key, emptyList(), req) { not ->
                        val status = not.status.status;
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
                            ctx.sendErr(err)
                            return@sendCommand
                        }
                        ctx.sendSuccess("RequestSinglePayment", mapOf())
                    }
                }
            }
            logger.info("OnMessage ${ctx.message()}")
        }
    })
}

fun generateQR(ctx: WsContext, staticToken: String?) {
    sdk.sendCommand(KeyHandshakeUrlRequest(staticToken, null) { notification ->

        val pub = notification.mainKey

        sdk.sendCommand(AuthenticateKeyRequest(pub, emptyList()), { res, err ->
            if (err != null) {
                ctx.sendErr(err)
                return@sendCommand
            }
            if (res.event().status.status() == AuthenticateKeyResponse.AuthResponseStatusType.DECLINED) {
                ctx.sendErr("Authentication failed. Reason: '${res.event().status().reason()}'")
                return@sendCommand
            }
            sdk.sendCommand(FetchProfileRequest(pub), { res, err ->
                if (err != null) {
                    ctx.sendErr(err)
                    return@sendCommand
                }

                val sessionToken = UUID.randomUUID().toString()
                val sessionState = UserSession(pub, res.profile)
                DB.insertUserToken(sessionToken, sessionState)
                ctx.sendSuccess("AuthenticateKeyRequest", mapOf("sessionToken" to sessionToken, "state" to sessionState))
            })
        })
    }) { res, err ->
        if (err != null) {
            ctx.sendErr(err)
            return@sendCommand
        }
        ctx.sendSuccess("KeyHandshakeUrlRequest", mapOf("url" to res.url()))
    }
}

fun buildFrontend() {
    logger.info("DEV_MODE=true → building frontend...")

    val processBuilder = ProcessBuilder()
        .directory(java.io.File("/home/user/portal/portal-demo/frontend"))
        .command("npm", "run", "build")
//        .inheritIO() // show output in console

    val process = processBuilder.start()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        logger.error("❌ Frontend build failed (exit code $exitCode)")
        return
    }

    // copy dist → static
    val distDir = java.nio.file.Paths.get("frontend", "dist")
    val staticDir = java.nio.file.Paths.get("/home/user/portal/portal-demo", "src", "main", "resources", "static")

    if (java.nio.file.Files.exists(staticDir)) {
        staticDir.toFile().deleteRecursively()
    }
    java.nio.file.Files.createDirectories(staticDir)

    distDir.toFile().copyRecursively(staticDir.toFile(), overwrite = true)

    logger.info("✅ Frontend build copied to resources/static")
}