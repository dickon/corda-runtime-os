package net.corda.flow.application.ledger.external.events

import java.nio.ByteBuffer
import net.corda.data.flow.event.external.ExternalEventContext
import net.corda.data.persistence.ConsensualLedgerRequest
import net.corda.data.persistence.EntityResponse
import net.corda.flow.external.events.factory.ExternalEventFactory
import net.corda.flow.external.events.factory.ExternalEventRecord
import net.corda.flow.state.FlowCheckpoint
import net.corda.schema.Schemas
import net.corda.virtualnode.toAvro
import java.time.Clock

abstract class AbstractLedgerExternalEventFactory<PARAMETERS : Any>(private val clock: Clock = Clock.systemUTC()) :
    ExternalEventFactory<PARAMETERS, EntityResponse, List<ByteBuffer>>
{

    abstract fun createRequest(parameters: PARAMETERS): Any

    override val responseType = EntityResponse::class.java

    override fun createExternalEvent(
        checkpoint: FlowCheckpoint,
        flowExternalEventContext: ExternalEventContext,
        parameters: PARAMETERS
    ): ExternalEventRecord {
        return ExternalEventRecord(
            topic = Schemas.VirtualNode.LEDGER_PERSISTENCE_TOPIC,
            payload = ConsensualLedgerRequest.newBuilder()
                .setTimestamp(clock.instant())
                .setHoldingIdentity(checkpoint.holdingIdentity.toAvro())
                .setRequest(createRequest(parameters))
                .setFlowExternalEventContext(flowExternalEventContext)
                .build()
        )
    }

    override fun resumeWith(checkpoint: FlowCheckpoint, response: EntityResponse): List<ByteBuffer> {
        return response.results
    }
}