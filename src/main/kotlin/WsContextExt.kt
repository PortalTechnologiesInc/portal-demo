package cc.getportal.demo

import com.google.gson.Gson
import io.javalin.websocket.WsContext

val gson = Gson()

fun WsContext.sendSuccess(cmd : String, args : Map<String, Any?>) {
    val obj = mutableMapOf<String, Any?>()
    obj["type"] = "success"
    obj["cmd"] = cmd
    obj.putAll(args)

    val json = gson.toJson(obj)
    send(json)
}

fun WsContext.sendErr(message : String) {
    val obj = mutableMapOf<String, Any?>()
    obj["type"] = "error"
    obj["message"] = message

    val json = gson.toJson(obj)
    send(json)
}