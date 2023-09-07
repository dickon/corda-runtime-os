package net.corda.messagebus.api.producer

/**
 * Object to encapsulate the events stored on topics
 * @property topic defined the id of the topic the record is stored on.
 * @property key is the unique per topic key for a record
 * @property value the value of the record
 * @property headers Optional list of headers to added to the message.
 */
@Deprecated(
    "This class is deprecated, use [CordaMessage] instead which provides a more generic interface.",
    replaceWith = ReplaceWith("CordaMessage")
)
data class CordaProducerRecord<K : Any, V : Any>(
    val topic: String,
    val key: K,
    val value: V?,
    val headers: List<Pair<String, String>> = listOf()
)

/**
 * Generic class to encapsulate outgoing events.
 * @property key is the unique per-topic key for a record.
 * @property value is the value of the record.
 */
sealed class CordaMessage<K, V>(
    val key: K,
    val value: V?,
) {
    /**
     * [CordaMessage] implementation for sending messages to an RPC endpoint.
     */
    data class RPC<K, V>(
        val msgKey: K,
        val msgValue: V?,
        val headers: List<Pair<String, String>> = listOf()
    ) : CordaMessage<K, V>(msgKey, msgValue)

    /**
     * [CordaMessage] implementation for sending messages to a Kafka topic.
     * @property topic defines the id of the topic the record is stored on.

     */
    data class Kafka<K, V>(
        val topic: String,
        val msgKey: K,
        val msgValue: V?,
        val headers: List<Pair<String, String>> = listOf()
    ) : CordaMessage<K, V>(msgKey, msgValue)

    /**
     * [CordaMessage] implementation for sending messages to a DB message bus.
     * @property topic Defines the id of the topic the record is stored on.
     * @property timestamp The timestamp of when the message was produced.
     */
    data class DB<K, V>(
        val topic: String,
        val msgKey: K,
        val msgValue: V?,
        val headers: List<Pair<String, String>> = listOf(),
        val timestamp: Long = 0
    ) : CordaMessage<K, V>(msgKey, msgValue)
}