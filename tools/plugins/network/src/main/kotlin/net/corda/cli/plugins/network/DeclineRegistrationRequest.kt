package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import net.corda.membership.rest.v1.types.request.ManualDeclinationReason
import picocli.CommandLine

@CommandLine.Command(
    name = "decline-registration",
    description = ["Decline a registration request."]
)
class DeclineRegistrationRequest : Runnable, RestCommand() {
    @CommandLine.Parameters(
        index = "0",
        description = ["The holding identity ID of the MGM"]
    )
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(
        index = "1",
        description = ["ID of the registration request to decline"]
    )
    lateinit var requestId: String

    @CommandLine.Option(
        names = ["--reason"],
        description = ["Reason for declining the registration request"]
    )
    lateinit var reason: ManualDeclinationReason

    override fun run() {
        createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.declineRegistrationRequest(holdingIdentityShortHash, requestId, reason)
        }
    }
}