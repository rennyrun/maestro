package maestro

import maestro.mockserver.MockEvent
import maestro.utils.StringUtils.toRegexSafe

data class OutgoingRequestRules(
    val url: String? = null,
    val assertHeaderIsPresent: String? = null,
    val assertHeadersAndValues: Map<String, String> = emptyMap(),
    val assertHttpMethod: String? = null,
    val assertRequestBodyContains: String? = null,
)

object AssertOutgoingRequestService {

    private val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)

    fun assert(events: List<MockEvent>, rules: OutgoingRequestRules): List<MockEvent> {
        val eventsFilteredByUrl = rules.url?.let { url ->
            events.filter { e -> e.path == url || e.path.matches(url.toRegexSafe(REGEX_OPTIONS)) }
        } ?: events

        val eventsFilteredByHttpMethod = rules.assertHttpMethod?.let { httpMethod ->
            eventsFilteredByUrl.filter { e -> e.method == httpMethod }
        } ?: eventsFilteredByUrl

        val eventsFilteredByHeader = rules.assertHeaderIsPresent?.let { header ->
            eventsFilteredByHttpMethod.filter { e -> e.headers?.containsKey(header.lowercase()) == true }
        } ?: eventsFilteredByHttpMethod

        val eventsFilteredByHeadersAndValues = rules.assertHeadersAndValues.entries.fold(eventsFilteredByHeader) { eventsList, (header, value) ->
            eventsList.filter { e -> e.headers?.get(header.lowercase()) == value }
        }

        val eventsMatching = rules.assertRequestBodyContains?.let { requestBody ->
            eventsFilteredByHeadersAndValues.filter { e -> e.bodyAsString?.contains(requestBody) == true }
        } ?: eventsFilteredByHeadersAndValues

        println("from ${events.size} events, ${eventsMatching.size} match the url ${rules.url}")
        return eventsMatching
    }

}