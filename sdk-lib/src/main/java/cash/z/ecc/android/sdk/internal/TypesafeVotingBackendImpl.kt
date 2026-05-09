package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.internal.jni.VotingRustBackend
import cash.z.ecc.android.sdk.internal.model.voting.FfiBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.FfiVotingHotkey
import cash.z.ecc.android.sdk.internal.model.voting.VoteRecord
import org.json.JSONArray
import org.json.JSONObject

@Suppress("TooManyFunctions", "LongParameterList")
internal class TypesafeVotingBackendImpl : TypesafeVotingBackend {
    private val rustBackendLazy =
        SuspendingLazy<Unit, VotingRustBackend> {
            VotingRustBackend.new()
        }

    override suspend fun computeShareNullifier(
        voteCommitment: ByteArray,
        shareIndex: Int,
        blind: ByteArray
    ): ByteArray =
        rustBackend().computeShareNullifier(voteCommitment, shareIndex, blind)

    override suspend fun openVotingDb(dbPath: String, walletId: String): TypesafeVotingDb =
        TypesafeVotingDbImpl(rustBackend().openVotingDb(dbPath, walletId))

    override suspend fun computeBundleSetup(notesJson: String): FfiBundleSetupResult =
        rustBackend().computeBundleSetup(notesJson)

    override suspend fun warmProvingCaches() =
        rustBackend().warmProvingCaches()

    override suspend fun extractPcztSighash(pcztBytes: ByteArray): ByteArray =
        rustBackend().extractPcztSighash(pcztBytes)

    override suspend fun extractSpendAuthSig(
        signedPcztBytes: ByteArray,
        actionIndex: Int
    ): ByteArray =
        rustBackend().extractSpendAuthSig(signedPcztBytes, actionIndex)

    private suspend fun rustBackend() = rustBackendLazy.getInstance(Unit)
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

    override suspend fun buildGovernancePczt(
        roundId: String,
        bundleIndex: Int,
        ufvk: String,
        networkId: Int,
        accountIndex: Int,
        notesJson: String,
        walletSeed: ByteArray,
        seedFingerprint: ByteArray,
        roundName: String,
        addressIndex: Int
    ): GovernancePcztResult =
        JSONObject(
            votingDb.buildGovernancePcztJson(
                roundId,
                bundleIndex,
                ufvk,
                networkId,
                accountIndex,
                notesJson,
                walletSeed,
                seedFingerprint,
                roundName,
                addressIndex
            )
        ).toGovernancePcztResult()
}

private fun <T> JSONArray.toList(transform: (org.json.JSONObject) -> T): List<T> =
    (0 until length()).map { index ->
        transform(getJSONObject(index))
    }

private fun org.json.JSONObject.getCheckedInt(name: String): Int =
    Math.toIntExact(getLong(name))

private fun JSONObject.toGovernancePcztResult() =
    GovernancePcztResult(
        pcztBytes = getString("pczt_bytes").fromHex(),
        rk = getString("rk").fromHex(),
        sighash = getString("pczt_sighash").fromHex(),
        actionIndex = getCheckedInt("action_index")
    )
