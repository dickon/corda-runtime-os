@file:Suppress("TooManyFunctions")

package net.corda.tracing

import io.javalin.core.JavalinConfig
import net.corda.messaging.api.processor.StateAndEventProcessor
import net.corda.messaging.api.records.EventLogRecord
import net.corda.messaging.api.records.Record
import net.corda.tracing.impl.BraveTracingService
import net.corda.tracing.impl.TracingState
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.eclipse.jetty.servlet.FilterHolder
import java.util.EnumSet
import java.util.concurrent.ExecutorService
import javax.servlet.DispatcherType

/**
 * Configures the tracing for a given Corda worker.
 * If the zipkin host parameter is not set then tracing will be disabled
 *
 * @param serviceName The service name that will be displayed in dashboards.
 * @param zipkinHost The url of the zipkin host the trace data will be sent to. The value should include a port number
 * if the server is listening on the default 9411 port. Example value: http://localhost:9411
 *
 */
fun configureTracing(serviceName: String, zipkinHost: String?) {
    if (zipkinHost.isNullOrEmpty()) {
        return
    }

    TracingState.currentTraceService = BraveTracingService(serviceName, zipkinHost)
}

fun wrapWithTracingExecutor(executor: ExecutorService): ExecutorService {
    return TracingState.currentTraceService.wrapWithTracingExecutor(executor)
}

fun <K, V> wrapWithTracingConsumer(kafkaConsumer: Consumer<K, V>): Consumer<K, V> {
    return TracingState.currentTraceService.wrapWithTracingConsumer(kafkaConsumer)
}

fun <K, V> wrapWithTracingProducer(kafkaProducer: Producer<K, V>): Producer<K, V> {
    return TracingState.currentTraceService.wrapWithTracingProducer(kafkaProducer)
}

fun traceBatch(operationName: String): BatchRecordTracer {
    return TracingState.currentTraceService.traceBatch(operationName)
}

fun <R> trace(operationName: String, processingBlock: TraceContext.() -> R): R {
    return TracingState.currentTraceService.nextSpan(operationName, processingBlock)
}

fun addTraceContextToRecords(records: List<Record<*, *>>): List<Record<*, *>> = records.map(::addTraceContextToRecord)

fun addTraceContextToRecord(it: Record<*, *>): Record<out Any, out Any> {
    return it.copy(headers = TracingState.currentTraceService.addTraceHeaders(it.headers))
}

fun traceEventProcessing(
    event: Record<*, *>,
    operationName: String,
    processingBlock: () -> List<Record<*, *>>
): List<Record<*, *>> {
    return TracingState.currentTraceService.nextSpan(operationName, event) {
        addTraceContextToRecords(processingBlock())
    }
}

fun traceEventProcessing(
    event: EventLogRecord<*, *>,
    operationName: String,
    processingBlock: () -> List<Record<*, *>>
): List<Record<*, *>> {
    return TracingState.currentTraceService.nextSpan(operationName, event) {
        addTraceContextToRecords(processingBlock())
    }
}

fun traceEventProcessingNullableSingle(
    event: Record<*, *>,
    operationName: String,
    processingBlock: () -> Record<*, *>?
): Record<*, *>? {
    return TracingState.currentTraceService.nextSpan(operationName, event) {
        processingBlock()?.let { addTraceContextToRecord(it) }
    }
}

fun traceEventProcessingSingle(
    event: Record<*, *>,
    operationName: String,
    processingBlock: () -> Record<*, *>
): Record<*, *> {
    return TracingState.currentTraceService.nextSpan(operationName, event) {
        addTraceContextToRecord(processingBlock())
    }
}

fun <K : Any, S : Any, V : Any> traceStateAndEventExecution(
    event: Record<K, V>,
    operationName: String,
    processingBlock: () -> StateAndEventProcessor.Response<S>
): StateAndEventProcessor.Response<S> {
    return TracingState.currentTraceService.nextSpan(operationName, event) {
        val result = processingBlock()
        result.copy(responseEvents = addTraceContextToRecords(result.responseEvents))
    }
}

/**
 * Close tracing system, flushing buffers before shutdown.
 *
 * Call this method to avoid losing events at shutdown.
 */
fun shutdownTracing() {
    TracingState.currentTraceService.close()
}

/**
 * Configure Javalin to read trace IDs from requests or generate new ones if missing.
 */
fun configureJavalinForTracing(config: JavalinConfig) {
    config.configureServletContextHandler { sch ->
        sch.addFilter(
            FilterHolder(TracingState.currentTraceService.getTracedServletFilter()),
            "/*",
            EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST)
        )
    }
}
