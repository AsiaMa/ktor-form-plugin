package com.oasis.plugins

import com.oasis.util.formUrlEncoded2
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson()
        formUrlEncoded2()
    }
}