package net.corda.interop.rest.impl.v1

import net.corda.configuration.read.ConfigurationReadService
import net.corda.interop.group.policy.read.InteropGroupPolicyReadService
import net.corda.interop.identity.registry.InteropIdentityRegistryService
import net.corda.interop.identity.write.InteropIdentityWriteService
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.membership.group.policy.validation.InteropGroupPolicyValidator
import net.corda.membership.read.MembershipGroupReaderProvider
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import net.corda.libs.interop.endpoints.v1.types.CreateInteropIdentityRest
import net.corda.libs.interop.endpoints.v1.types.GroupPolicy
import net.corda.libs.interop.endpoints.v1.types.P2pParameters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class InteropRestResourceTest {

    private val coordinatorFactory: LifecycleCoordinatorFactory = mock {}

    private val configurationReadService: ConfigurationReadService = mock {}

    private val interopIdentityRegistryService: InteropIdentityRegistryService = mock {}

    private val interopIdentityWriteService: InteropIdentityWriteService = mock {}

    private val virtualNodeInfoReadService: VirtualNodeInfoReadService = mock {
        on { getByHoldingIdentityShortHash(any()) } doReturn null
    }

    private val interopGroupPolicyReadService: InteropGroupPolicyReadService = mock {}

    private val interopGroupPolicyValidator: InteropGroupPolicyValidator = mock {}

    private val membershipGroupReaderProvider: MembershipGroupReaderProvider = mock {}

    @Test
    fun test1() {
        val me = InteropRestResourceImpl(
            coordinatorFactory,
            configurationReadService,
            interopIdentityRegistryService,
            interopIdentityWriteService,
            virtualNodeInfoReadService,
            interopGroupPolicyReadService,
            interopGroupPolicyValidator,
            membershipGroupReaderProvider
        )

        val createInteropIdentityRestRequest = CreateInteropIdentityRest.Request(
            "app1",
            GroupPolicy(
                1, "", P2pParameters(
                    emptyList(),
                    emptyList(),
                    "sessionPki",
                    "tlsPki",
                    "tlsVersion",
                    "protocolMode",
                    "tlsType"
                )
            )
        )
        val holdingIdentityShortHash = ""

        assertThrows<net.corda.rest.exception.InvalidInputDataException> {
            me.createInterOpIdentity(createInteropIdentityRestRequest, holdingIdentityShortHash)
        }
    }

    @Test
    fun test2() {
        val me = InteropRestResourceImpl(
            coordinatorFactory,
            configurationReadService,
            interopIdentityRegistryService,
            interopIdentityWriteService,
            virtualNodeInfoReadService,
            interopGroupPolicyReadService,
            interopGroupPolicyValidator,
            membershipGroupReaderProvider
        )

        val createInteropIdentityRestRequest = CreateInteropIdentityRest.Request(
            "app1",
            GroupPolicy(
                1, "", P2pParameters(
                    emptyList(),
                    emptyList(),
                    "sessionPki",
                    "tlsPki",
                    "tlsVersion",
                    "protocolMode",
                    "tlsType"
                )
            )
        )
        val holdingIdentityShortHash = "A57CF19885DE"

        assertThrows<net.corda.rest.exception.InvalidInputDataException> {
            me.createInterOpIdentity(createInteropIdentityRestRequest, holdingIdentityShortHash)
        }
    }
}