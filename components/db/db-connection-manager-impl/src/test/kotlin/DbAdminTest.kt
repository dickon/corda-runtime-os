import com.typesafe.config.ConfigFactory
import net.corda.db.connection.manager.DbConnectionManager
import net.corda.db.connection.manager.impl.DbAdminImpl
import net.corda.db.core.DbPrivilege
import net.corda.libs.configuration.SmartConfig
import net.corda.libs.configuration.SmartConfigFactory
import net.corda.libs.configuration.SmartConfigImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.sql.Connection
import java.sql.Statement
import javax.sql.DataSource

class DbAdminTest {
    private val statement = mock<Statement>()
    private val connection = mock<Connection>() {
        on { createStatement() }.doReturn(statement)
    }
    private val dataSource = mock<DataSource>() {
        on { connection }.doReturn(connection)
    }
    private val dbConnectionManager = mock<DbConnectionManager>() {
        on { getClusterDataSource() }.doReturn(dataSource)
    }
    private val secretConfig = SmartConfigImpl(ConfigFactory.empty(), mock(), mock())
    private val config = mock<SmartConfig>()
    private val configFactory = mock<SmartConfigFactory>() {
        on { create(any()) }.doReturn(config)
        on { makeSecret(any()) }.doReturn(secretConfig)
    }

    @Test
    fun `when create DDL user grant all`() {
        val dba = DbAdminImpl(dbConnectionManager)

        dba.createDbAndUser(
            "test",
            "test-schema",
            "test-user",
            "test-password",
            "test-url",
            DbPrivilege.DDL,
            configFactory
        )

        verify(statement).execute(argThat {
            this.contains("CREATE SCHEMA IF NOT EXISTS")
                    && this.contains("CREATE USER test-user WITH PASSWORD 'test-password'")
                    && this.contains("GRANT USAGE ON SCHEMA test-schema to test-user;")
                    && this.contains("GRANT ALL ON SCHEMA") })
    }

    @Test
    fun `when create DB and DDL user grant all`() {
        val dba = DbAdminImpl(dbConnectionManager)

        dba.createDbAndUser(
            "test-schema",
            "test-user",
            "test-password",
            DbPrivilege.DDL
        )

        verify(statement).execute(argThat {
            this.contains("CREATE SCHEMA IF NOT EXISTS")
                    && this.contains("CREATE USER test-user WITH PASSWORD 'test-password'")
                    && this.contains("GRANT USAGE ON SCHEMA test-schema to test-user;")
                    && this.contains("GRANT ALL ON SCHEMA") })
    }

    @Test
    fun `when create DML limited grant`() {
        val dba = DbAdminImpl(dbConnectionManager)

        dba.createDbAndUser(
            "test",
            "test-schema",
            "test-user",
            "test-password",
            "test-url",
            DbPrivilege.DML,
            configFactory
        )

        verify(statement).execute(argThat {
            this.contains("CREATE SCHEMA IF NOT EXISTS")
                    && this.contains("CREATE USER test-user WITH PASSWORD 'test-password'")
                    && this.contains("GRANT USAGE ON SCHEMA test-schema to test-user;")
                    && this.contains("ALTER DEFAULT PRIVILEGES IN SCHEMA test-schema GRANT SELECT, UPDATE, INSERT, DELETE ON TABLES") })
    }

    @Test
    fun `when create DML without grantee provided then limited grant`() {
        val dba = DbAdminImpl(dbConnectionManager)

        dba.createDbAndUser(
            "test-schema",
            "test-user",
            "test-password",
            DbPrivilege.DML
        )

        verify(statement).execute(argThat {
            this.contains("CREATE SCHEMA IF NOT EXISTS")
                    && this.contains("CREATE USER test-user WITH PASSWORD 'test-password'")
                    && this.contains("GRANT USAGE ON SCHEMA test-schema to test-user;")
                    && this.contains("ALTER DEFAULT PRIVILEGES IN SCHEMA test-schema GRANT SELECT, UPDATE, INSERT, DELETE ON TABLES") })
    }

//    @Test
//    fun `when create DML with grantee provided then limited grant`() {
//        val dba = DbAdminImpl(dbConnectionManager)
//
//        dba.createDbAndUser(
//            "test-schema",
//            "test-user",
//            "test-password",
//            DbPrivilege.DML,
//            "test-grantee"
//        )
//
//        verify(statement).execute(argThat {
//            this.contains("CREATE SCHEMA IF NOT EXISTS")
//                    && this.contains("CREATE USER test-user WITH PASSWORD 'test-password'")
//                    && this.contains("GRANT USAGE ON SCHEMA test-schema to test-user;")
//                    && this.contains("ALTER DEFAULT PRIVILEGES FOR ROLE test-grantee IN SCHEMA test-schema " +
//                        "GRANT SELECT, UPDATE, INSERT, DELETE ON TABLES") })
//    }
}