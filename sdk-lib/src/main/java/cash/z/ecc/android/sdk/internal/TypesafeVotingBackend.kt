package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.voting.FfiBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.FfiVotingHotkey

@Suppress("TooManyFunctions", "LongParameterList")
interface TypesafeVotingBackend {
    suspend fun openVotingDb(dbPath: String, walletId: String): TypesafeVotingDb

    suspend fun computeBundleSetup(notesJson: String): FfiBundleSetupResult

    suspend fun warmProvingCaches()
}

@Suppress("TooManyFunctions", "LongParameterList")
interface TypesafeVotingDb {
    suspend fun close()

    suspend fun initRound(
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    )

    suspend fun getRoundState(roundId: String): FfiRoundState?

    suspend fun listRounds(): List<FfiRoundSummary>

    suspend fun getBundleCount(roundId: String): Int

    suspend fun getVotes(roundId: String): List<VoteRecord>

    suspend fun clearRound(roundId: String)

    suspend fun deleteSkippedBundles(
        roundId: String,
        keepCount: Int
    ): Long

    suspend fun setupBundles(
        roundId: String,
        notesJson: String
    ): FfiBundleSetupResult

    suspend fun generateHotkey(
        roundId: String,
        seed: ByteArray
    ): FfiVotingHotkey
}

data class VoteRecord(
    val proposalId: Int,
    val bundleIndex: Int,
    val choice: Int,
    val submitted: Boolean
)
