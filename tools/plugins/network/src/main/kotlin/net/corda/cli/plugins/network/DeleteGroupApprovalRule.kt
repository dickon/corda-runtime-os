package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import picocli.CommandLine

@CommandLine.Command(
    name = "delete-group-approval-rule",
    description = ["Delete an approval rule for manually reviewing registration requests"]
)
class DeleteGroupApprovalRule : Runnable, RestCommand() {
    @CommandLine.Parameters(index = "0", description = ["The holding identity ID of the MGM"])
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(
        index = "1",
        description = ["The ID of the approval rule to delete"]
    )
    lateinit var ruleId: String

    override fun run() {
        createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.deleteGroupApprovalRule(holdingIdentityShortHash, ruleId)
            println("Successfully deleted")
        }
    }
}