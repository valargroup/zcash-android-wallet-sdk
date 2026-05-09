package cash.z.ecc.android.sdk.internal.model.voting

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.jni.JNI_HOTKEY_PUBLIC_KEY_BYTES_SIZE
import cash.z.ecc.android.sdk.internal.jni.JNI_HOTKEY_SECRET_KEY_BYTES_SIZE

@Keep
@ConsistentCopyVisibility
data class HotkeySecretKey internal constructor(
    val value: ByteArray
) {
    init {
        require(value.size == JNI_HOTKEY_SECRET_KEY_BYTES_SIZE) {
            "HotkeySecretKey must be $JNI_HOTKEY_SECRET_KEY_BYTES_SIZE bytes, got ${value.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotkeySecretKey) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    // Do not include secret key bytes in logs.
    override fun toString(): String = "HotkeySecretKey(size=${value.size})"

    companion object {
        internal fun new(bytes: ByteArray) = HotkeySecretKey(bytes)
    }
}

@Keep
@ConsistentCopyVisibility
data class HotkeyPublicKey internal constructor(
    val value: ByteArray
) {
    init {
        require(value.size == JNI_HOTKEY_PUBLIC_KEY_BYTES_SIZE) {
            "HotkeyPublicKey must be $JNI_HOTKEY_PUBLIC_KEY_BYTES_SIZE bytes, got ${value.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotkeyPublicKey) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "HotkeyPublicKey(${value.toHexString()})"

    companion object {
        internal fun new(bytes: ByteArray) = HotkeyPublicKey(bytes)
    }
}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

@Keep
@ConsistentCopyVisibility
data class FfiVotingHotkey internal constructor(
    val secretKey: HotkeySecretKey,
    val publicKey: HotkeyPublicKey,
    val address: String
) {
    internal constructor(sk: ByteArray, pk: ByteArray, addr: String) :
        this(HotkeySecretKey.new(sk), HotkeyPublicKey.new(pk), addr)
}

// Must match PHASE_* constants in backend-lib/src/main/rust/voting/json.rs.
internal const val FFI_ROUND_PHASE_INITIALIZED = 0
internal const val FFI_ROUND_PHASE_HOTKEY_GENERATED = 1
internal const val FFI_ROUND_PHASE_DELEGATION_CONSTRUCTED = 2
internal const val FFI_ROUND_PHASE_DELEGATION_PROVED = 3
internal const val FFI_ROUND_PHASE_VOTE_READY = 4

@Keep
data class FfiBundleSetupResult(
    val bundleCount: Int,
    val eligibleWeight: Long,
    val bundleWeights: List<Long> = emptyList()
) {
    internal constructor(bundleCount: Int, eligibleWeight: Long, bundleWeights: LongArray) :
        this(bundleCount, eligibleWeight, bundleWeights.toList())
}

@Keep
data class FfiRoundState(
    val roundId: String,
    val phase: Int,
    val snapshotHeight: Long,
    val hotkeyAddress: String?,
    val delegatedWeight: Long?,
    val proofGenerated: Boolean
) {
    val roundPhase = FfiRoundPhase.fromInt(phase)
}

@Keep
enum class FfiRoundPhase(
    val value: Int
) {
    INITIALIZED(FFI_ROUND_PHASE_INITIALIZED),
    HOTKEY_GENERATED(FFI_ROUND_PHASE_HOTKEY_GENERATED),
    DELEGATION_CONSTRUCTED(FFI_ROUND_PHASE_DELEGATION_CONSTRUCTED),
    DELEGATION_PROVED(FFI_ROUND_PHASE_DELEGATION_PROVED),
    VOTE_READY(FFI_ROUND_PHASE_VOTE_READY);

    companion object {
        fun fromInt(value: Int) =
            entries.firstOrNull { it.value == value }
                ?: error("Unknown round phase: $value")
    }
}

data class FfiRoundSummary(
    val roundId: String,
    val phase: Int,
    val snapshotHeight: Long,
    val createdAt: Long
) {
    val roundPhase = FfiRoundPhase.fromInt(phase)
}

data class VoteRecord(
    val proposalId: Int,
    val bundleIndex: Int,
    val choice: Int,
    val submitted: Boolean
)
