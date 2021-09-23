package net.corda.sandbox.internal.classtag

import net.corda.sandbox.SandboxException
import net.corda.sandbox.internal.CLASS_TAG_DELIMITER
import net.corda.sandbox.internal.CLASS_TAG_IDENTIFIER_IDX
import net.corda.sandbox.internal.CLASS_TAG_VERSION_IDX
import net.corda.sandbox.internal.ClassTagV1
import net.corda.v5.crypto.SecureHash

/** Implements [EvolvableTag]. */
internal class EvolvableTagImplV1(
    isPlatformClass: Boolean,
    classBundleName: String,
    cordappBundleName: String,
    cpkSignerSummaryHash: SecureHash
) : EvolvableTag(1, isPlatformClass, classBundleName, cordappBundleName, cpkSignerSummaryHash) {

    companion object {
        private const val ENTRIES_LENGTH = 6
        private const val IS_PLATFORM_CLASS_IDX = 2
        private const val CLASS_BUNDLE_NAME_IDX = 3
        private const val CORDAPP_BUNDLE_NAME_IDX = 4
        private const val CPK_PUBLIC_KEY_HASHES_IDX = 5

        /** Deserialises an [EvolvableTagImplV1] class tag. */
        fun deserialise(classTagEntries: List<String>): EvolvableTagImplV1 {
            if (classTagEntries.size != ENTRIES_LENGTH) throw SandboxException(
                "Serialised evolvable class tag contained ${classTagEntries.size} entries, whereas $ENTRIES_LENGTH " +
                        "entries were expected. The entries were $classTagEntries."
            )

            val isPlatformClass = classTagEntries[IS_PLATFORM_CLASS_IDX].toBoolean()

            val cpkSignerSummaryHashString = classTagEntries[CPK_PUBLIC_KEY_HASHES_IDX]
            val cpkSignerSummaryHash = try {
                SecureHash.create(cpkSignerSummaryHashString)
            } catch (e: IllegalArgumentException) {
                throw SandboxException(
                    "Couldn't parse hash $cpkSignerSummaryHashString in serialised evolvable class tag.", e
                )
            }

            return EvolvableTagImplV1(
                isPlatformClass,
                classTagEntries[CLASS_BUNDLE_NAME_IDX],
                classTagEntries[CORDAPP_BUNDLE_NAME_IDX],
                cpkSignerSummaryHash
            )
        }
    }

    override fun serialise(): String {
        // This approach - of allocating an array of the expected length and adding the entries by index - is designed
        // to minimise errors where serialisation and deserialisation retrieve entries from different indices.
        val entries = arrayOfNulls<Any>(ENTRIES_LENGTH)

        entries[CLASS_TAG_IDENTIFIER_IDX] = ClassTagV1.EVOLVABLE_IDENTIFIER
        entries[CLASS_TAG_VERSION_IDX] = version
        entries[IS_PLATFORM_CLASS_IDX] = isPlatformClass
        entries[CLASS_BUNDLE_NAME_IDX] = classBundleName
        entries[CORDAPP_BUNDLE_NAME_IDX] = cordappBundleName
        entries[CPK_PUBLIC_KEY_HASHES_IDX] = cpkSignerSummaryHash

        return entries.joinToString(CLASS_TAG_DELIMITER)
    }
}