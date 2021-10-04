package net.corda.sandbox.internal.classtag

import net.corda.sandbox.Sandbox
import net.corda.sandbox.SandboxException
import net.corda.sandbox.internal.CLASS_TAG_DELIMITER
import net.corda.sandbox.internal.CLASS_TAG_IDENTIFIER_IDX
import net.corda.sandbox.internal.CLASS_TAG_VERSION_IDX
import net.corda.sandbox.internal.ClassTagV1
import net.corda.sandbox.internal.sandbox.CpkSandboxInternal
import org.osgi.framework.Bundle

/** An implementation of [ClassTagFactory]. */
internal class ClassTagFactoryImpl : ClassTagFactory {
    override fun createSerialised(
        isStaticClassTag: Boolean,
        isPublicBundle: Boolean,
        bundle: Bundle,
        sandbox: Sandbox
    ): String {
        val bundleSymbolicName = bundle.symbolicName ?: throw SandboxException(
            "Bundle at ${bundle.location} does not have a symbolic name, preventing serialisation.")

        if (isPublicBundle) {
            return if (isStaticClassTag) {
                StaticTagImplV1(isPublicClass = true, bundleSymbolicName, ClassTagV1.PLACEHOLDER_HASH)
            } else {
                EvolvableTagImplV1(
                    isPublicClass = true,
                    bundleSymbolicName,
                    ClassTagV1.PLACEHOLDER_CORDAPP_BUNDLE_NAME,
                    ClassTagV1.PLACEHOLDER_HASH
                )
            }.serialise()

        }

        if (sandbox !is CpkSandboxInternal) throw SandboxException("Sandbox was neither a public sandbox nor a " +
                "CPK sandbox. A valid class tag cannot be constructed.")

        return if (isStaticClassTag) {
            StaticTagImplV1(isPublicClass = false, bundleSymbolicName, sandbox.cpk.cpkHash)
        } else {
            EvolvableTagImplV1(
                isPublicClass = false,
                bundleSymbolicName,
                sandbox.cordappBundle.symbolicName,
                sandbox.cpk.id.signerSummaryHash
            )
        }.serialise()
    }

    override fun deserialise(serialisedClassTag: String): ClassTag {
        val entries = serialisedClassTag.split(CLASS_TAG_DELIMITER)
        if (entries.size < 2) throw SandboxException(
            "Serialised class tag only contained ${entries.size} entries, whereas the minimum length is 2. The " +
                    "entries were $entries."
        )

        val type = entries[CLASS_TAG_IDENTIFIER_IDX]
        val versionString = entries[CLASS_TAG_VERSION_IDX]
        val version = versionString.toIntOrNull()
            ?: throw SandboxException(
                "Serialised class tag's version $versionString could not be converted to an integer."
            )

        return when {
            type == ClassTagV1.STATIC_IDENTIFIER && version == 1 -> StaticTagImplV1.deserialise(entries)
            type == ClassTagV1.EVOLVABLE_IDENTIFIER && version == 1 -> EvolvableTagImplV1.deserialise(entries)
            else -> throw SandboxException("Serialised class tag had type $type and version $version. This " +
                    "combination is unknown.")
        }
    }
}