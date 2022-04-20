package net.corda.messaging.stubs

import net.corda.messaging.api.processor.PubSubProcessor
import net.corda.messaging.api.records.Record
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future

class StubPubSubProcessor(
    private val latch: CountDownLatch,
    private val exception: Exception? = null
) : PubSubProcessor<String, ByteBuffer> {
    override fun onNext(event: Record<String, ByteBuffer>): Future<Unit> {
        latch.countDown()

        if (exception != null) {
            return CompletableFuture.failedFuture(exception)
        }

        return CompletableFuture.completedFuture(Unit)
    }

    override val keyClass: Class<String>
        get() = String::class.java
    override val valueClass: Class<ByteBuffer>
        get() = ByteBuffer::class.java
}
