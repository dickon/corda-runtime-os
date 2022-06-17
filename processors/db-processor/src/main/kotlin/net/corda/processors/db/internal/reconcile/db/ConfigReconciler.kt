package net.corda.processors.db.internal.reconcile.db

import net.corda.data.config.Configuration
import net.corda.db.connection.manager.DbConnectionManager
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.reconciliation.Reconciler
import net.corda.reconciliation.ReconcilerFactory
import net.corda.reconciliation.ReconcilerReader
import net.corda.reconciliation.ReconcilerWriter
import net.corda.v5.base.util.contextLogger

class ConfigReconciler(
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    private val dbConnectionManager: DbConnectionManager,
    private val reconcilerFactory: ReconcilerFactory,
    private val reconcilerReader: ReconcilerReader<String, Configuration>,
    private val reconcilerWriter: ReconcilerWriter<String, Configuration>
) : ReconcilerWrapper {
    companion object {
        private val log = contextLogger()
    }

    private var dbReconciler: DbReconcilerReader<String, Configuration>? = null
    private var reconciler: Reconciler? = null

    override fun close() {
        dbReconciler?.close()
        dbReconciler = null
        reconciler?.close()
        reconciler = null
    }

    override fun updateInterval(intervalMillis: Long) {
        log.debug("Config reconciliation interval set to $intervalMillis ms")

        if (dbReconciler == null) {
            dbReconciler =
                DbReconcilerReader(
                    coordinatorFactory,
                    dbConnectionManager,
                    String::class.java,
                    Configuration::class.java,
                    getAllConfigDBVersionedRecords
                ).also {
                    it.start()
                }
        }

        if (reconciler == null) {
            reconciler = reconcilerFactory.create(
                dbReader = dbReconciler!!,
                kafkaReader = reconcilerReader,
                writer = reconcilerWriter,
                keyClass = String::class.java,
                valueClass = Configuration::class.java,
                reconciliationIntervalMs = intervalMillis
            ).also { it.start() }
        } else {
            log.info("Updating Config ${Reconciler::class.java.name}")
            reconciler!!.updateInterval(intervalMillis)
        }
    }
}
