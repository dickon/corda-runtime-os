package net.corda.interop.core

import net.corda.data.interop.PersistentInteropIdentity
import net.corda.v5.application.interop.facade.FacadeId


data class InteropIdentity(
    val x500Name: String,
    val groupId: String,
    val owningVirtualNodeShortHash: String,
    val facadeIds: List<FacadeId>,
    val applicationName: String,
    val endpointUrl: String,
    val endpointProtocol: String
) {
    val shortHash = Utils.computeShortHash(x500Name, groupId)

    companion object {
        fun of(virtualNodeShortHash: String, interopIdentity: PersistentInteropIdentity): InteropIdentity {
            val listOfFacadeIds = mutableListOf<FacadeId>()
            for (facadeId in interopIdentity.facadeIds) {
                val facade = FacadeId.of(facadeId)
                listOfFacadeIds.add(facade)
            }
            return InteropIdentity(
                interopIdentity.x500Name,
                interopIdentity.groupId,
                virtualNodeShortHash,
                listOfFacadeIds,
                interopIdentity.applicationName,
                interopIdentity.endpointUrl,
                interopIdentity.endpointProtocol
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InteropIdentity

        if (x500Name != other.x500Name) return false
        if (groupId != other.groupId) return false
        return shortHash == other.shortHash
    }

    override fun hashCode(): Int {
        var result = x500Name.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + owningVirtualNodeShortHash.hashCode()
        result = 31 * result + shortHash.hashCode()
        return result
    }
}
