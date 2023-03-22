package net.corda.crypto.persistence.impl.tests.infra

import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.StartEvent
import org.mockito.kotlin.mock

// Note, this is really testing AbstractComponent and DependenciesTracker
class TestCryptoLifecycle(
    coordinatorFactory: LifecycleCoordinatorFactory,
    name: LifecycleCoordinatorName,
    val _mock: Lifecycle = mock()
) : Lifecycle by _mock {
    val lifecycleCoordinator = coordinatorFactory.createCoordinator(
        name
    ) { e, c ->
        if (e is StartEvent) {
            c.updateStatus(LifecycleStatus.UP)
        }
    }

    override val isRunning: Boolean
        get() = lifecycleCoordinator.isRunning

    override fun start() {
        lifecycleCoordinator.start()
    }

    override fun stop() {
        lifecycleCoordinator.stop()
    }
}