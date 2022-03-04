package net.corda.membership.read

import net.corda.membership.CPIWhiteList
import net.corda.v5.crypto.PublicKeyHash
import net.corda.v5.membership.GroupParameters
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.membership.MemberInfo

/**
 * Provides group information for a specific group and member. The view of group information may vary based on the
 * requesting member.
 */
interface MembershipGroupReader {
    /**
     * The ID of the group for which this service provides functionality for.
     */
    val groupId: String

    /**
     * The member for which this group information is visible for.
     */
    val owningMember: MemberX500Name

    /**
     * The current group parameters for this group.
     */
    val groupParameters: GroupParameters

    /**
     * The CPI whitelist for this group.
     */
    val cpiWhiteList: CPIWhiteList

    /**
     * Looks up a group member of the specified group by the public key SHA-256 hash
     * belonging to the member.
     * If the member is not found then the null value is returned.
     *
     * @param publicKeyHash Public key hash for the member to lookup.
     */
    fun lookup(publicKeyHash: PublicKeyHash): MemberInfo?

    /**
     * Looks up a group member of the specified group by the MemberX500Name
     * belonging to the member.
     * If the member is not found then the null value is returned.
     *
     * @param name MemberX500Name of the member to lookup.
     */
    fun lookup(name: MemberX500Name): MemberInfo?
}
