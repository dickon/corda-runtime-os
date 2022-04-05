package net.corda.cpiinfo.write.impl

import net.corda.configuration.read.ConfigurationReadService
import net.corda.cpiinfo.write.CpiInfoWriteService
import net.corda.libs.configuration.SmartConfig
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleStatus
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas.VirtualNode.Companion.CPI_INFO_TOPIC
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.debug
import net.corda.virtualnode.common.ConfigChangedEvent
import net.corda.virtualnode.common.MessagingConfigEventHandler
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import net.corda.data.packaging.CpiIdentifier as CpiIdentifierAvro
import net.corda.data.packaging.CpiMetadata as CpiMetadataAvro

/**
 * CPI Info Service writer so that we can [put] and [remove]
 * [CpiMetadata] from Kafka compacted queues
 *
 * Complements [CpiInfoReaderComponent]
 */
@Component(service = [CpiInfoWriteService::class])
class CpiInfoWriterComponentImpl @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = ConfigurationReadService::class)
    private val configurationReadService: ConfigurationReadService,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory
) : CpiInfoWriteService {
    companion object {
        val log: Logger = contextLogger()
        internal const val CLIENT_ID = "CPI_INFO_WRITER"
    }

    private val eventHandler: MessagingConfigEventHandler =
        MessagingConfigEventHandler(configurationReadService, this::onConfigChangeEvent, this::onConfig)

    override val lifecycleCoordinatorName = LifecycleCoordinatorName.forComponent<CpiInfoWriteService>()

    private val coordinator = coordinatorFactory.createCoordinator(lifecycleCoordinatorName, eventHandler)

    private var publisher: Publisher? = null

    @Suppress("Warnings")
    override fun put(cpiMetadata: CpiMetadata) {
        publish(listOf(Record(CPI_INFO_TOPIC, cpiMetadata.cpiId.toAvro(), cpiMetadata.toAvro())))
    }

    override fun remove(cpiMetadata: CpiMetadata) {
        publish(listOf(Record(CPI_INFO_TOPIC, cpiMetadata.cpiId.toAvro(), null)))
    }

    /** Synchronous publish */
    @Suppress("ForbiddenComment")
    private fun publish(records: List<Record<CpiIdentifierAvro, CpiMetadataAvro>>) {
        if (publisher == null) {
            log.error("Cpi Info Writer publisher is null, not publishing, this error will addressed in a later PR")
            return
        }

        //TODO:  according the publish kdoc, we need to handle failure, retries, and possibly transactions.  Next PR.
        val futures = publisher!!.publish(records)

        // Wait for the future (there should only be one) to complete.
        futures.forEach { it.get() }
    }

    override val isRunning: Boolean
        get() = coordinator.isRunning

    override fun start() {
        log.debug { "Cpi Info Writer Service component starting" }
        coordinator.start()
    }

    override fun stop() {
        log.debug { "Cpi Info Writer Service component stopping" }
        coordinator.stop()
    }

    /** Post a [ConfigChangedEvent]  */
    private fun onConfigChangeEvent(event: ConfigChangedEvent) = coordinator.postEvent(event)

    /**
     * Once we finally get a config, we can create a publisher connected to the
     * correct Kafka instance, and flag that we're up.
     */
    private fun onConfig(coordinator: LifecycleCoordinator, config: SmartConfig) {
        coordinator.updateStatus(LifecycleStatus.DOWN)
        publisher?.close()
        publisher = publisherFactory.createPublisher(PublisherConfig(CLIENT_ID), config)
        coordinator.updateStatus(LifecycleStatus.UP)
    }
}
