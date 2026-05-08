package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState

@Suppress("TooManyFunctions", "LongParameterList")
interface TypesafeVotingBackend {
    suspend fun openVotingDb(dbPath: String): Long

    /**
     * Must be called exactly once for [dbHandle]; using [dbHandle] after close is undefined behavior.
     */
    suspend fun closeVotingDb(dbHandle: Long)

    suspend fun setWalletId(dbHandle: Long, walletId: String)

    suspend fun initRound(
        dbHandle: Long,
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    )

    suspend fun getRoundState(dbHandle: Long, roundId: String): FfiRoundState?

    suspend fun listRoundsJson(dbHandle: Long): String

    suspend fun getVotes(dbHandle: Long, roundId: String): List<VoteRecord>

    suspend fun clearRound(dbHandle: Long, roundId: String)

    suspend fun deleteSkippedBundles(
        dbHandle: Long,
        roundId: String,
        keepCount: Int
    ): Long
}

data class VoteRecord(
    val proposalId: Int,
    val bundleIndex: Int,
    val choice: Int,
    val submitted: Boolean
)
