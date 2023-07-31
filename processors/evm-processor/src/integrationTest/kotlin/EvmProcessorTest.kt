import net.corda.data.interop.evm.EvmRequest
import net.corda.data.interop.evm.EvmResponse
import net.corda.processors.evm.internal.EVMOpsProcessor
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture


class EvmProcessorTest {

    @Test
    fun `Test a function balance call`(){
        val processor = EVMOpsProcessor()
        // Define this
        val queryBalanceForAddress = "0x70a08231000000000000000000000000bd820e71b2d7e09de2391e9abd395d5e9d9630bb"

        val evmRequest = EvmRequest(
            "RandomFlowId",
            "0x3f6FDbeb1649a5Aca8Fc302e36D1Ba8D12E0Acb5",
            "http://127.0.0.1:8545",
            "0",
            false,
            queryBalanceForAddress
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        val returnedResponse = evmResponse.get()
        println("Returned Response ${returnedResponse}")
    }


    @Test
    fun `Test the transfer of an amount`(){
        val processor = EVMOpsProcessor()

        val transfer100Encoded = "0xa9059cbb0000000000000000000000001a26cd80b83491c948b264c4a04c7324cbde95970000000000000000000000000000000000000000000000000000000000000064"

        val evmRequest = EvmRequest(
            "RandomFlowId",
            "0x3f6FDbeb1649a5Aca8Fc302e36D1Ba8D12E0Acb5",
            "http://127.0.0.1:8545",
            "0",
            true,
            transfer100Encoded
        )

        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        val returnedResponse = evmResponse.get()
        println("Returned Response ${returnedResponse}")
    }



}