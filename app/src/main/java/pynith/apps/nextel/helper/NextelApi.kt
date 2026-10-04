package pynith.apps.nextel.helper

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import pynith.apps.nextel.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiResult {
    data class Success(
        val message: String,
        val data: JSONObject,
        val statusCode: Int,
        val cookies: List<String> = emptyList()
    ) : ApiResult()

    data class Failure(val error: ApiError) : ApiResult()
}

data class ApiError(
    val statusCode: Int,
    val message: String,
    val fields: Map<String, List<String>> = emptyMap(),
    val retryAfterSeconds: Long? = null,
    val data: JSONObject = JSONObject()
) {
    fun firstFieldError(name: String): String? = fields[name]?.firstOrNull()

    fun displayMessage(): String {
        val retryAfter = retryAfterSeconds
        return if (statusCode == 429 && retryAfter != null && retryAfter > 0 &&
            !message.contains("second", ignoreCase = true)
        ) {
            "$message Please try again in $retryAfter seconds."
        } else {
            message
        }
    }
}

/** Thin JSON client for the existing Laravel /api/v1 contract. */
class NextelApi(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    fun get(path: String, token: String? = null, callback: (ApiResult) -> Unit) {
        enqueue("GET", path, null, token, callback)
    }

    fun post(
        path: String,
        body: JSONObject = JSONObject(),
        token: String? = null,
        callback: (ApiResult) -> Unit
    ) {
        enqueue("POST", path, body, token, callback)
    }

    fun requestBlocking(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): ApiResult {
        return try {
            httpClient.newCall(buildRequest(method, path, body, token)).execute().use(::parseResponse)
        } catch (error: IOException) {
            ApiResult.Failure(ApiError(0, error.localizedMessage ?: "Unable to reach the server."))
        } catch (error: Exception) {
            ApiResult.Failure(ApiError(0, error.localizedMessage ?: "The server returned an invalid response."))
        }
    }

    private fun enqueue(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?,
        callback: (ApiResult) -> Unit
    ) {
        val request = try {
            buildRequest(method, path, body, token)
        } catch (error: Exception) {
            callback(ApiResult.Failure(ApiError(0, error.localizedMessage ?: "Invalid API configuration.")))
            return
        }

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                callback(ApiResult.Failure(ApiError(0, error.localizedMessage ?: "Unable to reach the server.")))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    callback(parseResponse(it))
                }
            }
        })
    }

    private fun buildRequest(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?
    ): Request {
        val url = "$baseUrl/${path.trimStart('/')}"
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        return if (method.equals("GET", ignoreCase = true)) {
            builder.get().build()
        } else {
            val requestBody = (body ?: JSONObject()).toString().toRequestBody(JSON_MEDIA_TYPE)
            builder.post(requestBody).build()
        }
    }

    private fun parseResponse(response: Response): ApiResult {
        val rawBody = response.body?.string().orEmpty()
        val envelope = try {
            JSONObject(rawBody)
        } catch (_: Exception) {
            null
        }
        val message = envelope?.optString("message")
            ?.takeIf { it.isNotBlank() }
            ?: if (response.isSuccessful) "Request completed." else "The request could not be completed."
        val declaredSuccess = envelope?.optBoolean("success", response.isSuccessful) ?: false

        if (response.isSuccessful && declaredSuccess) {
            return ApiResult.Success(
                message = message,
                data = envelope?.optJSONObject("data") ?: JSONObject(),
                statusCode = response.code,
                cookies = response.headers.values("Set-Cookie")
            )
        }

        val errors = linkedMapOf<String, List<String>>()
        envelope?.optJSONObject("errors")?.let { errorObject ->
            val keys = errorObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = errorObject.opt(key)
                val values = when (value) {
                    is JSONArray -> (0 until value.length()).mapNotNull { index ->
                        value.optString(index).takeIf(String::isNotBlank)
                    }
                    null -> emptyList()
                    else -> listOf(value.toString()).filter(String::isNotBlank)
                }
                if (values.isNotEmpty()) errors[key] = values
            }
        }

        return ApiResult.Failure(
            ApiError(
                statusCode = response.code,
                message = message,
                fields = errors,
                retryAfterSeconds = response.header("Retry-After")?.toLongOrNull(),
                data = envelope?.optJSONObject("data") ?: JSONObject()
            )
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val httpClient: OkHttpClient = OkHttpClient.Builder()
            // API requests read cookies (app_gate, web session) from the shared
            // WebView cookie store instead of hard-coding a Cookie header.
            .cookieJar(WebCookieJar)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
