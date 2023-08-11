package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import net.corda.membership.rest.v1.types.request.PreAuthTokenRequest
import picocli.CommandLine
import java.time.Duration

@CommandLine.Command(
    name = "generate-preauth-token",
    description = ["Generate a pre-auth token"]
)
class GeneratePreAuthToken : Runnable, RestCommand() {
    @CommandLine.Parameters(index = "0", description = ["The holding identity ID of the MGM"])
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(index = "1", description = ["The X500 name of the member to preauthorize"])
    lateinit var ownerX500Name: String

    @CommandLine.Option(
        names = ["--ttl"],
        description = ["A (time-to-live) duration after which this token will become invalid"]
    )
    var ttl: Duration? = null

    @CommandLine.Option(
        names = ["--remarks"],
        description = ["Optional remarks"]
    )
    var remarks: String? = null

    override fun run() {
        val request = PreAuthTokenRequest(ownerX500Name, ttl, remarks)
        val preAuthToken = createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.generatePreAuthToken(holdingIdentityShortHash, request)
        }
        println("Token submitted successfully : $preAuthToken")
    }
}