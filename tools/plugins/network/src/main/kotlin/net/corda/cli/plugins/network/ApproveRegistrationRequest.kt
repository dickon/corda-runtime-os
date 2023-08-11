package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import picocli.CommandLine

@CommandLine.Command(
    name = "approve-registration",
    description = ["Approve a registration request."]
)
class ApproveRegistrationRequest : Runnable, RestCommand() {
    @CommandLine.Parameters(
        index = "0",
        description = ["The holding identity ID of the MGM"]
    )
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(
        index = "1",
        description = ["ID of the registration request to approve"]
    )
    lateinit var requestId: String

    override fun run() {
        createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.approveRegistrationRequest(holdingIdentityShortHash, requestId)
        }
    }
}