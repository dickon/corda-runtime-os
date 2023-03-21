package net.corda.applications.workers.packagingtest.contractverification

import net.corda.e2etest.utilities.ClusterAInfo
import net.corda.e2etest.utilities.ClusterBInfo
import net.corda.e2etest.utilities.ClusterInfo
import net.corda.e2etest.utilities.GROUP_ID
import net.corda.e2etest.utilities.MultiClusterNode
import net.corda.e2etest.utilities.RPC_FLOW_STATUS_SUCCESS
import net.corda.e2etest.utilities.conditionallyUploadCordaPackage
import net.corda.e2etest.utilities.registerStaticMember
import net.corda.e2etest.utilities.testRunUniqueId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path

@TestInstance(Lifecycle.PER_CLASS)
class ContractVerificationTests {
    @TempDir
    lateinit var tempDir: Path

    companion object {
        const val TEST_CPI_NAME = "packaging-verification-app-v1"
        const val TEST_CPB_LOCATION = "/META-INF/packaging-verification-app-v1.cpb"

        private val cpiName = "${TEST_CPI_NAME}_${testRunUniqueId}"

        private val aliceX500 = "CN=Alice-$testRunUniqueId, OU=Application, O=R3, L=London, C=GB"
        private val bobX500 = "CN=Bob-$testRunUniqueId, OU=Application, O=R3, L=London, C=GB"
        private val notaryX500 = "CN=Notary-$testRunUniqueId, OU=Application, O=R3, L=London, C=GB"

        private val statesValueList = listOf(10, 20)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
        }
    }

    object ClusterAInfo {
        object rest {
            val uri = URI("https://localhost:8888")
        }
    }

    object ClusterBInfo {
        object rest {
            val uri = URI("https://localhost:8888")
        }
    }
    private val staticMemberList = listOf(
        aliceX500,
        bobX500,
        notaryX500
    )

    @Test
    fun `contract verification across two clusters succeeds`() {
//        val multiClusterHelper = MultiClusterHelper(tempDir, mutualTls = false)

        // Clusters A and B have the same CPI uploaded, this is the happy flow test

        val clusterA = MultiClusterNode(ClusterAInfo.rest.uri)
        //clusterA.conditionallyUploadCordaPackage(cpiName, TEST_CPB_LOCATION, multiClusterHelper.memberGroupPolicy)
        conditionallyUploadCordaPackage(
            cpiName, TEST_CPB_LOCATION, GROUP_ID, staticMemberList
        )
        val clusterB = MultiClusterNode(ClusterBInfo.rest.uri)
        //clusterB.conditionallyUploadCordaPackage(cpiName, TEST_CPB_LOCATION, multiClusterHelper.memberGroupPolicy)

        val aliceHoldingId = clusterA.getOrCreateVirtualNodeFor(aliceX500, cpiName)
        val bobHoldingId = clusterB.getOrCreateVirtualNodeFor(bobX500, cpiName)
        val nholdinId = clusterB.getOrCreateVirtualNodeFor(notaryX500, cpiName)

        // TODO remove
        registerStaticMember(aliceHoldingId)
        registerStaticMember(bobHoldingId)
        registerStaticMember(nholdinId, true)

        // Mint states as Bob the issuer, checks that Bob's v1 contract is verified correctly

        val mintFlowId = clusterB.startRpcFlow(
            bobHoldingId,
            mapOf("stateValues" to statesValueList),
            "net.cordapp.testing.packagingverification.MintFlow"
        )
        val mintFlowResult = clusterB.awaitRpcFlowFinished(bobHoldingId, mintFlowId)
        assertThat(mintFlowResult.flowStatus).isEqualTo(RPC_FLOW_STATUS_SUCCESS)

        // Get states for Bob

        val bobInitialStatesFlowId = clusterB.startRpcFlow(
            bobHoldingId,
            emptyMap(),
            "net.cordapp.testing.packagingverification.ReportStatesFlow"
        )
        val bobInitialStatesResult = clusterB.awaitRpcFlowFinished(bobHoldingId, bobInitialStatesFlowId)
        assertThat(bobInitialStatesResult.flowStatus).isEqualTo(RPC_FLOW_STATUS_SUCCESS)

        assertThat(bobInitialStatesResult.flowResult).isEqualTo(statesValueList.sum().toString())

        // Transaction from Bob to Alice
        // Alice has the same CPI as Bob as this is the happy flow, so this should go through ok

        val transferStatesFlowId = clusterB.startRpcFlow(
            bobHoldingId,
            mapOf("recipientX500Name" to aliceX500, "value" to statesValueList[0]),
            "net.cordapp.testing.packagingverification.TransferStatesFlow"
        )
        val transferStatesResult = clusterB.awaitRpcFlowFinished(bobHoldingId, transferStatesFlowId)
        assertThat(transferStatesResult.flowStatus).isEqualTo(RPC_FLOW_STATUS_SUCCESS)

        // Get states for Alice

        val aliceGetStatesFlowId = clusterA.startRpcFlow(
            aliceHoldingId,
            emptyMap(),
            "net.cordapp.testing.packagingverification.ReportStatesFlow"
        )
        val aliceStatesResult = clusterA.awaitRpcFlowFinished(aliceHoldingId, aliceGetStatesFlowId)
        assertThat(aliceStatesResult.flowStatus).isEqualTo(RPC_FLOW_STATUS_SUCCESS)

        assertThat(aliceStatesResult.flowResult).isEqualTo(statesValueList[0].toString())

        // Get states for Bob

        val bobNewStatesFlowId = clusterB.startRpcFlow(
            bobHoldingId,
            emptyMap(),
            "net.cordapp.testing.packagingverification.ReportStatesFlow"
        )
        val bobNewStatesResult = clusterB.awaitRpcFlowFinished(bobHoldingId, bobNewStatesFlowId)
        assertThat(bobNewStatesResult.flowStatus).isEqualTo(RPC_FLOW_STATUS_SUCCESS)

        assertThat(bobNewStatesResult.flowResult).isEqualTo(statesValueList.subList(1, statesValueList.size).sum().toString())
    }
}
