package net.corda.ledger.consensual.flow.impl.transaction

import net.corda.ledger.common.data.transaction.CordaPackageSummary
import net.corda.ledger.common.data.transaction.PrivacySaltImpl
import net.corda.ledger.common.data.transaction.TransactionMetadata
import net.corda.ledger.common.data.transaction.WireTransaction
import net.corda.ledger.common.flow.impl.transaction.createTransactionSignature
import net.corda.ledger.consensual.data.transaction.ConsensualComponentGroup
import net.corda.sandbox.SandboxGroup
import net.corda.v5.application.crypto.DigitalSignatureVerificationService
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.cipher.suite.CipherSchemeMetadata
import net.corda.v5.cipher.suite.DigestService
import net.corda.v5.cipher.suite.merkle.MerkleTreeProvider
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualTransactionBuilder
import java.security.PublicKey
import java.time.Instant

// TODO Create an AMQP serializer if we plan on sending transaction builders between virtual nodes
@Suppress("LongParameterList")
class ConsensualTransactionBuilderImpl(
    private val cipherSchemeMetadata: CipherSchemeMetadata,
    private val digestService: DigestService,
    private val jsonMarshallingService: JsonMarshallingService,
    private val merkleTreeProvider: MerkleTreeProvider,
    private val serializationService: SerializationService,
    private val signingService: SigningService,
    private val digitalSignatureVerificationService: DigitalSignatureVerificationService,
    private val currentSandboxGroup: SandboxGroup, // TODO CORE-7101 use CurrentSandboxService when it gets available
    // cpi defines what type of signing/hashing is used (related to the digital signature signing and verification stuff)
    private val transactionMetadata: TransactionMetadata,
    override val states: List<ConsensualState> = emptyList(),
) : ConsensualTransactionBuilder {

    private var alreadySigned = false

    override fun withStates(vararg states: ConsensualState): ConsensualTransactionBuilder =
        copy(states = this.states + states)

    @Suspendable
    override fun sign(): ConsensualSignedTransaction {
        TODO("Not yet implemented")
    }

    @Suspendable
    override fun sign(vararg signatories: PublicKey): ConsensualSignedTransaction =
        sign(signatories.toList())

    @Suspendable
    override fun sign(signatories: Iterable<PublicKey>): ConsensualSignedTransaction{
        check(!alreadySigned) { "A transaction cannot be signed twice." }
        require(signatories.toList().isNotEmpty()){
            "At least one key needs to be provided in order to create a signed Transaction!"
        }
        val wireTransaction = buildWireTransaction()
        val signaturesWithMetadata = signatories.map {
            createTransactionSignature(
                signingService,
                serializationService,
                getCpiSummary(),
                wireTransaction.id,
                it
            )
        }

        val tx = ConsensualSignedTransactionImpl(
            serializationService,
            signingService,
            digitalSignatureVerificationService,
            wireTransaction,
            signaturesWithMetadata
        )

        alreadySigned = true
        return tx
    }

    private fun buildWireTransaction(): WireTransaction {
        // TODO(CORE-5982 more verifications)
        // TODO(CORE-5982 ? metadata verifications: nulls, order of CPKs, at least one CPK?))
        require(states.isNotEmpty()) { "At least one consensual state is required" }
        require(states.all { it.participants.isNotEmpty() }) { "All consensual states must have participants" }
        val componentGroupLists = calculateComponentGroupLists()

        val entropy = ByteArray(32)
        cipherSchemeMetadata.secureRandom.nextBytes(entropy)
        val privacySalt = PrivacySaltImpl(entropy)

        return WireTransaction(
            merkleTreeProvider,
            digestService,
            jsonMarshallingService,
            privacySalt,
            componentGroupLists
        )
    }

    private fun calculateComponentGroupLists(): List<List<ByteArray>> {
        val requiredSigningKeys = states
            .flatMap { it.participants }
            .distinct()

        return ConsensualComponentGroup
            .values()
            .sorted()
            .map { componentGroupIndex ->
            when (componentGroupIndex) {
                ConsensualComponentGroup.METADATA ->
                    listOf(
                        jsonMarshallingService.format(transactionMetadata)
                            .toByteArray(Charsets.UTF_8)
                    ) // TODO(update with CORE-6890)
                ConsensualComponentGroup.TIMESTAMP ->
                    listOf(serializationService.serialize(Instant.now()).bytes)
                ConsensualComponentGroup.REQUIRED_SIGNING_KEYS ->
                    requiredSigningKeys.map { serializationService.serialize(it).bytes }
                ConsensualComponentGroup.OUTPUT_STATES ->
                    states.map { serializationService.serialize(it).bytes }
                ConsensualComponentGroup.OUTPUT_STATE_TYPES ->
                    states.map { serializationService.serialize(currentSandboxGroup.getEvolvableTag(it::class.java)).bytes }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConsensualTransactionBuilderImpl) return false
        if (other.transactionMetadata != transactionMetadata) return false
        if (other.states.size != states.size) return false

        return other.states.withIndex().all {
            it.value == states[it.index]
        }
    }

    override fun hashCode(): Int = states.hashCode()

    private fun copy(states: List<ConsensualState> = this.states): ConsensualTransactionBuilderImpl {
        return ConsensualTransactionBuilderImpl(
            cipherSchemeMetadata,
            digestService,
            jsonMarshallingService,
            merkleTreeProvider,
            serializationService,
            signingService,
            digitalSignatureVerificationService,
            currentSandboxGroup,
            transactionMetadata,
            states,
        )
    }

    /**
     * TODO [CORE-7126] Fake values until we can get CPI information properly
     */
    private fun getCpiSummary(): CordaPackageSummary =
        CordaPackageSummary(
            name = "CPI name",
            version = "CPI version",
            signerSummaryHash = SecureHash("SHA-256", "Fake-value".toByteArray()).toHexString(),
            fileChecksum = SecureHash("SHA-256", "Another-Fake-value".toByteArray()).toHexString()
        )
}
