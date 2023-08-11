package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import picocli.CommandLine

@CommandLine.Command(
    name = "revoke-preauth-token",
    description = ["Revoke a pre-auth token"]
)
class RevokePreAuthToken : Runnable, RestCommand() {
    @CommandLine.Parameters(index = "0", description = ["The holding identity ID of the MGM"])
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(index = "1", description = ["The token ID to revoke"])
    lateinit var preAuthTokenId: String

    @CommandLine.Option(
        names = ["--remarks"],
        description = ["Optional remarks about why the token was revoked"]
    )
    var remarks: String? = null

    override fun run() {
        createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.revokePreAuthToken(holdingIdentityShortHash, preAuthTokenId, remarks)
            println("Token: $preAuthTokenId has been revoked")
        }
    }
}
