package com.oasis.plugins

import com.oasis.config.statusPages
import com.oasis.controller.SmsController
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {

    // Starting point for a Ktor app:
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
    routing {
        install(StatusPages) {
            statusPages()
        }

        route("/api") {

            post("/send-sms/code_async") {
                SmsController.sendSmsAsync(call)
            }
        }
    }
}
