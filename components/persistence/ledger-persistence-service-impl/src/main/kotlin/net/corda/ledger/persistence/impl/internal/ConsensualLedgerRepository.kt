package net.corda.ledger.persistence.impl.internal

import net.corda.data.persistence.EntityResponse
import net.corda.ledger.common.impl.transaction.PrivacySaltImpl
import net.corda.ledger.common.impl.transaction.TransactionMetaData.Companion.CPK_IDENTIFIERS_KEY
import net.corda.ledger.common.impl.transaction.WireTransaction
import net.corda.ledger.consensual.impl.transaction.ConsensualSignedTransactionImpl
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.application.serialization.deserialize
import net.corda.v5.base.types.toHexString
import net.corda.v5.base.util.contextLogger
import net.corda.v5.cipher.suite.DigestService
import net.corda.v5.cipher.suite.merkle.MerkleTreeProvider
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.crypto.SecureHash
import java.security.MessageDigest
import java.time.Instant
import javax.persistence.EntityManager
import javax.persistence.Tuple

/**
 * Reads and writes ledger transaction data to and from the virtual node vault database.
 */
class ConsensualLedgerRepository(
    private val merkleTreeProvider: MerkleTreeProvider,
    private val digestService: DigestService,
    private val jsonMarshallingService: JsonMarshallingService,
    private val serializationService: SerializationService
) {
    companion object {
        private val logger = contextLogger()
        private val componentGroupListsTuplesMapper = ComponentGroupListsTuplesMapper()
    }

    /** Reads [ConsensualSignedTransactionImpl] with given [id] from database. */
    fun findTransaction(entityManager: EntityManager, id: String): ConsensualSignedTransactionImpl? {

        val rows = entityManager.createNativeQuery(
            """
                SELECT tx.id, tx.privacy_salt, tx.account_id, tx.created, txc.group_idx, txc.leaf_idx, txc.data, txc.hash
                FROM {h-schema}consensual_transaction AS tx
                JOIN {h-schema}consensual_transaction_component AS txc ON tx.id = txc.transaction_id
                WHERE tx.id = :id
                ORDER BY txc.group_idx, txc.leaf_idx
                """,
            Tuple::class.java)
            .setParameter("id", id)
            .resultList
            .map { it as Tuple }

        if (rows.isEmpty()) return null

        val firstRowColumns = rows.first()
        val privacySalt = PrivacySaltImpl(firstRowColumns[1] as ByteArray)
        val componentGroupLists = rows.mapTuples(componentGroupListsTuplesMapper)
        val wireTransaction = WireTransaction(merkleTreeProvider, digestService, jsonMarshallingService, privacySalt, componentGroupLists)
        return ConsensualSignedTransactionImpl(
            serializationService,
            wireTransaction,
            findSignatures(entityManager, id))
    }

    /** Reads [DigitalSignatureAndMetadata] for signed transaction with given [transactionId] from database. */
    private fun findSignatures(
        entityManager: EntityManager,
        transactionId: String
    ): List<DigitalSignatureAndMetadata> {
        return entityManager.createNativeQuery(
            """
                SELECT signature
                FROM {h-schema}consensual_transaction_signature
                WHERE transaction_id = :transactionId
                ORDER BY signature_idx""",
            Tuple::class.java)
            .setParameter("transactionId", transactionId)
            .resultList
            .map { it as Tuple }
            .map { r -> serializationService.deserialize(r.get(0) as ByteArray) }
    }

    /** Persists [signedTransaction] data to database. */
    fun persistTransaction(
        entityManager: EntityManager,
        signedTransaction: ConsensualSignedTransactionImpl,
        account :String
    ): EntityResponse{
        val transactionId = signedTransaction.id.toHexString()
        val wireTransaction = signedTransaction.wireTransaction
        val now = Instant.now()
        persistTransaction(entityManager, now, wireTransaction, account)
        wireTransaction.componentGroupLists.mapIndexed { groupIndex, leaves ->
            leaves.mapIndexed { leafIndex, bytes ->
                persistComponentLeaf(entityManager, now, transactionId, groupIndex, leafIndex, bytes)
            }
        }
        persistTransactionStatus(entityManager, now, transactionId, "Faked") // TODO where to get the status from
        // TODO when and what do we write to the CPKs table?
        persistCpk(entityManager, now, wireTransaction)
        persistTransactionCpk(entityManager, transactionId)

        signedTransaction.signatures.forEachIndexed { index, digitalSignatureAndMetadata ->
            persistSignature(entityManager, now, transactionId, index, digitalSignatureAndMetadata)
        }

        return EntityResponse(emptyList())
    }

    /** Persists [wireTransaction] data to database. */
    private fun persistTransaction(
        entityManager: EntityManager,
        timestamp: Instant,
        wireTransaction: WireTransaction,
        account: String
    ) {
        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_transaction(id, privacy_salt, account_id, created)
                VALUES (:id, :privacySalt, :accountId, :createdAt)"""
        )
            .setParameter("id", wireTransaction.id.toHexString())
            .setParameter("privacySalt", wireTransaction.privacySalt.bytes)
            .setParameter("accountId", account)
            .setParameter("createdAt", timestamp)
            .executeUpdate()
    }

    /** Persists component's leaf [data] to database. */
    @Suppress("LongParameterList")
    private fun persistComponentLeaf(
        entityManager: EntityManager,
        timestamp: Instant,
        transactionId: String,
        groupIndex: Int,
        leafIndex: Int,
        data: ByteArray
    ) {
        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_transaction_component(transaction_id, group_idx, leaf_idx, data, hash, created)
                VALUES(:transactionId, :groupIndex, :leafIndex, :data, :hash, :createdAt)"""
        )
            .setParameter("transactionId", transactionId)
            .setParameter("groupIndex", groupIndex)
            .setParameter("leafIndex", leafIndex)
            .setParameter("data", data)
            .setParameter("hash", data.hashAsHexString())
            .setParameter("createdAt", timestamp)
            .executeUpdate()
    }

    /** Persists transaction's [status] to database. */
    private fun persistTransactionStatus(
        entityManager: EntityManager,
        timestamp: Instant,
        transactionId: String,
        status: String
    ) {
        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_transaction_status(transaction_id, status, created)
                VALUES (:txId, :status, :createdAt)"""
        )
            .setParameter("txId", transactionId)
            .setParameter("status", status)
            .setParameter("createdAt", timestamp)
            .executeUpdate()
    }

    /** Persists CPK data to database. */
    private fun persistCpk(
        entityManager: EntityManager,
        timestamp: Instant,
        wireTransaction: WireTransaction
    ) {
        // TODO get values from transaction metadata
        val cpkIdentifiers = wireTransaction.metadata[CPK_IDENTIFIERS_KEY]
        logger.info("cpkIdentifiers = [$cpkIdentifiers]")
        val fileHash = SecureHash.parse("SHA-256:1234567890123456")
        val name = "cpk-name"
        val signerHash = SecureHash.parse("SHA-256:0000000000000000")
        val version = 1
        val data = ByteArray(10000)

        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_cpk(file_hash, name, signer_hash, version, data, created)
                VALUES (:fileHash, :name, :signerHash, :version, :data, :createdAt) ON CONFLICT DO NOTHING""")
            .setParameter("fileHash", fileHash.toHexString())
            .setParameter("name", name)
            .setParameter("signerHash", signerHash.toHexString())
            .setParameter("version", version)
            .setParameter("data", data)
            .setParameter("createdAt", timestamp)
            .executeUpdate()
    }

    /** Persists link between transaction with ID [transactionId] and it's CPK data to database. */
    private fun persistTransactionCpk(
        entityManager: EntityManager,
        transactionId: String
    ) {
        // TODO get values from transaction metadata
        val fileHash = SecureHash.parse("SHA-256:1234567890123456")
        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_transaction_cpk(transaction_id, file_hash)
                VALUES (:transactionId, :fileHash)""")
            .setParameter("transactionId", transactionId)
            .setParameter("fileHash", fileHash.toHexString())
            .executeUpdate()
    }

    /** Persists transaction's [signature] to database. */
    private fun persistSignature(
        entityManager: EntityManager,
        timestamp: Instant,
        transactionId: String,
        index: Int,
        signature: DigitalSignatureAndMetadata
    ) {
        entityManager.createNativeQuery(
            """
                INSERT INTO {h-schema}consensual_transaction_signature(transaction_id, signature_idx, signature, pub_key_hash, created)
                VALUES (:transactionId, :signatureIdx, :signature, :publicKeyHash, :createdAt)""")
            .setParameter("transactionId", transactionId)
            .setParameter("signatureIdx", index)
            .setParameter("signature", serializationService.serialize(signature).bytes)
            .setParameter("publicKeyHash", signature.by.encoded.hashAsHexString())
            .setParameter("createdAt", timestamp)
            .executeUpdate()
    }

    private fun ByteArray.hashAsHexString() =
        MessageDigest.getInstance(DigestAlgorithmName.SHA2_256.name).digest(this).toHexString()
}