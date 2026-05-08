package cash.z.ecc.android.sdk.internal.model.voting

import androidx.annotation.Keep

@Keep
data class FfiRoundState(
    val roundId: String,
    val phase: Int,
    val snapshotHeight: Long,
    val hotkeyAddress: String?,
    val delegatedWeight: Long?,
    val proofGenerated: Boolean
) {
    val roundPhase: FfiRoundPhase get() = FfiRoundPhase.fromInt(phase)
}

@Suppress("MagicNumber")
enum class FfiRoundPhase(
    val value: Int
) {
    INITIALIZED(0),
    HOTKEY_GENERATED(1),
    DELEGATION_CONSTRUCTED(2),
    DELEGATION_PROVED(3),
    VOTE_READY(4);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: INITIALIZED
    }
}
