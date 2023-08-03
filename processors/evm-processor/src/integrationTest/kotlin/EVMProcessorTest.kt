import net.corda.data.interop.evm.EvmRequest
import net.corda.data.interop.evm.EvmResponse
import net.corda.processors.evm.internal.EVMOpsProcessor
import net.corda.v5.base.exceptions.CordaRuntimeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import org.web3j.EVMTest
import org.web3j.NodeType
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.ExecutionException


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EVMTest(type= NodeType.BESU)
class EvmProcessorTest {

    private lateinit var contractAddress: String;
    private val ERC20ByteCode = "0x60806040523480156200001157600080fd5b506040518060400160405280600581526020017f546f6b656e0000000000000000000000000000000000000000000000000000008152506040518060400160405280600381526020017f544b4e000000000000000000000000000000000000000000000000000000000081525081600390816200008f919062000527565b508060049081620000a1919062000527565b505050620000e633620000b96200012d60201b60201c565b60ff16600a620000ca919062000791565b620f4240620000da9190620007e2565b6200013660201b60201c565b33600560006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555062000919565b60006012905090565b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603620001a8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016200019f906200088e565b60405180910390fd5b620001bc60008383620002a360201b60201c565b8060026000828254620001d09190620008b0565b92505081905550806000808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825401925050819055508173ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef83604051620002839190620008fc565b60405180910390a36200029f60008383620002a860201b60201c565b5050565b505050565b505050565b600081519050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b600060028204905060018216806200032f57607f821691505b602082108103620003455762000344620002e7565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b600060088302620003af7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8262000370565b620003bb868362000370565b95508019841693508086168417925050509392505050565b6000819050919050565b6000819050919050565b60006200040862000402620003fc84620003d3565b620003dd565b620003d3565b9050919050565b6000819050919050565b6200042483620003e7565b6200043c62000433826200040f565b8484546200037d565b825550505050565b600090565b6200045362000444565b6200046081848462000419565b505050565b5b8181101562000488576200047c60008262000449565b60018101905062000466565b5050565b601f821115620004d757620004a1816200034b565b620004ac8462000360565b81016020851015620004bc578190505b620004d4620004cb8562000360565b83018262000465565b50505b505050565b600082821c905092915050565b6000620004fc60001984600802620004dc565b1980831691505092915050565b6000620005178383620004e9565b9150826002028217905092915050565b6200053282620002ad565b67ffffffffffffffff8111156200054e576200054d620002b8565b5b6200055a825462000316565b620005678282856200048c565b600060209050601f8311600181146200059f57600084156200058a578287015190505b62000596858262000509565b86555062000606565b601f198416620005af866200034b565b60005b82811015620005d957848901518255600182019150602085019450602081019050620005b2565b86831015620005f95784890151620005f5601f891682620004e9565b8355505b6001600288020188555050505b505050505050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b60008160011c9050919050565b6000808291508390505b60018511156200069c578086048111156200067457620006736200060e565b5b6001851615620006845780820291505b808102905062000694856200063d565b945062000654565b94509492505050565b600082620006b757600190506200078a565b81620006c757600090506200078a565b8160018114620006e05760028114620006eb5762000721565b60019150506200078a565b60ff8411156200070057620006ff6200060e565b5b8360020a9150848211156200071a57620007196200060e565b5b506200078a565b5060208310610133831016604e8410600b84101617156200075b5782820a9050838111156200075557620007546200060e565b5b6200078a565b6200076a84848460016200064a565b925090508184048111156200078457620007836200060e565b5b81810290505b9392505050565b60006200079e82620003d3565b9150620007ab83620003d3565b9250620007da7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8484620006a5565b905092915050565b6000620007ef82620003d3565b9150620007fc83620003d3565b92508282026200080c81620003d3565b915082820484148315176200082657620008256200060e565b5b5092915050565b600082825260208201905092915050565b7f45524332303a206d696e7420746f20746865207a65726f206164647265737300600082015250565b600062000876601f836200082d565b915062000883826200083e565b602082019050919050565b60006020820190508181036000830152620008a98162000867565b9050919050565b6000620008bd82620003d3565b9150620008ca83620003d3565b9250828201905080821115620008e557620008e46200060e565b5b92915050565b620008f681620003d3565b82525050565b6000602082019050620009136000830184620008eb565b92915050565b61131280620009296000396000f3fe608060405234801561001057600080fd5b50600436106100b45760003560e01c806370a082311161007157806370a08231146101a357806395d89b41146101d3578063a457c2d7146101f1578063a9059cbb14610221578063dd62ed3e14610251578063fc98ae1114610281576100b4565b806306fdde03146100b9578063095ea7b3146100d757806318160ddd1461010757806323b872dd14610125578063313ce567146101555780633950935114610173575b600080fd5b6100c16102a1565b6040516100ce9190610ba2565b60405180910390f35b6100f160048036038101906100ec9190610c5d565b610333565b6040516100fe9190610cb8565b60405180910390f35b61010f610356565b60405161011c9190610ce2565b60405180910390f35b61013f600480360381019061013a9190610cfd565b610360565b60405161014c9190610cb8565b60405180910390f35b61015d61038f565b60405161016a9190610d6c565b60405180910390f35b61018d60048036038101906101889190610c5d565b610398565b60405161019a9190610cb8565b60405180910390f35b6101bd60048036038101906101b89190610d87565b6103cf565b6040516101ca9190610ce2565b60405180910390f35b6101db610417565b6040516101e89190610ba2565b60405180910390f35b61020b60048036038101906102069190610c5d565b6104a9565b6040516102189190610cb8565b60405180910390f35b61023b60048036038101906102369190610c5d565b610520565b6040516102489190610cb8565b60405180910390f35b61026b60048036038101906102669190610db4565b610543565b6040516102789190610ce2565b60405180910390f35b6102896105ca565b60405161029893929190610e03565b60405180910390f35b6060600380546102b090610e70565b80601f01602080910402602001604051908101604052809291908181526020018280546102dc90610e70565b80156103295780601f106102fe57610100808354040283529160200191610329565b820191906000526020600020905b81548152906001019060200180831161030c57829003601f168201915b5050505050905090565b60008061033e610635565b905061034b81858561063d565b600191505092915050565b6000600254905090565b60008061036b610635565b9050610378858285610806565b610383858585610892565b60019150509392505050565b60006012905090565b6000806103a3610635565b90506103c48185856103b58589610543565b6103bf9190610ed0565b61063d565b600191505092915050565b60008060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b60606004805461042690610e70565b80601f016020809104026020016040519081016040528092919081815260200182805461045290610e70565b801561049f5780601f106104745761010080835404028352916020019161049f565b820191906000526020600020905b81548152906001019060200180831161048257829003601f168201915b5050505050905090565b6000806104b4610635565b905060006104c28286610543565b905083811015610507576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104fe90610f76565b60405180910390fd5b610514828686840361063d565b60019250505092915050565b60008061052b610635565b9050610538818585610892565b600191505092915050565b6000600160008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b6000806060600560009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1660646040518060400160405280600b81526020017f68656c6c6f20776f726c64000000000000000000000000000000000000000000815250925092509250909192565b600033905090565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff16036106ac576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106a390611008565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff160361071b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016107129061109a565b60405180910390fd5b80600160008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925836040516107f99190610ce2565b60405180910390a3505050565b60006108128484610543565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff811461088c578181101561087e576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161087590611106565b60405180910390fd5b61088b848484840361063d565b5b50505050565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610901576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016108f890611198565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610970576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109679061122a565b60405180910390fd5b61097b838383610b08565b60008060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905081811015610a01576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109f8906112bc565b60405180910390fd5b8181036000808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550816000808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825401925050819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef84604051610aef9190610ce2565b60405180910390a3610b02848484610b0d565b50505050565b505050565b505050565b600081519050919050565b600082825260208201905092915050565b60005b83811015610b4c578082015181840152602081019050610b31565b60008484015250505050565b6000601f19601f8301169050919050565b6000610b7482610b12565b610b7e8185610b1d565b9350610b8e818560208601610b2e565b610b9781610b58565b840191505092915050565b60006020820190508181036000830152610bbc8184610b69565b905092915050565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000610bf482610bc9565b9050919050565b610c0481610be9565b8114610c0f57600080fd5b50565b600081359050610c2181610bfb565b92915050565b6000819050919050565b610c3a81610c27565b8114610c4557600080fd5b50565b600081359050610c5781610c31565b92915050565b60008060408385031215610c7457610c73610bc4565b5b6000610c8285828601610c12565b9250506020610c9385828601610c48565b9150509250929050565b60008115159050919050565b610cb281610c9d565b82525050565b6000602082019050610ccd6000830184610ca9565b92915050565b610cdc81610c27565b82525050565b6000602082019050610cf76000830184610cd3565b92915050565b600080600060608486031215610d1657610d15610bc4565b5b6000610d2486828701610c12565b9350506020610d3586828701610c12565b9250506040610d4686828701610c48565b9150509250925092565b600060ff82169050919050565b610d6681610d50565b82525050565b6000602082019050610d816000830184610d5d565b92915050565b600060208284031215610d9d57610d9c610bc4565b5b6000610dab84828501610c12565b91505092915050565b60008060408385031215610dcb57610dca610bc4565b5b6000610dd985828601610c12565b9250506020610dea85828601610c12565b9150509250929050565b610dfd81610be9565b82525050565b6000606082019050610e186000830186610df4565b610e256020830185610cd3565b8181036040830152610e378184610b69565b9050949350505050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b60006002820490506001821680610e8857607f821691505b602082108103610e9b57610e9a610e41565b5b50919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610edb82610c27565b9150610ee683610c27565b9250828201905080821115610efe57610efd610ea1565b5b92915050565b7f45524332303a2064656372656173656420616c6c6f77616e63652062656c6f7760008201527f207a65726f000000000000000000000000000000000000000000000000000000602082015250565b6000610f60602583610b1d565b9150610f6b82610f04565b604082019050919050565b60006020820190508181036000830152610f8f81610f53565b9050919050565b7f45524332303a20617070726f76652066726f6d20746865207a65726f2061646460008201527f7265737300000000000000000000000000000000000000000000000000000000602082015250565b6000610ff2602483610b1d565b9150610ffd82610f96565b604082019050919050565b6000602082019050818103600083015261102181610fe5565b9050919050565b7f45524332303a20617070726f766520746f20746865207a65726f20616464726560008201527f7373000000000000000000000000000000000000000000000000000000000000602082015250565b6000611084602283610b1d565b915061108f82611028565b604082019050919050565b600060208201905081810360008301526110b381611077565b9050919050565b7f45524332303a20696e73756666696369656e7420616c6c6f77616e6365000000600082015250565b60006110f0601d83610b1d565b91506110fb826110ba565b602082019050919050565b6000602082019050818103600083015261111f816110e3565b9050919050565b7f45524332303a207472616e736665722066726f6d20746865207a65726f20616460008201527f6472657373000000000000000000000000000000000000000000000000000000602082015250565b6000611182602583610b1d565b915061118d82611126565b604082019050919050565b600060208201905081810360008301526111b181611175565b9050919050565b7f45524332303a207472616e7366657220746f20746865207a65726f206164647260008201527f6573730000000000000000000000000000000000000000000000000000000000602082015250565b6000611214602383610b1d565b915061121f826111b8565b604082019050919050565b6000602082019050818103600083015261124381611207565b9050919050565b7f45524332303a207472616e7366657220616d6f756e742065786365656473206260008201527f616c616e63650000000000000000000000000000000000000000000000000000602082015250565b60006112a6602683610b1d565b91506112b18261124a565b604082019050919050565b600060208201905081810360008301526112d581611299565b905091905056fea264697066735822122046485b2d6121952c89b375952d0cea3e43f6d22a4fbb1d5c52e11e6a253164af64736f6c63430008120033"
    private val transferRevertMethod = "0xa9059cbb000000000000000000000000c5973ef0360fcd067dc5db140cd15b7e725c7c1a000000000000000000000000000000000013426172c74d822b878fe800000000"
    private val queryBalanceForAddress = "0x70a08231000000000000000000000000fe3b557e8fb62b89f4916b721be55ceb828dbd73"
    private val transfer100Encoded = "0xa9059cbb0000000000000000000000001a26cd80b83491c948b264c4a04c7324cbde95970000000000000000000000000000000000000000000000000000000000000064"

    @BeforeEach
    fun `Deploy Smart Contract`(){
        val processor = EVMOpsProcessor()
        val evmRequest = EvmRequest(
            "RandomFlowId",
            "",
            "http://127.0.0.1:8545",
            "0",
            true,
            ERC20ByteCode
        )

        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        val returnedResponse = evmResponse.get()
        println(returnedResponse.payload)
        contractAddress = returnedResponse.payload
    }

    // Checking that the balance can be correctly called
    @Test
    fun `Test a function balance call`(){
        val processor = EVMOpsProcessor()
        // Define this
        val evmRequest = EvmRequest(
            "RandomFlowId",
            contractAddress,
            "http://127.0.0.1:8545",
            "0",
            false,
            queryBalanceForAddress
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        val returnedResponse = evmResponse.get()

        assert("0x00000000000000000000000000000000000000000000d3c21bcecceda1000000"==returnedResponse.payload)

    }


    @Test
    fun `Test the transfer function of an ERC20 Contract`(){
        val processor = EVMOpsProcessor()

        val evmRequest = EvmRequest(
            "RandomFlowId",
            contractAddress,
            "http://127.0.0.1:8545",
            "0",
            true,
            transfer100Encoded
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        val returnedResponse = evmResponse.get()
        println("Returned Response ${returnedResponse}")

        // Query the balance
        // Starting
    }

    @Test
    fun `Handle Test To The Wrong RpcUrl`(){
        val processor = EVMOpsProcessor()
        val evmRequest = EvmRequest(
            "RandomFlowId",
            contractAddress,
            "http://127.0.0.1:9545",
            "0",
            true,
            transfer100Encoded
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        try{
            evmResponse.get()
        }catch(e: Throwable){
            assert(e is ExecutionException)
        }
    }


    @Test
    fun `Handle Transfer Revert Method`(){
        val processor = EVMOpsProcessor()
        val evmRequest = EvmRequest(
            "RandomFlowId",
            contractAddress,
            "http://127.0.0.1:8545",
            "0",
            true,
            transferRevertMethod
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)

        val returnedResponse = evmResponse.get()
        println("Returned Response ${returnedResponse}")
        // TODO: Need to decode this result to get valuable input, but at the very least a hash is being returned
    }


    @Test
    fun `Handle Invalid Paramater Input`(){
        val processor = EVMOpsProcessor()
        val evmRequest = EvmRequest(
            "RandomFlowId",
            "Random Text String",
            "http://127.0.0.1:8545",
            "0",
            true,
            transferRevertMethod
        )
        val evmResponse = CompletableFuture<EvmResponse>()
        processor.onNext(evmRequest,evmResponse)
        try {
            evmResponse.get()
        }catch(e: Throwable){
            assert(e is ExecutionException)
        }
    }
}