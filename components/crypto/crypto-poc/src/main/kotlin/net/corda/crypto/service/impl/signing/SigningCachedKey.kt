package net.corda.crypto.service.impl.signing

import java.time.Instant

@Suppress("LongParameterList")
class SigningCachedKey(
    val id: String,
    val tenantId: String,
    val category: String,
    val alias: String?,
    val hsmAlias: String?,
    val publicKey: ByteArray,
    val keyMaterial: ByteArray?,
    val schemeCodeName: String,
    val masterKeyAlias: String?,
    val externalId: String?,
    val encodingVersion: Int?,
    val timestamp: Instant,
    val hsmId: String,
    val status: SigningKeyStatus
)