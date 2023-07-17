package net.corda.membership.rest.v1

import net.corda.membership.rest.v1.types.RestGroupParameters
import net.corda.rest.RestResource
import net.corda.rest.annotations.HttpDELETE
import net.corda.rest.annotations.HttpGET
import net.corda.rest.annotations.HttpPOST
import net.corda.rest.annotations.HttpPUT
import net.corda.rest.annotations.HttpRestResource
import net.corda.rest.annotations.RestPathParameter
import net.corda.rest.annotations.RestQueryParameter
import net.corda.rest.annotations.ClientRequestBodyParameter
import net.corda.membership.rest.v1.types.request.ApprovalRuleRequestParams
import net.corda.membership.rest.v1.types.request.PreAuthTokenRequest
import net.corda.membership.rest.v1.types.request.ManualDeclinationReason
import net.corda.membership.rest.v1.types.request.SuspensionActivationParameters
import net.corda.membership.rest.v1.types.response.ApprovalRuleInfo
import net.corda.membership.rest.v1.types.response.PreAuthToken
import net.corda.membership.rest.v1.types.response.PreAuthTokenStatus
import net.corda.membership.rest.v1.types.response.RestRegistrationRequestStatus

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
@Suppress("TooManyFunctions")
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
        @ClientRequestBodyParameter
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
        @RestQueryParameter(required = false, default = "false")
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
        @ClientRequestBodyParameter(required = false)
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
        @ClientRequestBodyParameter(description = "The approval rule information including the regular expression " +
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
        @ClientRequestBodyParameter(description = "The definition of the approval rule to create.")
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

    /**
     * The [viewRegistrationRequests] method enables you to view registration requests submitted for joining the
     * membership group which require a manual review. The requests may be optionally filtered by the X.500 name of the
     * requesting member, and/or by the status of the request (historic or pending review).
     *
     * Example usage:
     * ```
     * mgmOps.viewRegistrationRequests("58B6030FABDD")
     * mgmOps.viewRegistrationRequests("58B6030FABDD", "O=Alice, L=London, C=GB")
     * mgmOps.viewRegistrationRequests("58B6030FABDD", "O=Alice, L=London, C=GB", true)
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param requestSubjectX500Name Optional. X.500 name of the subject of the registration request.
     * @param viewHistoric Optional. Set this to 'true' to view both pending review and completed (historic) requests.
     * Defaults to 'false' (requests pending review only).
     *
     * @return Registration requests as a collection of [RestRegistrationRequestStatus].
     */
    @HttpGET(
        path = "{holdingIdentityShortHash}/registrations",
    )
    fun viewRegistrationRequests(
        @RestPathParameter(
            description = "The holding identity ID of the MGM of the membership group"
        )
        holdingIdentityShortHash: String,
        @RestQueryParameter(
            description = "X.500 name of the requesting member",
            required = false
        )
        requestSubjectX500Name: String? = null,
        @RestQueryParameter(
            description = "Include completed (historic) requests if set to 'true'",
            required = false,
            default = "false"
        )
        viewHistoric: Boolean = false
    ): Collection<RestRegistrationRequestStatus>

    /**
     * The [approveRegistrationRequest] method enables you to approve registration requests which require
     * manual approval. This method can only be used for requests that are in "PENDING_MANUAL_APPROVAL" status.
     *
     * Example usage:
     * ```
     * mgmOps.reviewRegistrationRequest("58B6030FABDD", "3B9A266F96E2")
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param requestId ID of the registration request.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/approve/{requestId}"
    )
    fun approveRegistrationRequest(
        @RestPathParameter(
            description = "The holding identity ID of the MGM of the membership group"
        )
        holdingIdentityShortHash: String,
        @RestPathParameter(
            description = "ID of the registration request"
        )
        requestId: String
    )

    /**
     * The [declineRegistrationRequest] method enables you to decline registration requests which require
     * manual approval. This method can only be used for requests that are in "PENDING_MANUAL_APPROVAL" status.
     *
     * Example usage:
     * ```
     * mgmOps.reviewRegistrationRequest(
     * "58B6030FABDD", "3B9A266F96E2", "Sample reason"
     * )
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param requestId ID of the registration request.
     * @param reason Reason [ManualDeclinationReason] for declining the specified registration request.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/decline/{requestId}"
    )
    fun declineRegistrationRequest(
        @RestPathParameter(
            description = "The holding identity ID of the MGM of the membership group"
        )
        holdingIdentityShortHash: String,
        @RestPathParameter(
            description = "ID of the registration request"
        )
        requestId: String,
        @ClientRequestBodyParameter(
            description = "Reason for declining the specified registration request"
        )
        reason: ManualDeclinationReason
    )

    /**
     * The [suspendMember] method enables you to suspend a member. A suspended member is blocked from communicating
     * with other members of the group, and will not see any updates related to the group or the other members.
     *
     * Example usage:
     * ```
     * mgmOps.suspendMember("58B6030FABDD", SuspendMemberParameters("O=Alice, L=London, C=GB"))
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param suspensionParams Parameters for suspending a member. See [SuspensionActivationParameters] for more details.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/suspend"
    )
    fun suspendMember(
        @RestPathParameter(
            description = "The holding identity ID of the MGM of the membership group"
        )
        holdingIdentityShortHash: String,
        @ClientRequestBodyParameter(
            description = "Parameters for suspending a member."
        )
        suspensionParams: SuspensionActivationParameters
    )

    /**
     * The [activateMember] method enables you to activate a previously suspended member. An activated member is
     * allowed to communicate with other members of the group again, and is able to receive updates related to the
     * group or the other members.
     *
     * Example usage:
     * ```
     * mgmOps.activateMember("58B6030FABDD", SuspendMemberParameters("O=Alice, L=London, C=GB"))
     * ```
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param activationParams Parameters for activating a member. See [SuspensionActivationParameters] for more details.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/activate"
    )
    fun activateMember(
        @RestPathParameter(
            description = "The holding identity ID of the MGM of the membership group"
        )
        holdingIdentityShortHash: String,

        @ClientRequestBodyParameter(
            description = "Parameters for suspending or activating a member."
        )
        activationParams: SuspensionActivationParameters
    )

    /**
     * The [updateGroupParameters] method allows you to make changes to the group parameters by submitting an updated
     * version of the group parameters. [newGroupParameters] may only include custom fields (with "ext." prefix) and
     * minimum platform version. If [newGroupParameters] contains no changes, the current group parameters are returned.
     *
     * @see MemberLookupRestResource.viewGroupParameters for how to view the current group parameters for the group.
     *
     * @param holdingIdentityShortHash The holding identity ID of the MGM of the membership group.
     * @param newGroupParameters Group parameters as a [Map] containing the desired changes.
     *
     * @return The newly updated group parameters.
     */
    @HttpPOST(
        path = "{holdingIdentityShortHash}/group-parameters",
        description = "This API allows you to make changes to the group parameters by submitting an updated version " +
                "of the group parameters.",
        responseDescription = "The newly updated group parameters"
    )
    fun updateGroupParameters(
        @RestPathParameter(description = "The holding identity ID of the MGM")
        holdingIdentityShortHash: String,
        @ClientRequestBodyParameter(description = "Updated version of the group parameters")
        newGroupParameters: RestGroupParameters,
    ): RestGroupParameters
}