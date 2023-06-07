package net.corda.processors.evm

import net.corda.libs.configuration.SmartConfig

interface EVMProcessor {
    fun start(bootConfig: SmartConfig)

    fun stop()
}