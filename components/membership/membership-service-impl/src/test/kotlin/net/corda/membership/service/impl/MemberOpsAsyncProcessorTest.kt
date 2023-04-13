package net.corda.membership.service.impl

import net.corda.crypto.core.ShortHash
import net.corda.data.KeyValuePair
import net.corda.data.KeyValuePairList
import net.corda.data.membership.async.request.MembershipAsyncRequest
import net.corda.data.membership.async.request.MembershipAsyncRequestState
import net.corda.data.membership.async.request.RegistrationAsyncRequest
import net.corda.data.membership.async.request.RetriableFailure
import net.corda.data.membership.async.request.SentToMgmWaitingForNetwork
import net.corda.data.membership.common.RegistrationRequestDetails
import net.corda.data.membership.common.RegistrationStatus
import net.corda.libs.configuration.SmartConfig
import net.corda.membership.persistence.client.MembershipPersistenceClient
import net.corda.membership.persistence.client.MembershipPersistenceOperation
import net.corda.membership.persistence.client.MembershipQueryClient
import net.corda.membership.persistence.client.MembershipQueryResult
import net.corda.membership.registration.InvalidMembershipRegistrationException
import net.corda.membership.registration.NotReadyMembershipRegistrationException
import net.corda.membership.registration.RegistrationProxy
import net.corda.messaging.api.processor.StateAndEventProcessor
import net.corda.messaging.api.records.Record
import net.corda.schema.configuration.MembershipConfig.TtlsConfig.TTLS
import net.corda.schema.configuration.MembershipConfig.TtlsConfig.WAIT_FOR_MGM_SESSION
import net.corda.test.util.time.TestClock
import net.corda.virtualnode.HoldingIdentity
import net.corda.virtualnode.VirtualNodeInfo
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class MemberOpsAsyncProcessorTest {
    private companion object {
        const val FAILURE_REASON = "oops"
        const val SERIAL = 1L
    }

    private val shortHash = ShortHash.of("123123123123")
    private val identity = mock<HoldingIdentity>()
    private val info = mock<VirtualNodeInfo> {
        on { holdingIdentity } doReturn identity
    }
    private val registerCommands = listOf(
        Record(
            "topic1",
            "key1",
            551
        )
    )
    private val registrationProxy = mock<RegistrationProxy> {
        on { register(any(), any(), any()) } doReturn registerCommands
    }
    private val virtualNodeInfoReadService = mock<VirtualNodeInfoReadService> {
        on { getByHoldingIdentityShortHash(shortHash) } doReturn info
    }
    private val setStatusCommands = listOf(
        Record(
            "topic2",
            "key2",
            552
        )
    )
    private val operation = mock<MembershipPersistenceOperation<Unit>> {
        on { createAsyncCommands() } doReturn setStatusCommands
    }
    private val membershipPersistenceClient = mock<MembershipPersistenceClient> {
        on {
            setRegistrationRequestStatus(
                any(),
                any(),
                any(),
                anyOrNull()
            )
        } doReturn operation
    }
    private val membershipQueryClient = mock<MembershipQueryClient> {
        on {
            queryRegistrationRequest(
                any(),
                any(),
            )
        } doReturn MembershipQueryResult.Success(null)
    }
    private val clock = TestClock(
        Instant.ofEpochMilli(4000)
    )
    private val membershipConfig =  mock<SmartConfig> {
        on { getLong("$TTLS.$WAIT_FOR_MGM_SESSION") } doReturn 10
    }

    private val processor = MemberOpsAsyncProcessor(
        registrationProxy,
        virtualNodeInfoReadService,
        membershipPersistenceClient,
        membershipQueryClient,
        membershipConfig,
        clock,
    )

    @Nested
    inner class OnNextTests {
        @Test
        fun `it with successful registration will keep the state to check if P2P passed`() {
            val id = UUID(0, 1)
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = MembershipAsyncRequestState(
                        command,
                        SentToMgmWaitingForNetwork(clock.instant().plusSeconds(10 * 60)),
                    ),
                    markForDLQ = false,
                    responseEvents = registerCommands,
                )

            )
        }

        @Test
        fun `it with successful registration will keep waiting for P2P communication`() {
            val id = UUID(0, 1)
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            val state = MembershipAsyncRequestState(
                command,
                SentToMgmWaitingForNetwork(clock.instant().plusSeconds(7 * 60)),
            )
            val reply = processor.onNext(
                state,
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = state,
                    markForDLQ = false,
                    responseEvents = registerCommands,
                )
            )
        }

        @Test
        fun `it will stop waiting for P2P communication after a while`() {
            val id = UUID(0, 1)
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            val state = MembershipAsyncRequestState(
                command,
                SentToMgmWaitingForNetwork(clock.instant().minusSeconds(5)),
            )
            val reply = processor.onNext(
                state,
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = null,
                    markForDLQ = true,
                    responseEvents = setStatusCommands,
                )
            )
        }

        @Test
        fun `it with a command that can not be replayed will raise the DLQ flag`() {
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(null),
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = null,
                    responseEvents = emptyList(),
                    markForDLQ = true,
                )
            )
        }

        @Test
        fun `it with a command that can be replayed will create a state`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(NotReadyMembershipRegistrationException(FAILURE_REASON))
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )

            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = MembershipAsyncRequestState(
                        command,
                        RetriableFailure(9),
                    ),
                    responseEvents = emptyList(),
                    markForDLQ = false,
                )
            )
        }

        @Test
        fun `it should decrement the retry count if it fails again`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(NotReadyMembershipRegistrationException(FAILURE_REASON))
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )

            val reply = processor.onNext(
                MembershipAsyncRequestState(
                    command,
                    RetriableFailure(6),
                ),
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = MembershipAsyncRequestState(
                        command,
                        RetriableFailure(5),
                    ),
                    responseEvents = emptyList(),
                    markForDLQ = false,
                )
            )
        }

        @Test
        fun `it should not retry if it tried too many times`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(NotReadyMembershipRegistrationException(FAILURE_REASON))
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )

            val reply = processor.onNext(
                MembershipAsyncRequestState(
                    command,
                    RetriableFailure(0),
                ),
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply).isEqualTo(
                StateAndEventProcessor.Response(
                    updatedState = null,
                    responseEvents = setStatusCommands,
                    markForDLQ = true,
                )
            )
        }

        @Test
        fun `it should persist invalid status if it tried too many times`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(NotReadyMembershipRegistrationException(FAILURE_REASON))
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )

            processor.onNext(
                MembershipAsyncRequestState(
                    command,
                    RetriableFailure(0),
                ),
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            verify(membershipPersistenceClient).setRegistrationRequestStatus(
                identity,
                id.toString(),
                RegistrationStatus.FAILED,
                FAILURE_REASON,
            )
        }
    }

    @Nested
    inner class RegisterTest {
        @Test
        fun `it should call the registration proxy`() {
            val id = UUID(0, 1)
            processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            verify(registrationProxy).register(id, identity, mapOf("key" to "value"))
        }

        @Test
        fun `it should retry if the node was not loaded yet`() {
            val id = UUID(0, 1)
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            "223123123123",
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            assertThat(reply.updatedState).isNotNull
        }


        @Test
        fun `it should not retry if the node was not loaded for too long`() {
            val id = UUID(0, 1)
            val command = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    "223123123123",
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            val state = MembershipAsyncRequestState(
                command,
                RetriableFailure(0),
            )
            val reply = processor.onNext(
                state,
                Record(
                    "topic",
                    "key",
                    command,
                )
            )

            assertThat(reply.updatedState).isNull()
        }

        @Test
        fun `it should not retry if the request id is not UUID`() {
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            "nop",
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            assertThat(reply.markForDLQ).isTrue
        }

        @Test
        fun `it should retry if the current status is not available`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Failure(
                    FAILURE_REASON
                )
            )
            val id = UUID(0, 1)
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            assertThat(reply.updatedState).isNotNull
        }

        @Test
        fun `it should retry do nothing if the status is not new any more`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    RegistrationRequestDetails(
                        Instant.MIN,
                        Instant.MIN,
                        RegistrationStatus.SENT_TO_MGM,
                        "",
                        0,
                        mock(),
                        mock(),
                        mock(),
                        null,
                        SERIAL,
                    )
                )
            )
            val id = UUID(0, 1)
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            assertThat(reply.updatedState).isNull()
            assertThat(reply.markForDLQ).isFalse
            verifyNoInteractions(registrationProxy)
        }

        @Test
        fun `it should register if the status is new`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    RegistrationRequestDetails(
                        Instant.MIN,
                        Instant.MIN,
                        RegistrationStatus.NEW,
                        "",
                        0,
                        mock(),
                        mock(),
                        mock(),
                        null,
                        SERIAL,
                    )
                )
            )
            val id = UUID(0, 1)
            processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            verify(registrationProxy).register(id, identity, mapOf("key" to "value"))
        }

        @Test
        fun `it should not retry if status is no longer sent to MGM`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    RegistrationRequestDetails(
                        Instant.MIN,
                        Instant.MIN,
                        RegistrationStatus.RECEIVED_BY_MGM,
                        "",
                        0,
                        mock(),
                        mock(),
                        mock(),
                        null,
                        SERIAL,
                    ),
                )
            )
            val id = UUID(0, 1)
            val request = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            processor.onNext(
                MembershipAsyncRequestState(
                    request,
                    SentToMgmWaitingForNetwork(clock.instant().plusSeconds(4)),
                ),
                Record(
                    "topic",
                    "key",
                    request,
                )
            )

            verify(registrationProxy, never()).register(any(), any(), any())
        }

        @Test
        fun `it should retry if status is stuck in sent to MGM`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    RegistrationRequestDetails(
                        Instant.MIN,
                        Instant.MIN,
                        RegistrationStatus.NEW,
                        "",
                        0,
                        mock(),
                        mock(),
                        mock(),
                        null,
                        SERIAL,
                    )
                )
            )
            val id = UUID(0, 1)
            val request = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            processor.onNext(
                MembershipAsyncRequestState(
                    request,
                    null,
                ),
                Record(
                    "topic",
                    "key",
                    request,
                )
            )

            verify(registrationProxy).register(id, identity, mapOf("key" to "value"))
        }

        @Test
        fun `it should retry if status is stuck in new`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    RegistrationRequestDetails(
                        Instant.MIN,
                        Instant.MIN,
                        RegistrationStatus.NEW,
                        "",
                        0,
                        mock(),
                        mock(),
                        mock(),
                        null,
                        SERIAL,
                    )
                )
            )
            val id = UUID(0, 1)
            val request = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            processor.onNext(
                MembershipAsyncRequestState(
                    request,
                    null,
                ),
                Record(
                    "topic",
                    "key",
                    request,
                )
            )

            verify(registrationProxy).register(id, identity, mapOf("key" to "value"))
        }

        @Test
        fun `it should retry if status was not persisted`() {
            whenever(membershipQueryClient.queryRegistrationRequest(any(), any())).doReturn(
                MembershipQueryResult.Success(
                    null
                )
            )
            val id = UUID(0, 1)
            val request = MembershipAsyncRequest(
                RegistrationAsyncRequest(
                    shortHash.value,
                    id.toString(),
                    KeyValuePairList(
                        listOf(
                            KeyValuePair(
                                "key",
                                "value"
                            )
                        )
                    )
                )
            )
            processor.onNext(
                MembershipAsyncRequestState(
                    request,
                    null,
                ),
                Record(
                    "topic",
                    "key",
                    request,
                )
            )

            verify(registrationProxy).register(id, identity, mapOf("key" to "value"))
        }

        @Test
        fun `it should not retry if the request id invalid`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(InvalidMembershipRegistrationException(FAILURE_REASON))
            val reply = processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            assertThat(reply.markForDLQ).isTrue
        }

        @Test
        fun `it should persist invalid status if the request is invalid`() {
            val id = UUID(0, 1)
            whenever(registrationProxy.register(any(), any(), any()))
                .doThrow(InvalidMembershipRegistrationException(FAILURE_REASON))
            processor.onNext(
                null,
                Record(
                    "topic",
                    "key",
                    MembershipAsyncRequest(
                        RegistrationAsyncRequest(
                            shortHash.value,
                            id.toString(),
                            KeyValuePairList(
                                listOf(
                                    KeyValuePair(
                                        "key",
                                        "value"
                                    )
                                )
                            )
                        )
                    ),
                )
            )

            verify(membershipPersistenceClient).setRegistrationRequestStatus(
                identity,
                id.toString(),
                RegistrationStatus.INVALID,
                FAILURE_REASON,
            )
        }
    }
}
