package com.oasis.models.query

data class SendSmsAsyncQuery(val tel: String, val code: String, val lang: String, val sign: String)