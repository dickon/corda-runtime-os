package net.corda.cli.plugins.network

import picocli.CommandLine.Command

@Command(
    name = "operate",
    description = [
        "MGM operations for managing application networks"
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
        DeleteGroupApprovalRule::class,
        GetRegistrationsRequest::class,
        ApproveRegistrationRequest::class,
        DeclineRegistrationRequest::class,
        ActivateMember::class,
        SuspendMember::class
    ]
)
class Operate