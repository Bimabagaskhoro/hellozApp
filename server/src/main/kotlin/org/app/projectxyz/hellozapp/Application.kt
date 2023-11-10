package org.app.projectxyz.hellozapp

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.app.projectxyz.hellozapp.mock.spalshscreen.splashScreenHelper
import org.app.projectxyz.hellozapp.mock.wording.wordingHelper
import org.app.projectxyz.hellozapp.utils.PORT

fun main() {
    embeddedServer(Netty, port = PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/api/hellozapp/wording") {
            call.respond(wordingHelper)
        }
        get {
            call.respond(splashScreenHelper)
        }
    }
}
