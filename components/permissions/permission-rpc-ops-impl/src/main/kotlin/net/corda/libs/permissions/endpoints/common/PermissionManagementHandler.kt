package net.corda.libs.permissions.endpoints.common

import java.util.concurrent.TimeoutException
import net.corda.httprpc.exception.ExceptionDetails
import net.corda.httprpc.exception.InternalServerException
import net.corda.httprpc.exception.InvalidInputDataException
import net.corda.httprpc.exception.ResourceNotFoundException
import net.corda.libs.permissions.common.exception.EntityAlreadyExistsException
import net.corda.libs.permissions.common.exception.EntityAssociationAlreadyExistsException
import net.corda.libs.permissions.common.exception.EntityAssociationDoesNotExistException
import net.corda.libs.permissions.common.exception.EntityNotFoundException
import net.corda.libs.permissions.manager.PermissionManager
import net.corda.libs.permissions.manager.exception.UnexpectedPermissionResponseException
import net.corda.libs.permissions.manager.exception.RemotePermissionManagementException
import net.corda.messaging.api.exception.CordaRPCAPIResponderException
import net.corda.messaging.api.exception.CordaRPCAPISenderException
import org.slf4j.Logger

@Suppress("ComplexMethod", "ThrowsCount")
fun <T : Any> withPermissionManager(
    permissionManager: PermissionManager,
    logger: Logger,
    block: PermissionManager.() -> T?
): T? {
    return try {
        block.invoke(permissionManager)
    } catch (e: UnexpectedPermissionResponseException) {
        logger.warn("Permission manager received an unexpected response: ${e::class.java.name}: ${e.message}")
        throw InternalServerException(
            exceptionDetails = buildExceptionDetails(e)
        )
    } catch (e: RemotePermissionManagementException) {
        logger.warn("Remote permission management error: ${e.exceptionType}: ${e.message}")
        when (e.exceptionType) {
            EntityNotFoundException::class.java.name -> throw ResourceNotFoundException(e.message!!)
            EntityAssociationDoesNotExistException::class.java.name -> throw InvalidInputDataException(e.message!!)
            EntityAssociationAlreadyExistsException::class.java.name -> throw InvalidInputDataException(e.message!!)
            EntityAlreadyExistsException::class.java.name -> throw InvalidInputDataException(e.message!!)
            else -> throw InternalServerException(
                exceptionDetails = buildExceptionDetails(e.exceptionType, e.message ?: "Remote permission management error occurred.")
            )
        }
    } catch (e: CordaRPCAPISenderException) {
        logger.warn("Error during sending of permission management request.", e)
        throw InternalServerException(
            exceptionDetails = buildExceptionDetails(e.cause ?: e)
        )
    } catch (e: CordaRPCAPIResponderException) {
        logger.warn("Permission manager received error from responder: ${e.message}", e.cause)
        throw InternalServerException(
            exceptionDetails = buildExceptionDetails(e.cause ?: e)
        )
    } catch (e: TimeoutException) {
        logger.warn("Permission management operation timed out.", e)
        throw InternalServerException("Permission management operation timed out.")
    } catch (e: Exception) {
        logger.warn("Unexpected error during permission management operation.", e)
        throw InternalServerException(
            "Unexpected permission management error occurred.",
            exceptionDetails = ExceptionDetails(e::class.java.name, e.message ?: "")
        )
    }
}

private fun buildExceptionDetails(e: Throwable) = ExceptionDetails(e::class.java.name, e.message)
private fun buildExceptionDetails(type: String, reason: String) = ExceptionDetails(type, reason)