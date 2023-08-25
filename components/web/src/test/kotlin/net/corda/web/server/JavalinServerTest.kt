package net.corda.web.server

import io.javalin.Javalin
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.v5.base.exceptions.CordaRuntimeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class JavalinServerTest {

    private val lifecycleCoordinator = org.mockito.kotlin.mock<LifecycleCoordinator>()
    private val lifecycleCoordinatorFactory = org.mockito.kotlin.mock<LifecycleCoordinatorFactory> {
        on { createCoordinator(any(), any()) }.doReturn(lifecycleCoordinator)
    }

    private lateinit var javalinServer: JavalinServer
    private val javalinMock: Javalin = mock()
    private val javalinFactory = mock<JavalinFactory> {
        on { create() }.doReturn(javalinMock)
    }

    private val port = 8888

    @BeforeEach
    fun setup() {
        javalinServer = JavalinServer(lifecycleCoordinatorFactory, javalinFactory)
    }

    @Test
    fun `starting the server should call start on Javalin`() {
        javalinServer.start(port)
        verify(javalinMock).start(port)
    }

    @Test
    fun `stopping the server should call stop on Javalin`() {
        javalinServer.start(port)
        javalinServer.stop()
        verify(javalinMock).stop()
    }

    @Test
    fun `starting an already started server should throw exception`() {
        javalinServer.start(port)
        assertThrows<CordaRuntimeException> {
            javalinServer.start(port)
        }
    }

    @Test
    fun `registering an endpoint with improper endpoint string throws`() {
        javalinServer.start(port)
        assertThrows<CordaRuntimeException> {
            javalinServer.registerHandler(HTTPMethod.GET, "") { c -> c }
        }
        assertThrows<CordaRuntimeException> {
            javalinServer.registerHandler(HTTPMethod.GET, "noslash") { c -> c }
        }
        assertThrows<CordaRuntimeException> {
            javalinServer.registerHandler(HTTPMethod.GET, "not a url") { c -> c }
        }
        assertDoesNotThrow {
            javalinServer.registerHandler(HTTPMethod.GET, "/url") { c -> c }
        }
    }

    @Test
    fun `registering an endpoint should call the correct method on javalin` (){
        javalinServer.start(port)

        javalinServer.registerHandler(HTTPMethod.GET, "/url") { c -> c }
        verify(javalinMock).get(eq("/url"), any())

        javalinServer.registerHandler(HTTPMethod.POST, "/url") { c -> c }
        verify(javalinMock).post(eq("/url"), any())
    }

}