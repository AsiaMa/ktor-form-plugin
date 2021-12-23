package com.oasis.controller

import com.oasis.models.query.SendSmsAsyncQuery
import com.oasis.service.SmsService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*

object SmsController {
    private val smsService: SmsService = SmsService()

    // suspend fun sendSmsAsync(call: ApplicationCall) {
    //     val formParameters = call.receiveParameters()
    //     val tel = formParameters["tel"].toString()
    //     val code = formParameters["code"].toString()
    //     val lang = formParameters["lang"].toString()
    //     val sign = formParameters["sign"].toString()
    //
    //     val sendSmsAsyncQuery = SendSmsAsyncQuery(tel, code, lang, sign)
    //
    //     call.respond(smsService.sendSmsAsync(sendSmsAsyncQuery))
    // }

    suspend fun sendSmsAsync(call: ApplicationCall) {
        val sendSmsAsyncQuery = call.receive<SendSmsAsyncQuery>()

        call.respond(smsService.sendSmsAsync(sendSmsAsyncQuery))
    }
}