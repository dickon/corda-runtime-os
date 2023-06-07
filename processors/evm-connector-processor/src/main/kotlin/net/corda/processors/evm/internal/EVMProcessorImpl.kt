package net.corda.processors.evm.internal

import net.corda.libs.configuration.SmartConfig
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.createCoordinator
import net.corda.processors.evm.EVMProcessor
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.LoggerFactory

@Component(service = [EVMProcessor::class])
class EVMProcessorImpl @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
) : EVMProcessor {

    private companion object {
        val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val lifecycleCoordinator = coordinatorFactory.createCoordinator<EVMProcessorImpl>(::eventHandler)


    override fun start(bootConfig: SmartConfig) {
        logger.info("EVM processor starting.")
    }

    override fun stop() {
        logger.info("EVM processor stopping.")
    }

    private fun eventHandler(event: LifecycleEvent, coordinator: LifecycleCoordinator) {

    }
}