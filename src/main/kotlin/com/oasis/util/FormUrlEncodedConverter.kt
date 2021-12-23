package com.oasis.util

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.features.ContentTransformationException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CopyableThrowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

class FormUrlEncodedConverter(private val gson: Gson = Gson()) : ContentConverter {

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val type = request.typeInfo
        val javaType = type.jvmErasure

        if (gson.isExcluded(javaType)) {
            throw ExcludedTypeGsonException(javaType)
        }

        return withContext(Dispatchers.IO) {
            //key1=value1&key2=value2&key3=value3
            val reader = channel.readUTF8Line()

            val map = mutableMapOf<String, String>()
            val keyValueList = reader!!.split("&")
            for (keyValue in keyValueList) {
                val itemList = keyValue.split("=")
                map[itemList[0]] = itemList[1]
            }

            gson.fromJson(gson.toJson(map), javaType.javaObjectType) ?: throw UnsupportedNullValuesException()
        }
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any {
        return TextContent(gson.toJson(value), contentType.withCharset(context.call.suitableCharset()))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal class ExcludedTypeGsonException(
        private val type: KClass<*>
    ) : Exception("Type ${type.jvmName} is excluded so couldn't be used in receive"),
        CopyableThrowable<ExcludedTypeGsonException> {

        override fun createCopy(): ExcludedTypeGsonException = ExcludedTypeGsonException(type).also {
            it.initCause(this)
        }
    }

    internal class UnsupportedNullValuesException :
        ContentTransformationException("Receiving null values is not supported")

    private fun Gson.isExcluded(type: KClass<*>) =
        excluder().excludeClass(type.java, false)
}

fun ContentNegotiation.Configuration.formUrlEncoded(contentType: ContentType = ContentType.Application.FormUrlEncoded) {
    register(contentType, FormUrlEncodedConverter())
}