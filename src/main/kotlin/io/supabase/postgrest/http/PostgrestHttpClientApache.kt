package io.supabase.postgrest.http

import io.supabase.postgrest.json.PostgrestJsonConverter
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import java.net.URI

/**
 * Default implementation of the [PostgrestHttpClient] used by the PostgrestDefaultClient.
 *
 * Uses closable apache HTTP-Client 5.x.
 */
class PostgrestHttpClientApache(
        private val httpClient: () -> CloseableHttpClient,
        private val jsonConverter: PostgrestJsonConverter
) : PostgrestHttpClient {

    override fun execute(uri: URI, method: Method, headers: Map<String, String>, body: Any?): PostgrestHttpResponse {
        return httpClient().use { httpClient ->
            val httpRequest = HttpUriRequestBase(method.name, uri)
            body?.apply {
                val dataAsString = jsonConverter.serialize(body)
                httpRequest.entity = StringEntity(dataAsString)
            }
            headers.forEach { (name, value) -> httpRequest.addHeader(name, value) }

            return@use httpClient.execute(httpRequest, responseHandler(headers))
        }
    }

    private fun responseHandler(requestHeaders: Map<String, String>): HttpClientResponseHandler<PostgrestHttpResponse> {
        return HttpClientResponseHandler<PostgrestHttpResponse> { response ->
            throwIfError(response)

            val body = response.entity?.let { EntityUtils.toString(it) }

            val responseHeaders = response.headers.map { it.name to it.value }.toMap()

            val count = extractCount(responseHeaders, requestHeaders)

            return@HttpClientResponseHandler PostgrestHttpResponse(
                    status = response.code,
                    body = body,
                    count = count
            )
        }
    }

    private fun throwIfError(response: ClassicHttpResponse) {
        val status = response.code
        val statusSuccessful = status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION

        if (!statusSuccessful) {
            val entityAsString = response.entity?.let { EntityUtils.toString(it) }

            throw PostgrestHttpException(status, entityAsString)
        }
    }
}