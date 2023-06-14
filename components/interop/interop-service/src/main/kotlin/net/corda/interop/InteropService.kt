package net.corda.interop

import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.avro.serialization.CordaAvroSerializationFactory
import net.corda.interop.service.InteropFacadeToFlowMapperService
import net.corda.libs.configuration.helper.getConfig
import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.StartEvent
import net.corda.lifecycle.createCoordinator
import net.corda.membership.read.MembershipGroupReaderProvider
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.StateAndEventSubscription
import net.corda.messaging.api.subscription.config.SubscriptionConfig
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.schema.Schemas
import net.corda.schema.Schemas.Flow.FLOW_INTEROP_EVENT_TOPIC
import net.corda.schema.configuration.ConfigKeys.FLOW_CONFIG
//import net.corda.schema.configuration.ConfigKeys.FLOW_CONFIG
import net.corda.schema.configuration.ConfigKeys.MESSAGING_CONFIG
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@Suppress("LongParameterList")
@Component(service = [InteropService::class], immediate = true)
class InteropService @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = ConfigurationReadService::class)
    private val configurationReadService: ConfigurationReadService,
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory,
    @Reference(service = CordaAvroSerializationFactory::class)
    private val cordaAvroSerializationFactory: CordaAvroSerializationFactory,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory,
    @Reference(service = MembershipGroupReaderProvider::class)
    private val membershipGroupReaderProvider: MembershipGroupReaderProvider,
    @Reference(service = InteropFacadeToFlowMapperService::class)
    private val facadeToFlowMapperService: InteropFacadeToFlowMapperService
) : Lifecycle {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private const val CONSUMER_GROUP = "InteropConsumer"
        private const val SUBSCRIPTION = "SUBSCRIPTION"
        private const val REGISTRATION = "REGISTRATION"
        private const val CONFIG_HANDLE = "CONFIG_HANDLE"
        private const val GROUP_NAME = "interop_alias_translator"
        private const val CLEANUP_TASK = "CLEANUP_TASK"
        private const val INTEROP_ALIAS_IDENTITY_TOPIC = "interop.alias.identity"
    }

    private val coordinator = coordinatorFactory.createCoordinator<InteropService>(::eventHandler)
    private var publisher: Publisher? = null

    private fun eventHandler(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
        logger.info("$event")
        when (event) {
            is StartEvent -> {
                coordinator.createManagedResource(REGISTRATION) {
                    coordinator.followStatusChangesByName(
                        setOf(
                            LifecycleCoordinatorName.forComponent<ConfigurationReadService>()
                        )
                    )
                }
            }
            is RegistrationStatusChangeEvent -> {
                if (event.status == LifecycleStatus.UP) {
                    coordinator.createManagedResource(CONFIG_HANDLE) {
                        configurationReadService.registerComponentForUpdates(
                            coordinator,
                            setOf(FLOW_CONFIG, MESSAGING_CONFIG)
                        )
                    }
                } else {
                    coordinator.closeManagedResources(setOf(CONFIG_HANDLE))
                }
            }
            is ConfigChangedEvent -> {
                restartInteropProcessor(event)
            }
        }
    }

    private fun restartInteropProcessor(event: ConfigChangedEvent) {
        val messagingConfig = event.config.getConfig(MESSAGING_CONFIG)
        val flowConfig = event.config.getConfig(FLOW_CONFIG)
        //TODO temporary code (commented and uncommented) to setup members of interop group,
        // and send seed message in absence of a flow, this will be phased out later on by CORE-10446
        publisher?.close()
        publisher = publisherFactory.createPublisher(
            PublisherConfig("interop-registration-service"),
            messagingConfig
        )
        publisher?.start()

        coordinator.createManagedResource("InteropAliasProcessor.subscription") {
            subscriptionFactory.createCompactedSubscription(
                SubscriptionConfig(GROUP_NAME, Schemas.P2P.P2P_HOSTED_IDENTITIES_TOPIC),
                InteropAliasProcessor(publisher!!),
                messagingConfig).also {
                it.start()
            }
        }

        coordinator.createManagedResource("InteropAliasIdentityProcessor.subscription") {
            subscriptionFactory.createCompactedSubscription(
                SubscriptionConfig(GROUP_NAME, INTEROP_ALIAS_IDENTITY_TOPIC),
                InteropIdentityProcessor(),
                messagingConfig).also {
                it.start()
            }
        }

        coordinator.createManagedResource(CLEANUP_TASK) {
            ScheduledTaskState(
                Executors.newSingleThreadScheduledExecutor(),
                publisherFactory.createPublisher(
                    PublisherConfig("$CONSUMER_GROUP-cleanup-publisher"),
                    messagingConfig
                ),
                mutableMapOf()
            )
        }
        val newScheduledTaskState = coordinator.getManagedResource<ScheduledTaskState>(CLEANUP_TASK)!!
        coordinator.createManagedResource(SUBSCRIPTION) {
            subscriptionFactory.createStateAndEventSubscription(
                SubscriptionConfig(CONSUMER_GROUP, FLOW_INTEROP_EVENT_TOPIC),
                InteropProcessor(
                    cordaAvroSerializationFactory, membershipGroupReaderProvider, facadeToFlowMapperService, flowConfig
                ),
                messagingConfig,
                InteropListener(newScheduledTaskState)
            )
        }
        coordinator.getManagedResource<StateAndEventSubscription<*, *, *>>(SUBSCRIPTION)!!.start()
        coordinator.updateStatus(LifecycleStatus.UP)
    }

    override val isRunning: Boolean
        get() = coordinator.isRunning

    override fun start() {
        coordinator.start()
        membershipGroupReaderProvider.start()
    }

    override fun stop() {
        coordinator.stop()
        membershipGroupReaderProvider.stop()
    }

    @Suppress("unused")
    @Deactivate
    fun close() {
        coordinator.close()
    }
}