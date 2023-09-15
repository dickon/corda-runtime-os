package net.corda.flow.application.services.impl.interop.facade

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.corda.v5.application.interop.facade.Facade
import net.corda.v5.application.interop.facade.FacadeId
import net.corda.v5.application.interop.facade.FacadeMethod
import net.corda.v5.application.interop.facade.FacadeRequest
import net.corda.v5.application.interop.facade.FacadeResponse
import net.corda.v5.application.interop.parameters.TypedParameter
import net.corda.v5.application.interop.parameters.TypedParameterValue

/**
 * A [FacadeResponseImpl] is a response to a [FacadeRequest] to invoke a [FacadeMethod] on a [Facade].
 * @param methodName The method that was invoked.
 * @param outParameters The values of the out parameters of the method.
 */
@JsonSerialize(using = FacadeResponseSerializer::class)
@JsonDeserialize(using = FacadeResponseDeserializer::class)
data class FacadeResponseImpl(
    private val facadeId: FacadeId,
    private val methodName: String,
    private val outParameters: List<TypedParameterValue<*>>
) : FacadeResponse {

    private val outParametersByName = outParameters.associateBy { it.parameter.name }
    override fun getFacadeId(): FacadeId {
        return facadeId
    }

    override fun getMethodName(): String {
        return methodName
    }

    override fun getOutParameters(): List<TypedParameterValue<*>> {
        return outParameters
    }

    /**
     * Get the value of an out parameter.
     * @param parameter The parameter to get the value of.
     */
    @Suppress("UNCHECKED_CAST")
    override operator fun <T : Any> get(parameter: TypedParameter<T>): T {
        val value = outParametersByName[parameter.name]
            ?: throw IllegalArgumentException("No value for parameter ${parameter.name}")

        return (value as? TypedParameterValue<T>)?.value
            ?: throw IllegalArgumentException("Value for parameter ${parameter.name} is of the wrong type")
    }
}