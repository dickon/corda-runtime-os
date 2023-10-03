package net.corda.applications.workers.workercommon

import java.nio.file.Path
import net.corda.schema.configuration.BootConfig
import picocli.CommandLine.Option

/** The startup parameters handled by all workers. */
class DefaultWorkerParams(healthPortOverride: Int = WORKER_SERVER_PORT) {
    @Option(names = ["-h", "--help"], usageHelp = true, description = ["Display help and exit."])
    var helpRequested = false

    @Option(names = ["-v", "--version"], description = ["Display version and exit."])
    var versionRequested = false

    @Option(
        names = ["-i", "--instance-id"],
        description = ["The Kafka instance ID for this worker. Defaults to a random value."]
    )
    var instanceId: Int? = null

    @Option(
        names = ["-t", "--topic-prefix"],
        description = ["The prefix to use for Kafka topics. Defaults to the empty string."]
    )
    // This needs revision as arguably it belongs to the `messagingParams`
    var topicPrefix : String? = null

    // This needs revision as arguably it belongs to the `messagingParams`. Defaulting to 1MB to match kafkas default and our config
    // schema default
    @Option(
        names = ["-M", "--max-allowed-message-size"],
        description = ["The maximum message size in bytes allowed to be sent to the message bus."]
    )
    var maxAllowedMessageSize: Int? = null

    @Option(
        names = ["-p", "--worker-server-port"],
        description = ["The port the worker http server should listen on. Defaults to $WORKER_SERVER_PORT."]
    )
    var workerServerPort = healthPortOverride

    @Option(names = ["-m", "--messaging-params"], description = ["Messaging parameters for the worker."])
    var messaging = emptyMap<String, String>()

    @Option(names = ["-s", "--${BootConfig.BOOT_SECRETS}"], description = ["Secrets parameters for the worker."], required = true)
    var secrets = emptyMap<String, String>()

    @Option(names = ["--workspace-dir"], description = ["Corda workspace directory."])
    var workspaceDir: String? = null

    @Option(names = ["--temp-dir"], description = ["Corda temp directory."])
    var tempDir: String? = null

    @Option(names = ["-a", "--addon"], description = ["Add-on configuration"])
    var addon = emptyMap<String, String>()

    @Option(names = ["-f", "--values"], description = ["Load configuration from a file. " +
            "This configuration is merged in with the configuration set in the command line flags. " +
            "Command line flags win. " +
            "When multiple files are specified, values in the right-most file wins."])
    var configFiles = emptyList<Path>()

    @Option(names = ["--send-trace-to"], description = ["URL of server that accepts Zipkin format traces."])
    var zipkinTraceUrl: String? = null

    @Option(names = ["--trace-samples-per-second"], description = ["Number of request traces to sample per second, " +
            "defaults to 1 sample per second. Set to \"unlimited\" to record all samples"])
    var traceSamplesPerSecond: String? = null

    @Option(
        names = ["--${BootConfig.BOOT_STATE_MANAGER}"],
        description = ["Configuration for the state manager."]
    )
    var stateManagerParams = emptyMap<String, String>()

    @Option(names = ["--worker-endpoint-crypto"], description = ["Internal RPC endpoint for the CryptoWorker"], required = true)
    var cryptoWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-db"], description = ["Internal RPC endpoint for the DBWorker"], required = true)
    var dbWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-flow"], description = ["Internal RPC endpoint for the FlowWorker"], required = true)
    var flowWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-flowMapper"], description = ["Internal RPC endpoint for the FlowMapperWorker"], required = true)
    var flowMapperWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-verification"], description = ["Internal RPC endpoint for the VerificationWorker"], required = true)
    var verificationWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-membership"], description = ["Internal RPC endpoint for the MembershipWorker"], required = true)
    var membershipWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-rest"], description = ["Internal RPC endpoint for the RestWorker"], required = true)
    var restWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-p2pGateway"], description = ["Internal RPC endpoint for the P2PGatewayWorker"], required = true)
    var p2pGatewayWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-p2pLinkManager"], description = ["Internal RPC endpoint for the P2PLinkManagerWorker"], required = true)
    var p2pLinkManagerWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-persistence"], description = ["Internal RPC endpoint for the PersistenceWorker"], required = true)
    var persistenceWorkerEndpoint: String? = null

    @Option(names = ["--worker-endpoint-uniqueness"], description = ["Internal RPC endpoint for the UniquenessWorker"], required = true)
    var uniquenessWorkerEndpoint: String? = null
}