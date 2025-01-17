package net.corda.virtualnode.write.db.impl.writer

import javax.persistence.EntityManager
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.libs.packaging.core.CpiMetadata

internal interface CpiEntityRepository {

    fun getCpiMetadataByChecksum(cpiFileChecksum: String): CpiMetadata?

    fun getCPIMetadataByNameAndVersion(name: String, version: String): CpiMetadata?

    fun getCPIMetadataById(em: EntityManager, id: CpiIdentifier): CpiMetadata?
}

