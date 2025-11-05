package cc.getportal.demo

import cc.getportal.command.notification.KeyHandshakeUrlNotification
import cc.getportal.command.request.AuthenticateKeyRequest
import cc.getportal.command.request.KeyHandshakeUrlRequest
import cc.getportal.command.response.AuthenticateKeyResponse
import com.sun.tools.javac.resources.ct
import io.javalin.Javalin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Bootstrap")

fun main() {
    Portal.connect(healthEndpoint = "http://localhost:3000/health", wsEndpoint = "ws://localhost:3000/ws", token = "token")

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

            Portal.sdk.sendCommand(KeyHandshakeUrlRequest { notification ->

                val pub = notification.mainKey

                Portal.sdk.sendCommand(AuthenticateKeyRequest(pub, emptyList()), { res, err ->
                    if (err != null){
                        ctx.sendErr(err)
                        return@sendCommand
                    }
                    if (res.event().status.status() == AuthenticateKeyResponse.AuthResponseStatusType.DECLINED) {
                        ctx.sendErr("Authentication failed. Reason: '${res.event().status().reason()}'")
                        return@sendCommand
                    }
                    ctx.sendSuccess("AuthenticateKeyRequest", mapOf())
                })
            }) { res, err ->
                if (err != null) {
                    ctx.sendErr(err)
                    return@sendCommand
                }
                ctx.sendSuccess("KeyHandshakeUrlRequest", mapOf("url" to res.url()))
            }
        }
        ws.onClose { ctx ->
        }

        ws.onMessage { ctx ->
            if(ctx.message() == "PING") {
                ctx.send("PONG")
                return@onMessage
            }

            logger.debug("OnMessage ${ctx.message()}")
        }
    })
}

