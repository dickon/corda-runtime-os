package net.corda.testing.driver.sandbox

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import net.corda.crypto.core.ShortHash
import net.corda.libs.packaging.Cpi
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.reconciliation.VersionedRecord
import net.corda.testing.driver.DriverConstants.DRIVER_SERVICE
import net.corda.testing.driver.DriverConstants.DRIVER_SERVICE_RANKING
import net.corda.virtualnode.HoldingIdentity
import net.corda.virtualnode.VirtualNodeInfo
import net.corda.virtualnode.read.VirtualNodeInfoListener
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.propertytypes.ServiceRanking
import org.slf4j.LoggerFactory

interface VirtualNodeLoader {
    companion object {
        const val VNODE_LOADER_NAME = "net.corda.testing.driver.sandbox.VirtualNodeLoader"
    }

    fun loadVirtualNode(resourceName: String, holdingIdentity: HoldingIdentity): VirtualNodeInfo
    fun unloadVirtualNode(virtualNodeInfo: VirtualNodeInfo)
    fun forgetCPI(id: CpiIdentifier)
}

@Suppress("unused")
@Component(
    name = VirtualNodeLoader.VNODE_LOADER_NAME,
    service = [ VirtualNodeLoader::class, VirtualNodeInfoReadService::class ],
    property = [ DRIVER_SERVICE ]
)
@ServiceRanking(DRIVER_SERVICE_RANKING)
class VirtualNodeLoaderImpl @Activate constructor(
    @Reference
    private val cpiLoader: CpiLoader,
) : VirtualNodeLoader, VirtualNodeInfoReadService {
    private val virtualNodeInfoMap = ConcurrentHashMap<HoldingIdentity, VirtualNodeInfo>()
    private val resourcesLookup = mutableMapOf<CpiIdentifier, String>()
    private val cpiResources = mutableMapOf<String, Cpi>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val isRunning: Boolean
        get() = true

    override fun loadVirtualNode(resourceName: String, holdingIdentity: HoldingIdentity): VirtualNodeInfo {
        val cpi = cpiResources.computeIfAbsent(resourceName) { key ->
            cpiLoader.loadCPI(key).also { cpi ->
                resourcesLookup[cpi.metadata.cpiId] = key
            }
        }
        return VirtualNodeInfo(
            holdingIdentity,
            CpiIdentifier(
                cpi.metadata.cpiId.name,
                cpi.metadata.cpiId.version,
                cpi.metadata.cpiId.signerSummaryHash
            ),
            null,
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            null,
            timestamp = Instant.now(),
        ).also(::put)
    }

    override fun unloadVirtualNode(virtualNodeInfo: VirtualNodeInfo) {
        virtualNodeInfoMap.remove(virtualNodeInfo.holdingIdentity)
    }

    override fun forgetCPI(id: CpiIdentifier) {
        resourcesLookup.remove(id)?.also(cpiResources::remove)
    }

    override fun getAll(): List<VirtualNodeInfo> {
        return virtualNodeInfoMap.values.toList()
    }

    override fun get(holdingIdentity: HoldingIdentity): VirtualNodeInfo? {
        return virtualNodeInfoMap[holdingIdentity]
    }

    override fun getByHoldingIdentityShortHash(holdingIdentityShortHash: ShortHash): VirtualNodeInfo? {
        TODO("Not yet implemented - getByHoldingIdentityShortHash")
    }

    private fun put(virtualNodeInfo: VirtualNodeInfo) {
        if (virtualNodeInfoMap.putIfAbsent(virtualNodeInfo.holdingIdentity, virtualNodeInfo) != null) {
            throw IllegalStateException("Virtual node $virtualNodeInfo already exists.")
        }
    }

    override fun registerCallback(listener: VirtualNodeInfoListener): AutoCloseable {
        return AutoCloseable {}
    }

    override fun getAllVersionedRecords(): Stream<VersionedRecord<HoldingIdentity, VirtualNodeInfo>>? {
        TODO("Not yet implemented - getAllVersionedRecords")
    }

    override val lifecycleCoordinatorName: LifecycleCoordinatorName
        get() = LifecycleCoordinatorName(VirtualNodeLoaderImpl::class.java.simpleName)

    override fun start() {
        logger.info("Started")
    }

    override fun stop() {
        logger.info("Stopped")
    }
}