package com.oasis.util

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

class FormUrlEncodedConverter2(private val gson: Gson = Gson()) : ContentConverter {

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {

        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val type = request.typeInfo
        val javaType = type.jvmErasure

        return withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, List<String>>()

            // key1=value1&key2=value2&key3=value3
            channel.readUTF8Line()?.let { body ->
                val parameter = parseQueryString(body)
                for (entry in parameter.entries()) {
                    map[entry.key] = entry.value
                }
            }

            val constructor: KFunction<Any> = javaType.primaryConstructor ?: javaType.constructors.single()
            val parameters = constructor.parameters
            val arguments = parameters.map { parameter ->
                val parameterType = parameter.type
                val parameterName = parameter.name ?: getParameterNameFromAnnotation(javaType, parameter)
                val value: Any? = createFromParameters(map, parameterName, parameterType.javaType, parameter.isOptional)
                parameter to value
            }.filterNot { it.first.isOptional && it.second == null }.toMap()

            try {
                constructor.callBy(arguments)
            } catch (cause: InvocationTargetException) {
                throw cause.cause ?: cause
            }
        }
    }

    private fun createFromParameters(
        parameters: MutableMap<String, List<String>>,
        name: String,
        type: Type,
        optional: Boolean
    ): Any? {
        return when (val values = parameters[name]) {
            null -> when {
                !optional -> {
                    throw MissingRequestParameterException(name)
                }
                else -> null
            }
            else -> {
                try {
                    ConversionUtil.fromValues(values, type)
                } catch (cause: Throwable) {
                    throw ParameterConversionException(name, type.toString(), cause)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getParameterNameFromAnnotation(kClass: KClass<*>, parameter: KParameter): String = TODO()

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any,
    ): Any {
        return TextContent(gson.toJson(value), contentType.withCharset(context.call.suitableCharset()))
    }
}

fun ContentNegotiation.Configuration.formUrlEncoded2(contentType: ContentType = ContentType.Application.FormUrlEncoded) {
    register(contentType, FormUrlEncodedConverter2())
}

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
// @Retention(AnnotationRetention.RUNTIME) still runtime, but KT-41082
annotation class SerialName(val value: String)