package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import picocli.CommandLine

@CommandLine.Command(
    name = "get-registrations",
    description = ["Check the status of a registration request."]
)
class GetRegistrationsRequest : Runnable, RestCommand() {
    @CommandLine.Parameters(index = "0", description = ["The holding identity ID of the MGM"])
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Option(
        names = ["--request-id"],
        description = ["ID of the registration request. Returns all visible requests if not specified."]
    )
    var requestId: String? = null

    override fun run() {
        val registrationRequests = createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.viewRegistrationRequests(holdingIdentityShortHash, requestId)
        }
        println(registrationRequests)
    }
}