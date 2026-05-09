package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.jni.VotingRustBackend
import cash.z.ecc.android.sdk.internal.model.voting.FfiBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.FfiVotingHotkey
import org.json.JSONArray

@Suppress("TooManyFunctions", "LongParameterList")
class TypesafeVotingBackendImpl : TypesafeVotingBackend {
    override suspend fun openVotingDb(dbPath: String, walletId: String): TypesafeVotingDb =
        TypesafeVotingDbImpl(VotingRustBackend.new().openVotingDb(dbPath, walletId))

    override suspend fun computeBundleSetup(notesJson: String): FfiBundleSetupResult =
        VotingRustBackend.new().computeBundleSetup(notesJson)

    override suspend fun warmProvingCaches() =
        VotingRustBackend.new().warmProvingCaches()
}

@Suppress("TooManyFunctions", "LongParameterList")
private class TypesafeVotingDbImpl(
    private val votingDb: VotingRustBackend.VotingDb
) : TypesafeVotingDb {
    override suspend fun close() = votingDb.close()

    override suspend fun initRound(
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    ) = votingDb.initRound(
        roundId,
        snapshotHeight,
        eaPK,
        ncRoot,
        nullifierIMTRoot,
        sessionJson
    )

    override suspend fun getRoundState(roundId: String): FfiRoundState? =
        votingDb.getRoundState(roundId)

    override suspend fun listRounds(): List<FfiRoundSummary> =
        JSONArray(votingDb.listRoundsJson()).toList { obj ->
            FfiRoundSummary(
                roundId = obj.getString("round_id"),
                phase = obj.getCheckedInt("phase"),
                snapshotHeight = obj.getLong("snapshot_height"),
                createdAt = obj.getLong("created_at")
            )
        }

    override suspend fun getBundleCount(roundId: String): Int =
        votingDb.getBundleCount(roundId)

    override suspend fun getVotes(roundId: String): List<VoteRecord> =
        JSONArray(votingDb.getVotesJson(roundId)).toList { obj ->
            VoteRecord(
                proposalId = obj.getCheckedInt("proposal_id"),
                bundleIndex = obj.getCheckedInt("bundle_index"),
                choice = obj.getCheckedInt("choice"),
                submitted = obj.getBoolean("submitted")
            )
        }

    override suspend fun clearRound(roundId: String) =
        votingDb.clearRound(roundId)

    override suspend fun deleteSkippedBundles(
        roundId: String,
        keepCount: Int
    ): Long = votingDb.deleteSkippedBundles(roundId, keepCount)

    override suspend fun setupBundles(
        roundId: String,
        notesJson: String
    ): FfiBundleSetupResult =
        votingDb.setupBundles(roundId, notesJson)

    override suspend fun generateHotkey(
        roundId: String,
        seed: ByteArray
    ): FfiVotingHotkey =
        votingDb.generateHotkey(roundId, seed)
}

private fun <T> JSONArray.toList(transform: (org.json.JSONObject) -> T): List<T> =
    (JSON_ARRAY_START_INDEX until length()).map { index ->
        transform(getJSONObject(index))
    }

private fun org.json.JSONObject.getCheckedInt(name: String): Int =
    Math.toIntExact(getLong(name))

private const val JSON_ARRAY_START_INDEX = 0
