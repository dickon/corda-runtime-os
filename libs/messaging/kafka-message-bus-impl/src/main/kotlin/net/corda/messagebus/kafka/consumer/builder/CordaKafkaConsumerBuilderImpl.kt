package net.corda.messagebus.kafka.consumer.builder

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import net.corda.avro.serialization.CordaAvroSerializationFactory
import java.util.Properties
import net.corda.libs.configuration.SmartConfig
import net.corda.messagebus.api.configuration.ConsumerConfig
import net.corda.messagebus.api.consumer.CordaConsumer
import net.corda.messagebus.api.consumer.CordaConsumerRebalanceListener
import net.corda.messagebus.api.consumer.builder.CordaConsumerBuilder
import net.corda.messagebus.kafka.config.MessageBusConfigResolver
import net.corda.messagebus.kafka.consumer.CordaKafkaConsumerImpl
import net.corda.messagebus.kafka.serialization.CordaKafkaSerializationFactory
import net.corda.messagebus.kafka.utils.KafkaRetryUtils.executeKafkaActionWithRetry
import net.corda.messaging.api.chunking.MessagingChunkFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.wiring.BundleWiring
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Generate a Corda Kafka Consumer.
 * Consumer uses deserializers that make use of the [avroSchemaRegistry]
 * Consumer may read records as chunks and will make use of a [ConsumerChunkDeserializerService] built using the
 * [messagingChunkFactory]
 */
@Component(service = [CordaConsumerBuilder::class])
class CordaKafkaConsumerBuilderImpl @Activate constructor(
    @Reference(service = MessagingChunkFactory::class)
    private val messagingChunkFactory: MessagingChunkFactory,
    @Reference(service = CordaAvroSerializationFactory::class)
    private val cordaAvroSerializationFactory: CordaAvroSerializationFactory,
    @Reference(service = CordaKafkaSerializationFactory::class)
    private val cordaKafkaSerializationFactory: CordaKafkaSerializationFactory
) : CordaConsumerBuilder {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun <K : Any, V : Any> createConsumer(
        consumerConfig: ConsumerConfig,
        messageBusConfig: SmartConfig,
        kClazz: Class<K>,
        vClazz: Class<V>,
        onSerializationError: (ByteArray) -> Unit,
        listener: CordaConsumerRebalanceListener?,
    ): CordaConsumer<K, V> {
        val resolver = MessageBusConfigResolver(messageBusConfig.factory)
        val (resolvedConfig, kafkaProperties) = resolver.resolve(messageBusConfig, consumerConfig)

        return executeKafkaActionWithRetry(
            action = {
                val avroKeyDeserializer =
                    cordaAvroSerializationFactory.createAvroDeserializer(onSerializationError, kClazz)
                val avroValueDeserializer =
                    cordaAvroSerializationFactory.createAvroDeserializer(onSerializationError, vClazz)

                val kafkaAdaptorKeyDeserializer =
                    cordaKafkaSerializationFactory.createAvroBasedKafkaDeserializer(onSerializationError, kClazz)
                val kafkaAdaptorValueDeserializer =
                    cordaKafkaSerializationFactory.createAvroBasedKafkaDeserializer(onSerializationError, vClazz)

                val consumerChunkDeserializerService =
                    messagingChunkFactory.createConsumerChunkDeserializerService(
                        avroKeyDeserializer,
                        avroValueDeserializer,
                        onSerializationError
                    )
                val consumer =
                    createKafkaConsumer(kafkaProperties, kafkaAdaptorKeyDeserializer, kafkaAdaptorValueDeserializer)
                CordaKafkaConsumerImpl(
                    resolvedConfig,
                    consumer,
                    listener,
                    consumerChunkDeserializerService,
                    KafkaClientMetrics(consumer)
                )
            },
            errorMessage = {
                "MessageBusConsumerBuilder failed to create consumer for group ${consumerConfig.group}, " +
                        "with configuration: $messageBusConfig"
            },
            log = log
        )
    }

    private fun createKafkaConsumer(
        kafkaProperties: Properties,
        keyDeserializer: Deserializer<Any>,
        valueDeserializer: Deserializer<Any>,
    ): KafkaConsumer<Any, Any> {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val currentBundle = FrameworkUtil.getBundle(KafkaConsumer::class.java)

        return try {
            if (currentBundle != null) {
                Thread.currentThread().contextClassLoader = currentBundle.adapt(BundleWiring::class.java).classLoader
            }
            KafkaConsumer(
                kafkaProperties,
                keyDeserializer,
                valueDeserializer
            )
        } finally {
            Thread.currentThread().contextClassLoader = contextClassLoader
        }
    }
}
