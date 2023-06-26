package net.corda.cli.plugin.initialconfig

import com.typesafe.config.ConfigRenderOptions
import net.corda.db.core.DbPrivilege
import net.corda.libs.configuration.datamodel.DbConnectionConfig
import net.corda.libs.configuration.helper.VaultSecretConfigGenerator
import net.corda.libs.configuration.secret.EncryptionSecretsServiceImpl
import net.corda.libs.configuration.secret.SecretsCreateService
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.io.FileWriter
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.UUID

@Command(
    name = "create-db-config",
    description = ["Create the SQL statements to insert the connection manager config for database"]
)
class DbConfigSubcommand : Runnable {
    enum class SecretsServiceType {
        CORDA, VAULT
    }

    @Option(
        names = ["-n", "--name"],
        required = true,
        description = ["Name of the database connection. Required."]
    )
    var connectionName: String? = null

    @Option(
        names = ["-j", "--jdbc-url"],
        required = true,
        description = ["The JDBC URL for the connection. Required."]
    )
    var jdbcUrl: String? = null

    @Option(
        names = ["--jdbc-pool-max-size"],
        description = ["The maximum size for the JDBC connection pool. Defaults to 10"]
    )
    var jdbcPoolMaxSize: Int = 10

    @Option(
        names = ["--jdbc-pool-min-size"],
        description = ["The minimum size for the JDBC connection pool. Defaults to null"]
    )
    var jdbcPoolMinSize: Int? = null

    @Option(
        names = ["--idle-timeout"],
        description = ["The maximum time (in seconds) a connection can stay idle in the pool. Defaults to 120"]
    )
    var idleTimeout: Int = 120

    @Option(
        names = ["--max-lifetime"],
        description = ["The maximum time (in seconds) a connection can stay in the pool. Defaults to 1800"]
    )
    var maxLifetime: Int = 1800

    @Option(
        names = ["--keepalive-time"],
        description = ["The interval time (in seconds) in which connections will be tested for aliveness. Defaults to 0"]
    )
    var keepaliveTime: Int = 0

    @Option(
        names = ["--validation-timeout"],
        description = ["The maximum time (in seconds) that the pool will wait for a connection to be validated as alive. Defaults to 5"]
    )
    var validationTimeout: Int = 5

    @Option(
        names = ["-u", "--user"],
        required = true,
        description = ["User name for the database connection. Required."]
    )
    var username: String? = null

    @Option(
        names = ["-p", "--password"],
        description = ["Password name for the database connection."]
    )
    var password: String? = null

    @Option(
        names = ["-a", "--is-admin"],
        description = ["Whether this is an admin (DDL) connection. Defaults to false"]
    )
    var isAdmin: Boolean = false

    @Option(
        names = ["-d", "--description"],
        description = ["Detailed info on the database connection"]
    )
    var description: String = "Initial configuration - autogenerated by setup script"

    @Option(
        names = ["-s", "--salt"],
        description = ["Salt for the encrypting secrets service. Used only by CORDA type secrets service."]
    )
    var salt: String? = null

    @Option(
        names = ["-e", "--passphrase"],
        description = ["Passphrase for the encrypting secrets service. Used only by CORDA type secrets service."]
    )
    var passphrase: String? = null

    @Option(
        names = ["-l", "--location"],
        description = ["location to write the sql output to"]
    )
    var location: String? = null

    @Option(
        names = ["-v", "--vault-path"],
        description = ["Vault path of the secret located in HashiCorp Vault. Used only by VAULT type secrets service."]
    )
    var vaultPath: String? = null

    @Option(
        names = ["-k", "--key"],
        description = ["Vault key for the secrets service. Used only by VAULT type secrets service."],
        defaultValue = "corda-config-database-password"
    )
    var vaultKey: String = "corda-config-database-password"

    @Option(
        names = ["-t", "--type"],
        description = ["Secrets service type. Valid values: \${COMPLETION-CANDIDATES}. Default: \${DEFAULT-VALUE}. " +
                "CORDA generates a Config snippet based on 'passphrase' and 'salt' which are the same passphrase and " +
                "salt you would pass in at Corda bootstrapping to use built in Corda decryption to hide secrets in the Config. " +
                "VAULT generates a configuration compatible with the HashiCorp Vault Corda addon, available to Corda Enterprise. " +
                "For VAULT Config generation you must supply the 'vault-path' parameter as well as the key of the secret."]
    )
    var type: SecretsServiceType = SecretsServiceType.CORDA

    override fun run() {
        val secretsService: SecretsCreateService = when (type) {
            SecretsServiceType.CORDA -> EncryptionSecretsServiceImpl(
                checkParamPassed(passphrase)
                { "'passphrase' must be set for CORDA type secrets." },
                checkParamPassed(salt)
                { "'salt' must be set for CORDA type secrets." })

            SecretsServiceType.VAULT -> VaultSecretConfigGenerator(
                checkParamPassed(vaultPath)
                { "'vaultPath' must be set for VAULT type secrets." })
        }

        val value = when (type) {
            SecretsServiceType.CORDA -> checkParamPassed(password)
                { "'password' must be set for CORDA type secrets." }
            SecretsServiceType.VAULT -> checkParamPassed(vaultKey)
                { "'vaultPath' must be set for VAULT type secrets." }
        }

        val dbConnectionConfig = DbConnectionConfig(
            id = UUID.randomUUID(),
            name = connectionName!!,
            privilege = if (isAdmin) DbPrivilege.DDL else DbPrivilege.DML,
            updateTimestamp = Instant.now(),
            updateActor = "Setup Script",
            description = description,
            config = createConfigDbConfig(
                jdbcUrl!!,
                username!!,
                value,
                vaultKey,
                jdbcPoolMaxSize,
                jdbcPoolMinSize,
                idleTimeout,
                maxLifetime,
                keepaliveTime,
                validationTimeout,
                secretsService
            )
        ).also { it.version = 0 }


        val output = dbConnectionConfig.toInsertStatement()

        if (location == null) {
            println(output)
        } else {
            FileWriter(File("${location!!.removeSuffix("/")}/db-config.sql")).run {
                write(output)
                flush()
                close()
            }
        }
    }

    private inline fun checkParamPassed(value: String?, lazyMessage: () -> String) = if (value.isNullOrBlank()) {
        throw IllegalArgumentException(lazyMessage())
    } else {
        value
    }
}

/**
 * Generate a JSON config string for the config database.
 *
 * @param jdbcUrl URL for the database
 * @param usernmae
 * @param value
 * @param jdcbPoolMaxSize
 * @param secretsService a factory that can produce representations of secrets
 * @return a string containing a JSON config
 *
 */
@Suppress("LongParameterList")
private fun createConfigDbConfig(
    jdbcUrl: String,
    username: String,
    value: String,
    key: String,
    jdbcPoolMaxSize: Int,
    jdbcPoolMinSize: Int?,
    idleTimeout: Int,
    maxLifetime: Int,
    keepaliveTime: Int,
    validationTimeout: Int,
    secretsService: SecretsCreateService,
): String {
    return "{\"database\":{" +
            "\"jdbc\":" +
            "{\"url\":\"$jdbcUrl\"}," +
            "\"pass\":${createSecureConfig(secretsService, value, key)}," +
            "\"user\":\"$username\"," +
            "\"pool\":" +
            "{\"max_size\":$jdbcPoolMaxSize," +
            if (jdbcPoolMinSize != null) { "\"min_size\":$jdbcPoolMinSize," } else { "" } +
            "\"idleTimeoutSeconds\":$idleTimeout," +
            "\"maxLifetimeSeconds\":$maxLifetime," +
            "\"keepaliveTimeSeconds\":$keepaliveTime," +
            "\"validationTimeoutSeconds\":$validationTimeout}}}"
}

fun createSecureConfig(secretsService: SecretsCreateService, value: String, key: String): String {
    return secretsService.createValue(value, key).root().render(ConfigRenderOptions.concise())
}
