package net.corda.interop.identity.write.impl

import net.corda.data.KeyValuePair
import net.corda.data.KeyValuePairList
import net.corda.data.membership.PersistentMemberInfo
import net.corda.interop.core.InteropIdentity
import net.corda.membership.lib.MemberInfoExtension
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas
import net.corda.v5.base.types.MemberX500Name
import net.corda.virtualnode.HoldingIdentity
import net.corda.virtualnode.toAvro
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference


@Suppress("ForbiddenComment")
class MembershipInfoProducer(
    private val publisher: AtomicReference<Publisher?>,
    private val sessionKeyGenerator: SessionKeyGenerator,

) {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        val mgmContext = listOf(
            KeyValuePair(MemberInfoExtension.STATUS, "ACTIVE"),
            KeyValuePair(MemberInfoExtension.MODIFIED_TIME, Instant.now().toString()),
            KeyValuePair(MemberInfoExtension.SERIAL, "1"),
        ).sorted()
        private fun getMemberContext(identityToPublish: InteropIdentity, ownedInteropIdentity: InteropIdentity ) : List<KeyValuePair> {
            return listOf(
                KeyValuePair(MemberInfoExtension.PARTY_NAME, identityToPublish.x500Name),
                KeyValuePair(String.format(MemberInfoExtension.URL_KEY, "0"), identityToPublish.endpointUrl),
                KeyValuePair(
                    String.format(MemberInfoExtension.PROTOCOL_VERSION, "0"),
                    identityToPublish.endpointProtocol
                ),
                KeyValuePair(MemberInfoExtension.GROUP_ID, ownedInteropIdentity.groupId),
                KeyValuePair(MemberInfoExtension.SOFTWARE_VERSION, "5.0.0.0-Fox10-RC03"),
                KeyValuePair(MemberInfoExtension.PLATFORM_VERSION, "5000"),
                KeyValuePair(MemberInfoExtension.INTEROP_ROLE, "interop"),
            ).sorted()
        }
    }

    fun publishMemberInfo(
        realHoldingIdentity: HoldingIdentity?,
        ownedInteropIdentity: InteropIdentity,
        newInteropIdentities: List<InteropIdentity>
    ) {
        if (publisher.get() == null) {
            log.error("Member info publisher is null, not publishing.")
            return
        }
        val viewOwningMemberHoldingIdentity = HoldingIdentity(
            MemberX500Name.parse(ownedInteropIdentity.x500Name), ownedInteropIdentity.groupId)

        //todo CORE-16385 Remove hardcoded values
        val memberInfoList = newInteropIdentities.map { identityToPublish ->
            var memberContext = getMemberContext(identityToPublish, ownedInteropIdentity)
            if (realHoldingIdentity != null) {
                val sessionKey = sessionKeyGenerator.getOrCreateSessionKey(identityToPublish, realHoldingIdentity.shortHash.value)
                memberContext = memberContext.plus(
                    listOf(
                        KeyValuePair(String.format(MemberInfoExtension.PARTY_SESSION_KEYS, 0), sessionKey),
                        KeyValuePair(MemberInfoExtension.LEDGER_KEYS_KEY.format(0), sessionKey),
                        KeyValuePair(MemberInfoExtension.INTEROP_MAPPING_X500_NAME, realHoldingIdentity.x500Name.toString()),
                        KeyValuePair(MemberInfoExtension.INTEROP_MAPPING_GROUP, realHoldingIdentity.groupId)
                    )
                ).sorted()
            }

            Record(
                Schemas.Membership.MEMBER_LIST_TOPIC,
                "${ownedInteropIdentity.shortHash}-${identityToPublish.shortHash}",
                PersistentMemberInfo(
                    viewOwningMemberHoldingIdentity.copy(groupId = ownedInteropIdentity.groupId).toAvro(),
                    KeyValuePairList(memberContext),
                    KeyValuePairList(mgmContext),
                    null,
                    null
                )
            )
        }
        publisher.get()!!.publish(memberInfoList)
    }
}
