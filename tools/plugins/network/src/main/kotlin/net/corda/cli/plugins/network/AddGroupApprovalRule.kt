package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import net.corda.membership.rest.v1.types.request.ApprovalRuleRequestParams
import picocli.CommandLine

@CommandLine.Command(
    name = "add-group-approval-rule",
    description = ["Add an approval rule for manually reviewing registration requests"]
)
class AddGroupApprovalRule : Runnable, RestCommand() {
    @CommandLine.Parameters(index = "0", description = ["The holding identity ID of the MGM"])
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(
        index = "1",
        description = ["The regular expression rule to configure manual reviews"]
    )
    lateinit var ruleRegex: String

    override fun run() {
        val ruleParams = ApprovalRuleRequestParams(ruleRegex)
        val approvalRuleInfo = createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.addGroupApprovalRule(holdingIdentityShortHash, ruleParams)
        }
        println(approvalRuleInfo)
    }
}