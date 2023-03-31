package net.corda.crypto.softhsm.impl

import java.time.Instant
import javax.persistence.EntityManager
import net.corda.crypto.persistence.WrappingKeyInfo
import net.corda.crypto.persistence.db.model.WrappingKeyEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertNotNull


class TestWrappingRepoitory {

    @Test
    fun `JPA equality on primary key only rule for WrappingKeyEntities`() {
        val alpha1 = WrappingKeyEntity("alpha", Instant.now(), 1, "DES", byteArrayOf())
        val alpha2 = WrappingKeyEntity("alpha", Instant.now(), 2, "AES", byteArrayOf())
        val beta = WrappingKeyEntity("beta", Instant.now(), 42, "DES", byteArrayOf())
        assertThat(alpha1).isEqualTo(alpha2)
        assertThat(alpha1).isNotEqualTo(beta)
    }

    @Test
    fun `save a wrapping key`() {
        val stored = ArrayList<WrappingKeyEntity>()
        val em = mock<EntityManager> {
            on { persist(any()) } doAnswer {
                stored.add(it.getArgument(0))
                Unit
            }
            on { find<WrappingKeyEntity>(any(), any()) } doAnswer { stored.first() }
            on { transaction } doReturn mock()
        }
        val repo = WrappingRepositoryImpl(
            mock {
                on { createEntityManager() } doReturn em
            }
        )
        val wrappingKeyInfo = WrappingKeyInfo(1, "caesar", byteArrayOf())
        repo.saveKey("a", wrappingKeyInfo)
        val retrievedWrappingKeyInfo = repo.findKey("a")
        assertNotNull(retrievedWrappingKeyInfo)
        assertThat(wrappingKeyInfo.encodingVersion).isEqualTo(retrievedWrappingKeyInfo.encodingVersion)
    }
}