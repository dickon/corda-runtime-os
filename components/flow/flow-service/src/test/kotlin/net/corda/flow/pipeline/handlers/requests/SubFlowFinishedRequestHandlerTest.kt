package net.corda.flow.pipeline.handlers.requests

import net.corda.data.flow.event.FlowEvent
import net.corda.data.flow.event.Wakeup
import net.corda.data.flow.state.checkpoint.FlowStackItem
import net.corda.data.flow.state.checkpoint.FlowStackItemSession
import net.corda.data.flow.state.session.SessionStateType
import net.corda.data.flow.state.waiting.SessionConfirmation
import net.corda.data.flow.state.waiting.SessionConfirmationType
import net.corda.flow.RequestHandlerTestContext
import net.corda.flow.fiber.FlowIORequest
import net.corda.flow.pipeline.exceptions.FlowFatalException
import net.corda.flow.pipeline.sessions.FlowSessionStateException
import net.corda.flow.utils.mutableKeyValuePairList
import net.corda.messaging.api.records.Record
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@Suppress("MaxLineLength")
class SubFlowFinishedRequestHandlerTest {

    private companion object {
        const val FLOW_NAME = "flow name"
        const val SESSION_ID_1 = "s1"
        const val SESSION_ID_2 = "s2"
        const val SESSION_ID_3 = "s3"
        val SESSION_IDS = listOf(SESSION_ID_1, SESSION_ID_2, SESSION_ID_3)
        val SESSIONS = SESSION_IDS.map { FlowStackItemSession(it, true) }

        @JvmStatic
        fun isInitiatingFlow(): Stream<Arguments> {
            return Stream.of(Arguments.of(true), Arguments.of(false))
        }
    }

    private val record = Record("", "", FlowEvent())
    private val testContext = RequestHandlerTestContext(Any())
    private val flowSessionManager = testContext.flowSessionManager
    private val handler = SubFlowFinishedRequestHandler(
        testContext.flowRecordFactory,
        testContext.closeSessionService
    )

    private fun createFlowStackItem(isInitiatingFlow: Boolean, sessions: List<FlowStackItemSession> = SESSIONS) =
        FlowStackItem.newBuilder()
            .setFlowName(FLOW_NAME)
            .setIsInitiatingFlow(isInitiatingFlow)
            .setSessions(sessions)
            .setContextPlatformProperties(mutableKeyValuePairList())
            .setContextUserProperties(mutableKeyValuePairList())
            .build()

    @BeforeEach
    fun setup() {
        whenever(
            testContext
                .flowRecordFactory
                .createFlowEventRecord(eq(testContext.flowId), any<Wakeup>())
        ).thenReturn(record)
    }

    @ParameterizedTest(name = "Returns an updated WaitingFor of SessionConfirmation (Close) when the flow has sessions to close (isInitiatingFlow={0})")
    @MethodSource("isInitiatingFlow")
    fun `Returns an updated WaitingFor of SessionConfirmation (Close) when the flow has sessions to close`(
        isInitiatingFlow: Boolean
    ) {
        whenever(testContext.closeSessionService.getSessionsToCloseForWaitingFor(testContext.flowCheckpoint, SESSION_IDS))
            .thenReturn(SESSION_IDS)

        val result = handler.getUpdatedWaitingFor(
            testContext.flowEventContext,
            FlowIORequest.SubFlowFinished(createFlowStackItem(isInitiatingFlow).sessions.map { it.sessionId })
        )
        assertEquals(SessionConfirmation(SESSION_IDS, SessionConfirmationType.CLOSE), result.value)
    }

    @ParameterizedTest(name = "Returns an updated WaitingFor of Wakeup when the flow has no sessions to close (isInitiatingFlow={0})")
    @MethodSource("isInitiatingFlow")
    fun `Returns an updated WaitingFor of Wakeup when the flow  has no sessions to close`(isInitiatingFlow: Boolean) {
        val result = handler.getUpdatedWaitingFor(
            testContext.flowEventContext,
            FlowIORequest.SubFlowFinished(createFlowStackItem(isInitiatingFlow, emptyList()).sessions.map { it.sessionId })
        )
        assertEquals(net.corda.data.flow.state.waiting.Wakeup(), result.value)
    }

    @Test
    fun `Throws exception when updating WaitingFor when session does not exist within checkpoint`() {
        whenever(testContext.closeSessionService.getSessionsToCloseForWaitingFor(testContext.flowCheckpoint, SESSION_IDS)
        ).thenThrow(FlowSessionStateException("Session does not exist"))

        assertThrows<FlowFatalException> {
            handler.getUpdatedWaitingFor(
                testContext.flowEventContext,
                FlowIORequest.SubFlowFinished(createFlowStackItem(true).sessions.map { it.sessionId })
            )
        }
    }

    @ParameterizedTest(name = "Sends session close messages to non-errored sessions and does not create a Wakeup record when the flow has sessions to close (isInitiatingFlow={0})")
    @MethodSource("isInitiatingFlow")
    fun `Sends session close messages to non-errored sessions and does not create a Wakeup record when the flow has sessions to close`(
        isInitiatingFlow: Boolean
    ) {
        val nonErroredSessions = listOf(SESSION_ID_2, SESSION_ID_3)

        whenever(
            flowSessionManager.doAllSessionsHaveStatus(
                testContext.flowCheckpoint,
                nonErroredSessions,
                SessionStateType.CLOSED
            )
        ).thenReturn(false)

        val outputContext = handler.postProcess(
            testContext.flowEventContext,
            FlowIORequest.SubFlowFinished(createFlowStackItem(isInitiatingFlow).sessions.map { it.sessionId })
        )
        verify(testContext.closeSessionService).handleCloseForSessions(SESSION_IDS, testContext.flowCheckpoint)
        verify(testContext.flowRecordFactory, never()).createFlowEventRecord(eq(testContext.flowId), any<Wakeup>())
        assertThat(outputContext.outputRecords).hasSize(0)
    }

    @ParameterizedTest(name = "Does not send session close messages and creates a Wakeup record when the flow has no sessions to close (isInitiatingFlow={0})")
    @MethodSource("isInitiatingFlow")
    fun `Does not send session close messages and creates a Wakeup record when the flow has no sessions to close`(
        isInitiatingFlow: Boolean
    ) {
        whenever(
            flowSessionManager.doAllSessionsHaveStatus(
                testContext.flowCheckpoint,
                emptyList(),
                SessionStateType.CLOSED
            )
        ).thenReturn(true)

        val outputContext = handler.postProcess(
            testContext.flowEventContext,
            FlowIORequest.SubFlowFinished(createFlowStackItem(isInitiatingFlow, emptyList()).sessions.map { it.sessionId })
        )
        verify(testContext.flowRecordFactory).createFlowEventRecord(eq(testContext.flowId), any<Wakeup>())
        assertThat(outputContext.outputRecords).containsOnly(record)
    }

    @ParameterizedTest(name = "Does not send session close messages and creates a Wakeup record when the flow has only closed sessions (isInitiatingFlow={0})")
    @MethodSource("isInitiatingFlow")
    fun `Does not send session close messages and creates a Wakeup record when the flow has only closed sessions`(
        isInitiatingFlow: Boolean
    ) {
        whenever(
            flowSessionManager.doAllSessionsHaveStatus(
                testContext.flowCheckpoint,
                SESSION_IDS,
                SessionStateType.CLOSED
            )
        ).thenReturn(true)

        val outputContext = handler.postProcess(
            testContext.flowEventContext,
            FlowIORequest.SubFlowFinished(createFlowStackItem(isInitiatingFlow).sessions.map { it.sessionId })
        )

        verify(testContext.closeSessionService).handleCloseForSessions(SESSION_IDS, testContext.flowCheckpoint)
        verify(testContext.flowRecordFactory).createFlowEventRecord(eq(testContext.flowId), any<Wakeup>())
        assertThat(outputContext.outputRecords).containsOnly(record)
    }

    @Test
    fun `Throws exception when session does not exist within checkpoint`() {
        whenever(testContext.closeSessionService.handleCloseForSessions(SESSION_IDS, testContext.flowCheckpoint)
        ).thenThrow(FlowSessionStateException("Session does not exist"))

        assertThrows<FlowFatalException> {
            handler.postProcess(
                testContext.flowEventContext,
                FlowIORequest.SubFlowFinished(createFlowStackItem(true).sessions.map { it.sessionId })
            )
        }
    }
}