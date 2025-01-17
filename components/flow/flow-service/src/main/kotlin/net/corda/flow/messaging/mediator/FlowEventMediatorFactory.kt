package net.corda.flow.messaging.mediator

import net.corda.data.flow.event.FlowEvent
import net.corda.data.flow.state.checkpoint.Checkpoint
import net.corda.libs.configuration.SmartConfig
import net.corda.messaging.api.mediator.MultiSourceEventMediator

/**
 * Creates a Multi-Source Event Mediator for flow engine.
 */
interface FlowEventMediatorFactory {
    /**
     * Creates a Multi-Source Event Mediator for flow engine.
     *
     * @param configs Map of configurations (keys are API defined configuration keys).
     * @param messagingConfig Messaging configuration.
     * @see net.corda.schema.configuration.ConfigKeys
     */
    fun create(
        configs: Map<String, SmartConfig>,
        messagingConfig: SmartConfig,
    ): MultiSourceEventMediator<String, Checkpoint, FlowEvent>

}