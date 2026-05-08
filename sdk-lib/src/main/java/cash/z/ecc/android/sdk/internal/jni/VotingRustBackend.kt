package cash.z.ecc.android.sdk.internal.jni

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState

@Keep
@Suppress("TooManyFunctions", "LongParameterList")
internal object VotingRustBackend {
    @JvmStatic
    external fun openVotingDb(dbPath: String): Long

    /** Must be called exactly once; using [dbHandle] after close is undefined behavior. */
    @JvmStatic
    external fun closeVotingDb(dbHandle: Long)

    @JvmStatic
    external fun setWalletId(dbHandle: Long, walletId: String): Boolean

    @JvmStatic
    external fun initRound(
        dbHandle: Long,
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    ): Boolean

    @JvmStatic
    external fun getRoundState(dbHandle: Long, roundId: String): FfiRoundState?

    @JvmStatic
    external fun listRoundsJson(dbHandle: Long): String

    @JvmStatic
    external fun getVotesJson(dbHandle: Long, roundId: String): String

    @JvmStatic
    external fun clearRound(dbHandle: Long, roundId: String): Boolean

    @JvmStatic
    external fun deleteSkippedBundles(
        dbHandle: Long,
        roundId: String,
        keepCount: Int
    ): Long

    @JvmStatic
    external fun computeShareNullifier(
        voteCommitment: ByteArray,
        shareIndex: Int,
        blind: ByteArray
    ): ByteArray?
}
