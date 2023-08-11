package net.corda.cli.plugins.network

import picocli.CommandLine.Command

@Command(
    name = "operate",
    description = [
        "Operate on the network",
        "This sub command should only be used for internal development"
    ],
    subcommands = [
        AllowClientCertificate::class,
        ExportGroupPolicy::class,
        GetPreAuthRules::class,
        AddPreAuthRule::class,
        DeletePreAuthRule::class,
        GeneratePreAuthToken::class,
        GetPreAuthTokens::class,
        RevokePreAuthToken::class,
        AddGroupApprovalRule::class,
        GetGroupApprovalRules::class,
        DeleteGroupApprovalRule::class
    ]
)
class Operate : Runnable {
    override fun run() {
        // Implementation for the operate command
    }
}