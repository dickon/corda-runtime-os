package net.corda.flow.mapper.impl.executor

import net.corda.data.ExceptionEnvelope
import net.corda.data.flow.event.MessageDirection
import net.corda.data.flow.event.SessionEvent
import net.corda.data.flow.event.session.SessionAck
import net.corda.data.flow.event.session.SessionClose
import net.corda.data.flow.event.session.SessionError
import net.corda.data.flow.state.mapper.FlowMapperState
import net.corda.data.flow.state.mapper.FlowMapperStateType
import net.corda.flow.mapper.FlowMapperResult
import net.corda.flow.mapper.executor.FlowMapperEventExecutor
import net.corda.flow.mapper.factory.RecordFactory
import net.corda.libs.configuration.SmartConfig
import org.slf4j.LoggerFactory
import java.time.Instant

@Suppress("LongParameterList")
class SessionEventExecutor(
    private val eventKey: String,
    private val sessionEvent: SessionEvent,
    private val flowMapperState: FlowMapperState?,
    private val instant: Instant,
    private val flowConfig: SmartConfig,
    private val recordFactory: RecordFactory
    ) : FlowMapperEventExecutor {

    private companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }
    private val messageDirection = sessionEvent.messageDirection

    override fun execute(): FlowMapperResult {
        return if (flowMapperState == null) {
            handleNullState()
        } else {
            processOtherSessionEvents(flowMapperState, instant)
        }
    }

    private fun handleNullState(): FlowMapperResult {
        val sessionId = sessionEvent.sessionId
        val eventPayload = sessionEvent.payload

        val errEvent = SessionEvent(
            MessageDirection.INBOUND,
            instant,
            toggleSessionId(sessionEvent.sessionId),
            null,
            sessionEvent.initiatingIdentity,
            sessionEvent.initiatedIdentity,
            0,
            emptyList(),
            SessionError(
                ExceptionEnvelope(
                    "FlowMapper-SessionExpired",
                    "Tried to process session event for expired session with sessionId $sessionId"
                )
            )
        )

        return if (eventPayload !is SessionError) {
            log.warn(
                "Flow mapper received session event for session which does not exist. Session may have expired. Returning error to " +
                        "counterparty. Key: $eventKey, Event: class ${sessionEvent.payload::class.java}, $sessionEvent"
            )
            FlowMapperResult(
                null, listOf(
                    recordFactory.makeRecord(eventKey, sessionEvent, flowMapperState, instant, flowConfig, errEvent)
                )
            )
        } else {
            log.warn(
                "Flow mapper received error event from counterparty for session which does not exist. Session may have expired. " +
                        "Ignoring event. Key: $eventKey, Event: $sessionEvent"
            )
            FlowMapperResult(null, listOf())
        }
    }

    /**
     * Output the session event to the correct topic and key
     */
    private fun processOtherSessionEvents(flowMapperState: FlowMapperState, instant: Instant): FlowMapperResult {
        val ackEvent = SessionEvent(
            MessageDirection.INBOUND,
            instant,
            toggleSessionId(sessionEvent.sessionId),
            null,
            sessionEvent.initiatingIdentity,
            sessionEvent.initiatedIdentity,
            sessionEvent.sequenceNum,
            emptyList(),
            SessionAck()
        )

        val errorMsg = "Flow mapper received error event from counterparty for session which does not exist. " +
                "Session may have expired. Key: $eventKey, Event: $sessionEvent. "

        return when (flowMapperState.status) {
            null -> {
                log.warn("FlowMapperState with null status. Key: $eventKey, Event: $sessionEvent.")
                FlowMapperResult(null, listOf())
            }
            FlowMapperStateType.CLOSING -> {
                if (messageDirection == MessageDirection.OUTBOUND) {
                    log.warn("Attempted to send a message but flow mapper state is in CLOSING. Session ID: ${sessionEvent.sessionId}")
                    FlowMapperResult(flowMapperState, listOf())
                } else {
                    if (sessionEvent.payload is SessionClose) {
                        FlowMapperResult(
                            flowMapperState, listOf(
                                recordFactory.makeRecord(eventKey, sessionEvent, flowMapperState, instant, flowConfig, ackEvent)
                            )
                        )
                    } else {
                        FlowMapperResult(flowMapperState, listOf())
                    }
                }
            }

            FlowMapperStateType.OPEN -> {
                val outputRecord = recordFactory.makeRecord(eventKey, sessionEvent, flowMapperState, instant, flowConfig, sessionEvent)
                FlowMapperResult(flowMapperState, listOf(outputRecord))
            }
            FlowMapperStateType.ERROR -> {
                log.warn(errorMsg + "Ignoring event.")
                FlowMapperResult(flowMapperState, listOf())
            }
        }
    }
}
