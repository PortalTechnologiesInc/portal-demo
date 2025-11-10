package cc.getportal.demo

import cc.getportal.command.request.AuthenticateKeyRequest
import cc.getportal.command.request.FetchProfileRequest
import cc.getportal.command.request.KeyHandshakeUrlRequest
import cc.getportal.command.response.AuthenticateKeyResponse
import cc.getportal.model.Profile
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("Bootstrap")

object SessionsDB {
    val tokenToUser = ConcurrentHashMap<String, UserSession>()
}

data class UserSession(val key: String, val profile: Profile?)

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

    Portal.connect(healthEndpoint = healthEndpoint, wsEndpoint = wsEndpoint, token = token)

    Thread.sleep(1000 * 5)
    startWebApp()
}

fun startWebApp() {
    val app = Javalin.create { config ->
        run {
            if(System.getenv("DEV_MODE") == "true") {
                buildFrontend()
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
        .get("/api/v1") { ctx -> ctx.result("Hello World") }
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
                    if(staticToken.isEmpty()) {
                        ctx.sendErr("Static token can not be empty")
                        return@onMessage
                    }
                    generateQR(ctx, staticToken)
                }
                "RequestSinglePayment" -> {
                    val sessionToken = command[1]
                    val userState = SessionsDB.tokenToUser.get(sessionToken)
                    if(userState == null) {
                        ctx.sendErr("Not authenticated")
                        return@onMessage
                    }
                    ctx.sendSuccess("RequestSinglePayment", mapOf())
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

                val sessionToken = UUID.randomUUID().toString()
                val sessionState = UserSession(pub, res.profile)
                SessionsDB.tokenToUser[sessionToken] = sessionState
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