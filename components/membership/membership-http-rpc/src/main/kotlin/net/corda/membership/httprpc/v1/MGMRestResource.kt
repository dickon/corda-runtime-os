package net.corda.membership.httprpc.v1

import net.corda.httprpc.RestResource
import net.corda.httprpc.annotations.HttpDELETE
import net.corda.httprpc.annotations.HttpGET
import net.corda.httprpc.annotations.HttpPOST
import net.corda.httprpc.annotations.HttpPUT
import net.corda.httprpc.annotations.HttpRestResource
import net.corda.httprpc.annotations.RestPathParameter
import net.corda.httprpc.annotations.RestQueryParameter
import net.corda.httprpc.annotations.RestRequestBodyParameter
import net.corda.membership.httprpc.v1.types.request.ApprovalRuleRequestParams
import net.corda.membership.httprpc.v1.types.request.PreAuthTokenRequest
import net.corda.membership.httprpc.v1.types.response.ApprovalRuleInfo
import net.corda.membership.httprpc.v1.types.response.PreAuthToken
import net.corda.membership.httprpc.v1.types.response.PreAuthTokenStatus

/**
 * The MGM API consists of a number of endpoints used to manage membership groups. A membership group is a logical
 * grouping of a number of Corda Identities to communicate and transact with one another with a specific set of CorDapps.
 * The API allows you to generate the group policy for a membership group, required for new members to join the group.
 */
@HttpRestResource(
    name = "MGM API",
    description = "The MGM API consists of a number of endpoints used to manage membership groups. A membership group" +
            " is a logical grouping of a number of Corda Identities to communicate and transact with one another with" +
            " a specific set of CorDapps. The API allows you to generate the group policy for a membership group," +
            " required for new members to join the group.",
    path = "mgm"
)
interface MGMRestResource : RestResource {
    /**
     * The [generateGroupPolicy] method enables you to retrieve the group policy from the MGM represented by
     * [holdingIdentityShortHash], required for new members to join the membership group.
     *
     * Example usage:
     * ```
     * mgmOps.generateGroupPolicy(holdingIdentityShortHash = "58B6030FABDD")
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group to be joined.
     *
     * @return The group policy generated by the MGM in JSON [String] format.
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/info",
        description = "This API retrieves the group policy from the MGM required to join the membership group.",
        responseDescription = "The group policy from the MGM required to join the membership group as a string " +
                "in JSON format"
    )
    fun generateGroupPolicy(
        @RestPathParameter(description = "The holding identity ID of the MGM of the membership group to be joined")
        holdingIdentityShortHash: String
    ): String

    /**
     * Adds client certificate subject to the mutual TLS allowed client certificates.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @param subject The certificate subject.
     */
    @HttpPUT(
        path = "{holdingIdentityShortHash}/mutual-tls/allowed-client-certificate-subjects/{subject}",
        description = "This API allows a client certificate with a " +
            "given subject to be used in mutual TLS connections.",
    )
    fun mutualTlsAllowClientCertificate(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String,
        @RestPathParameter(description = "The certificate subject.")
        subject: String,
    )

    /**
     * Remove client certificate subject from the mutual TLS group allowed client certificates.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @param subject The certificate subject.
     */
    @HttpDELETE(
        path = "{holdingIdentityShortHash}/mutual-tls/allowed-client-certificate-subjects/{subject}",
        description = "This API disallows a client certificate with a " +
                "given subject to be used in mutual TLS connections.",
    )
    fun mutualTlsDisallowClientCertificate(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String,
        @RestPathParameter(description = "The certificate subject.")
        subject: String,
    )

    /**
     * List the allowed client certificate subjects for mutual TLS.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @return List of the allowed client certificate subjects.
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/mutual-tls/allowed-client-certificate-subjects",
        description = "This API list the allowed  client certificates subjects " +
                "to be used in mutual TLS connections.",
        responseDescription = "List of the allowed client certificate subjects",
    )
    fun mutualTlsListClientCertificate(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String,
    ): Collection<String>

    /**
     * Generate a preAuthToken.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @param request Details of the token to create.
     *
     * @return Details of the created token.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/preauthtoken",
    )
    fun generatePreAuthToken(
        @RestPathParameter
        holdingIdentityShortHash: String,
        @RestRequestBodyParameter
        request: PreAuthTokenRequest
    ): PreAuthToken

    /**
     * Query for preAuthTokens.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @param ownerX500Name The X500 name of the member to query for.
     * @param preAuthTokenId The token ID to query for.
     * @param viewInactive Return tokens with status [PreAuthTokenStatus.REVOKED], [PreAuthTokenStatus.CONSUMED],
     * [PreAuthTokenStatus.AUTO_INVALIDATED] as well as [PreAuthTokenStatus.AVAILABLE].
     *
     * @return A list tokens matching the query or an empty list, if no tokens match the query.
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/preauthtoken",
    )
    fun getPreAuthTokens(
        @RestPathParameter
        holdingIdentityShortHash: String,
        @RestQueryParameter(required = false)
        ownerX500Name: String? = null,
        @RestQueryParameter(required = false)
        preAuthTokenId: String? = null,
        @RestQueryParameter(required = false)
        viewInactive: Boolean = false
    ): Collection<PreAuthToken>

    /**
     * Revoke a preAuthToken.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM.
     * @param preAuthTokenId The token ID to revoke.
     * @param remarks Some optional remarks about why the token was revoked.
     *
     * @return Details of the revoked token.
     */
    @HttpPUT(
        path = "{holdingIdentityShortHash}/preauthtoken/revoke/{preAuthTokenId}",
    )
    fun revokePreAuthToken(
        @RestPathParameter
        holdingIdentityShortHash: String,
        @RestPathParameter
        preAuthTokenId: String,
        @RestRequestBodyParameter(required = false)
        remarks: String? = null
    ): PreAuthToken

    /**
     * The [addGroupApprovalRule] method enables you to add a regular expression rule to configure the approval
     * method for registration (or re-registration) requests. Requests are evaluated against the rules added
     * using this method to determine whether they will be automatically or manually approved. If the [MemberInfo]
     * proposed in the request contains any new or changed keys that match one or more of the approval rules, the
     * request will require manual approval.
     *
     * Example usage:
     * ```
     * mgmOps.addGroupApprovalRule("58B6030FABDD", ApprovalRuleRequestParams("corda.roles.*", "roles rule"))
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param ruleParams The approval rule information including the regular expression associated with the rule, and an
     * optional label describing the rule
     *
     * @return Details of the newly persisted approval rule.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/approval/rules",
        description = "This API adds a rule to the set of group approval rules.",
        responseDescription = "Details of the newly persisted approval rule"
    )
    fun addGroupApprovalRule(
        @RestPathParameter(description = "The holding identity ID of the MGM of the membership group")
        holdingIdentityShortHash: String,
        @RestRequestBodyParameter(description = "The approval rule information including the regular expression " +
                "associated with the rule, and an optional label describing the rule")
        ruleParams: ApprovalRuleRequestParams,
    ): ApprovalRuleInfo

    /**
     * The [getGroupApprovalRules] method enables you to retrieve the set of approval rules the group is currently
     * configured with. Registration (or re-registration) requests are evaluated against these rules to determine
     * whether they will be automatically or manually approved. If the [MemberInfo] proposed in a request contains any
     * new or changed keys that match one or more of the approval rules in this collection, the request will require
     * manual approval.
     *
     * Example usage:
     * ```
     * mgmOps.getGroupApprovalRules(holdingIdentityShortHash = "58B6030FABDD")
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     *
     * @return Group approval rules as a collection of [ApprovalRuleInfo].
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/approval/rules",
        description = "This API retrieves the set of rules the group is currently configured with",
        responseDescription = "Collection of group approval rules"
    )
    fun getGroupApprovalRules(
        @RestPathParameter(description = "The holding identity ID of the MGM of the membership group")
        holdingIdentityShortHash: String
    ): Collection<ApprovalRuleInfo>

    /**
     * The [deleteGroupApprovalRule] method allows you to delete a group approval rule that was added through the
     * [addGroupApprovalRule] method.
     *
     * Example usage:
     * ```
     * mgmOps.deleteGroupApprovalRule(holdingIdentityShortHash = "58B6030FABDD", ruleId = "b305129b-8c92-4092-b3a2-e6d452ce2b01")
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param ruleId ID of the group approval rule to be deleted.
     */
    @HttpDELETE(
        path = "{holdingIdentityShortHash}/approval/rules/{ruleId}",
        description = "This API deletes a previously added group approval rule."
    )
    fun deleteGroupApprovalRule(
        @RestPathParameter(description = "The holding identity ID of the MGM of the membership group")
        holdingIdentityShortHash: String,
        @RestPathParameter(description = "The ID of the group approval rule to be deleted")
        ruleId: String
    )

    /**
     * This method enables you to add a regular expression rule to configure the manual reviews of registration
     * requests. Requests which contain a valid pre-auth token are evaluated against the rules added using this method
     * to determine whether they will be automatically or manually approved. If the [MemberInfo] proposed in the request
     * contains any new, removed or changed keys that match one or more of the approval rules, the request will require
     * manual approval.
     *
     * @see addGroupApprovalRule for how to add approval rules for registrations with a pre-auth token.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param ruleParams The approval rule definition. See [ApprovalRuleRequestParams] for more details.
     *
     * @return Details of the newly persisted approval rule.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/approval/rules/preauth",
        description = "This API adds a rule to the set of group approval rules for registrations " +
                "including a pre-auth token.",
        responseDescription = "Details of the newly persisted approval rule."
    )
    fun addPreAuthGroupApprovalRule(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String,
        @RestRequestBodyParameter(description = "The definition of the approval rule to create.")
        ruleParams: ApprovalRuleRequestParams,
    ): ApprovalRuleInfo

    /**
     * This method enables you to retrieve the set of approval rules the group is currently configured with for
     * registration requests containing a valid pre-auth token. Registration requests with a valid pre-auth token are
     * evaluated against these rules to determine whether they will be automatically or manually approved. If the
     * [MemberInfo] proposed in a request contains any new, removed, or changed keys that match one or more of the
     * approval rules in this collection, the request will require manual approval.
     *
     * @see getGroupApprovalRules for how to retrieve approval rules for registrations with a pre-auth token.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     *
     * @return Group approval rules as a collection of [ApprovalRuleInfo].
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/approval/rules/preauth",
        description = "This API retrieves the set of rules the group is currently configured with for " +
                "registration request with a pre-auth token.",
        responseDescription = "A collection of group approval rules."
    )
    fun getPreAuthGroupApprovalRules(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String
    ): Collection<ApprovalRuleInfo>

    /**
     * This method allows you to delete a group approval rule that was added through the [addPreAuthGroupApprovalRule]
     * method.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param ruleId ID of the group approval rule to be deleted.
     */
    @HttpDELETE(
        path = "{holdingIdentityShortHash}/approval/rules/preauth/{ruleId}",
        description = "This API deletes a group approval rule for registrations including a pre-auth token."
    )
    fun deletePreAuthGroupApprovalRule(
        @RestPathParameter(description = "The holding identity ID of the MGM.")
        holdingIdentityShortHash: String,
        @RestPathParameter(description = "The ID of the group approval rule to be deleted.")
        ruleId: String
    )
}