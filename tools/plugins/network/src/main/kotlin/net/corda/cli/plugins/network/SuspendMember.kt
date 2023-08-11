package net.corda.cli.plugins.network

import net.corda.cli.plugins.common.RestClientUtils.createRestClient
import net.corda.cli.plugins.common.RestCommand
import net.corda.membership.rest.v1.MGMRestResource
import net.corda.membership.rest.v1.types.request.SuspensionActivationParameters
import picocli.CommandLine

@CommandLine.Command(
    name = "suspend-member",
    description = ["Suspend a member."]
)
class SuspendMember : Runnable, RestCommand() {
    @CommandLine.Parameters(
        index = "0",
        description = ["The holding identity ID of the MGM"]
    )
    lateinit var holdingIdentityShortHash: String

    @CommandLine.Parameters(
        index = "1",
        description = ["The X500 name of the member to suspend"]
    )
    lateinit var memberX500Name: String

    override fun run() {
        val suspensionParams = SuspensionActivationParameters(memberX500Name)
        createRestClient(MGMRestResource::class).use { client ->
            client.start().proxy.suspendMember(holdingIdentityShortHash, suspensionParams)
        }
    }
}