package cc.getportal.demo

import io.javalin.Javalin

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
}

