package net.corda.tracing.impl

import brave.Span
import brave.Tracer
import brave.propagation.TraceContext
import net.corda.tracing.BatchPublishTracing

class BraveBatchPublishTracing(
    private val clientId: String,
    private val tracer: Tracer,
    private val tracingContextExtractor: TraceContext.Extractor<List<Pair<String, String>>>,
) : BatchPublishTracing {
    private val batchSpans = mutableListOf<Span>()

    override fun begin(recordHeaders: List<List<Pair<String,String>>>) {
        // Ensure any repeat calls to begin abandon any existing spans
        batchSpans.forEach { it.abandon() }
        batchSpans.clear()

        // We are going to group all the messages in the batch by their trace id (if they have one)
        // We then create a span for each sub batch to ensure we maintain the parent child relationships of the trace as
        // the incoming batch can contain messages with different trace contexts.
        batchSpans.addAll(
            recordHeaders.mapNotNull { headers ->
                val extracted = tracingContextExtractor.extract(headers)
                if (extracted.context() == null) {
                    null
                } else {
                    extracted
                }
            }.groupBy { ctx ->
                ctx.context().traceId()
            }.map { grp ->
                tracer.nextSpan(grp.value.first())
                    .name("Send Batch - $clientId")
                    .tag("send.batch.size", grp.value.size.toString())
                    .tag("send.batch.parent.size", recordHeaders.size.toString())
                    .start()
            }
        )
    }

    override fun complete() {
        batchSpans.forEach { it.finish() }
    }

    override fun abort() {
        batchSpans.forEach { it.abandon() }
    }
}