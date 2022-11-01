package net.corda.virtualnode.rpcops.impl.validation

import net.corda.cpiinfo.read.CpiInfoReadService
import net.corda.httprpc.exception.BadRequestException
import net.corda.httprpc.exception.ResourceNotFoundException
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.v5.crypto.SecureHash
import net.corda.virtualnode.ShortHash
import net.corda.virtualnode.ShortHashException
import net.corda.virtualnode.read.VirtualNodeInfoReadService

class VirtualNodeValidationService(
    private val virtualNodeInfoReadService: VirtualNodeInfoReadService,
    private val cpiInfoReadService: CpiInfoReadService
) {
    fun validateVirtualNodeExists(virtualNodeShortId: String) {
        virtualNodeInfoReadService.getByHoldingIdentityShortHash(parseShortHash(virtualNodeShortId))
            ?: throw ResourceNotFoundException("Virtual node", virtualNodeShortId)
    }

    fun validateAndGetUpgradeCpi(cpiFileChecksum: String): CpiMetadata {
        val cpiByChecksum = cpiInfoReadService.getAll().filter { it.fileChecksum == SecureHash.parse(cpiFileChecksum) }
        if (cpiByChecksum.isEmpty()) {
            throw ResourceNotFoundException("CPI", cpiFileChecksum)
        }
        return cpiByChecksum.single()
    }

    fun validateCpiUpgradePrerequisites(currentCpi: CpiMetadata, upgradeCpi: CpiMetadata) {
        require(upgradeCpi.cpiId.name == currentCpi.cpiId.name) {
            "Upgrade CPI must have the same name as the current CPI."
        }

        require(upgradeCpi.cpiId.signerSummaryHash == currentCpi.cpiId.signerSummaryHash) {
            "Upgrade CPI must have the same signature summary hash."
        }
    }

    private fun parseShortHash(virtualNodeShortId: String): ShortHash {
        return try {
            ShortHash.of(virtualNodeShortId)
        } catch (e: ShortHashException) {
            throw BadRequestException("Invalid holding identity short hash${e.message?.let { ": $it" }}")
        }
    }
}