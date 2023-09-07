package net.corda.messagebus.kafka.utils

import java.nio.charset.StandardCharsets
import net.corda.messagebus.api.CordaTopicPartition
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KafkaConversionsTest {
    private val partition = 100
    private val topicName = "topic1"
    private val testTopicPrefix = "test-"

    @Test
    fun toCordaConsumerRecord() {
        val headerData = "h_value".toByteArray(StandardCharsets.UTF_8)
        val kafkaConsumerRecord = ConsumerRecord<Any, Any>(
            "prefix1_topic1",
            1,
            2,
            "key1",
            10
        )

        kafkaConsumerRecord.headers().add("h1", headerData)

        val result = kafkaConsumerRecord.toCordaConsumerRecord<String, Int>("prefix1_")

        assertThat(result.topic).isEqualTo("topic1")
        assertThat(result.partition).isEqualTo(1)
        assertThat(result.offset).isEqualTo(2)
        assertThat(result.key).isEqualTo("key1")
        assertThat(result.value).isEqualTo(10)
        assertThat(result.headers).containsExactly("h1" to "h_value")
    }

    @Test
    fun `toCordaConsumerRecord null record value`() {
        val headerData = "h_value".toByteArray(StandardCharsets.UTF_8)
        val kafkaConsumerRecord = ConsumerRecord<Any, Any>(
            "prefix1_topic1",
            1,
            2,
            "key1",
            null
        )

        kafkaConsumerRecord.headers().add("h1", headerData)

        val result = kafkaConsumerRecord.toCordaConsumerRecord<String, Int>("prefix1_")

        assertThat(result.topic).isEqualTo("topic1")
        assertThat(result.partition).isEqualTo(1)
        assertThat(result.offset).isEqualTo(2)
        assertThat(result.key).isEqualTo("key1")
        assertThat(result.value).isNull()
        assertThat(result.headers).containsExactly("h1" to "h_value")
    }

    @Test
    fun `toCordaConsumerRecord with key and value`() {
        val headerData = "h_value".toByteArray(StandardCharsets.UTF_8)
        val kafkaConsumerRecord = ConsumerRecord<Any, Any>(
            "prefix1_topic1",
            1,
            2,
            "",
            0
        )

        kafkaConsumerRecord.headers().add("h1", headerData)

        val result = kafkaConsumerRecord.toCordaConsumerRecord<String, Int>("prefix1_", "key1", 10)

        assertThat(result.topic).isEqualTo("topic1")
        assertThat(result.partition).isEqualTo(1)
        assertThat(result.offset).isEqualTo(2)
        assertThat(result.key).isEqualTo("key1")
        assertThat(result.value).isEqualTo(10)
        assertThat(result.headers).containsExactly("h1" to "h_value")
    }

    @Test
    fun `toCordaConsumerRecord with key and null value`() {
        val headerData = "h_value".toByteArray(StandardCharsets.UTF_8)
        val kafkaConsumerRecord = ConsumerRecord<Any, Any>(
            "prefix1_topic1",
            1,
            2,
            "",
            0
        )

        kafkaConsumerRecord.headers().add("h1", headerData)

        val result = kafkaConsumerRecord.toCordaConsumerRecord<String, Int>("prefix1_", "key1", null)

        assertThat(result.topic).isEqualTo("topic1")
        assertThat(result.partition).isEqualTo(1)
        assertThat(result.offset).isEqualTo(2)
        assertThat(result.key).isEqualTo("key1")
        assertThat(result.value).isNull()
        assertThat(result.headers).containsExactly("h1" to "h_value")
    }

    @Test
    fun toTopicPartitionAddsPrefixToTopic() {
        val cordaTopicPartition = CordaTopicPartition(topicName, partition)
        val topicPartition = cordaTopicPartition.toTopicPartition(testTopicPrefix)

        assertThat(topicPartition.partition()).isEqualTo(partition)
        assertThat(topicPartition.topic()).isEqualTo("$testTopicPrefix$topicName")
    }

    @Test
    fun toTopicPartitionDoesNotAddPrefixToTopicWhenPrefixAlreadyAdded() {
        val cordaTopicPartition = CordaTopicPartition("$testTopicPrefix$topicName", partition)
        val topicPartition = cordaTopicPartition.toTopicPartition(testTopicPrefix)

        assertThat(topicPartition.partition()).isEqualTo(partition)
        assertThat(topicPartition.topic()).isEqualTo("$testTopicPrefix$topicName")
    }

    @Test
    fun toCordaTopicPartitionRemovesPrefixFromTopic() {
        val topicPartition = TopicPartition("$testTopicPrefix$topicName", partition)
        val cordaTopicPartition = topicPartition.toCordaTopicPartition(testTopicPrefix)

        assertThat(cordaTopicPartition.partition).isEqualTo(partition)
        assertThat(cordaTopicPartition.topic).isEqualTo(topicName)
    }

    @Test
    fun toCordaTopicPartitionDoesNotRemoveAnythingWhenPrefixIsNotFound() {
        val topicPartition = TopicPartition(topicName, partition)
        val cordaTopicPartition = topicPartition.toCordaTopicPartition(testTopicPrefix)

        assertThat(cordaTopicPartition.partition).isEqualTo(partition)
        assertThat(cordaTopicPartition.topic).isEqualTo(topicName)
    }
}
