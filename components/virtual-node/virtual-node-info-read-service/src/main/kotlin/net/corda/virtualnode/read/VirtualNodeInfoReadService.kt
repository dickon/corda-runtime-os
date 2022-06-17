package net.corda.virtualnode.read

import net.corda.lifecycle.Lifecycle
import net.corda.reconciliation.ReconcilerReader
import net.corda.virtualnode.HoldingIdentity
import net.corda.virtualnode.VirtualNodeInfo

/**
 * Lightweight service to provide information regarding the "virtual nodes" currently
 * available within the system.
 *
 * You will probably use the `get` methods if you're manually interacting with the service,
 * e.g. testing for virtual nodes before creating them via RPC
 *
 * You will probably use the callback if you are automatically interacting with the service,
 * e.g. a status page listening all nodes in the system.
 */
interface VirtualNodeInfoReadService : ReconcilerReader<HoldingIdentity, VirtualNodeInfo>, Lifecycle {

    /**
     * Returns virtual node info for all onboarded virtual nodes
     * without starting any bundles or instantiating any classes.
     *
     * Returns empty list if no virtual nodes are onboarded
     */
    fun getAll(): List<VirtualNodeInfo>

    /**
     * Returns virtual node context information for a given holding identity
     * without starting any bundles or instantiating any classes.
     *
     * Implementation may return null if it is part of a component.
     *
     * Returns `null` if no such information exists for the given [HoldingIdentity]
     */
    fun get(holdingIdentity: HoldingIdentity): VirtualNodeInfo?

    /**
     * Returns the virtual node info by short-hash code, for a given holding identity
     * without starting any bundles or instantiating any classes.
     *
     * Implementation may return null if it is part of a component.
     *
     * Short id can be generated by calling the [HoldingIdentity.id]
     *
     * Returns `null` if no such information exists
     */
    fun getById(id: String): VirtualNodeInfo?

    /**
     * Callback on receipt of a VirtualNodeInfo message.
     *
     * Call [close()] on the returned object to unregister.
     */
    fun registerCallback(listener: VirtualNodeInfoListener): AutoCloseable
}
