package cc.getportal.demo

import cc.getportal.command.notification.KeyHandshakeUrlNotification
import cc.getportal.command.request.AuthenticateKeyRequest
import cc.getportal.command.request.FetchProfileRequest
import cc.getportal.command.request.KeyHandshakeUrlRequest
import cc.getportal.command.response.AuthenticateKeyResponse
import com.sun.tools.javac.resources.ct
import io.javalin.Javalin
import io.javalin.websocket.WsContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Bootstrap")

fun main() {
    Portal.connect(healthEndpoint = "http://localhost:3000/health", wsEndpoint = "ws://localhost:3000/ws", token = "token")

    Thread.sleep(1000 * 5)
    startWebApp()
}

fun startWebApp() {
    val app = Javalin.create(/*config*/)
        .get("/") { ctx -> ctx.result("Hello World") }
        .start(7070)

    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
        app.stop()
    }))

    app.events({ event ->
        event.serverStopped({
            Portal.disconnect()
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
                    generateQR(ctx, staticToken)
                }
            }
            logger.debug("OnMessage ${ctx.message()}")
        }
    })
}

fun generateQR(ctx: WsContext, staticToken: String?) {
    Portal.sdk.sendCommand(KeyHandshakeUrlRequest(staticToken, null) { notification ->

        val pub = notification.mainKey

        Portal.sdk.sendCommand(AuthenticateKeyRequest(pub, emptyList()), { res, err ->
            if (err != null) {
                ctx.sendErr(err)
                return@sendCommand
            }
            if (res.event().status.status() == AuthenticateKeyResponse.AuthResponseStatusType.DECLINED) {
                ctx.sendErr("Authentication failed. Reason: '${res.event().status().reason()}'")
                return@sendCommand
            }
            Portal.sdk.sendCommand(FetchProfileRequest(pub), { res, err ->
                if (err != null) {
                    ctx.sendErr(err)
                    return@sendCommand
                }
                ctx.sendSuccess("AuthenticateKeyRequest", mapOf("profile" to res.profile))
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

