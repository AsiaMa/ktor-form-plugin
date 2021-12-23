package com.oasis.config

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun StatusPages.Configuration.statusPages() {
    exception<RuntimeException> { cause ->
        call.respond(HttpStatusCode.InternalServerError, cause.message ?: "no message")
        throw cause
    }

    exception<MissingKotlinParameterException> { cause ->
        call.respond(
            HttpStatusCode.UnprocessableEntity,
            mapOf("errors" to mapOf(cause.parameter.name to listOf("can't be empty")))
        )
    }
}