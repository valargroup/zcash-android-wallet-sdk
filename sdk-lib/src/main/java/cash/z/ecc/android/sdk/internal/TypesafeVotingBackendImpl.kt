@file:Suppress("TooManyFunctions")

package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.internal.jni.VotingProofProgressCallback
import cash.z.ecc.android.sdk.internal.jni.VotingRustBackend
import cash.z.ecc.android.sdk.internal.model.voting.JniBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundState
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.JniVoteRecord
import cash.z.ecc.android.sdk.internal.model.voting.JniVotingHotkey
import org.json.JSONArray
import org.json.JSONObject

private const val PCZT_HASH_BYTES = 32

@Suppress("TooManyFunctions", "LongParameterList")
class TypesafeVotingBackendImpl : TypesafeVotingBackend {
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

    override suspend fun computeBundleSetup(notesJson: String): JniBundleSetupResult =
        rustBackend().computeBundleSetup(notesJson)

    override suspend fun warmProvingCaches() =
        rustBackend().warmProvingCaches()

    override suspend fun ballotDivisorZatoshi(): Long =
        rustBackend().ballotDivisorZatoshi()

    override suspend fun extractPcztSighash(pcztBytes: ByteArray): ByteArray =
        rustBackend().extractPcztSighash(pcztBytes)

    override suspend fun extractSpendAuthSig(
        signedPcztBytes: ByteArray,
        actionIndex: Int
    ): ByteArray =
        rustBackend().extractSpendAuthSig(signedPcztBytes, actionIndex)

    override suspend fun getWalletNotes(
        walletDbPath: String,
        snapshotHeight: Long,
        networkId: Int,
        accountUuidBytes: ByteArray
    ): String =
        rustBackend().getWalletNotesJson(walletDbPath, snapshotHeight, networkId, accountUuidBytes)

    override suspend fun buildSharePayloadsJson(
        encSharesJson: String,
        commitmentJson: String,
        voteDecision: Int,
        numOptions: Int,
        vcTreePosition: Long,
        singleShareMode: Boolean
    ): String =
        rustBackend().buildSharePayloadsJson(
            encSharesJson,
            commitmentJson,
            voteDecision,
            numOptions,
            vcTreePosition,
            singleShareMode
        )

    override suspend fun signCastVote(
        hotkeySeed: ByteArray,
        networkId: Int,
        roundId: String,
        rVpk: ByteArray,
        vanNullifier: ByteArray,
        vanNew: ByteArray,
        voteCommitment: ByteArray,
        proposalId: Int,
        anchorHeight: Int,
        alphaV: ByteArray
    ): ByteArray =
        rustBackend().signCastVote(
            hotkeySeed,
            networkId,
            roundId,
            rVpk,
            vanNullifier,
            vanNew,
            voteCommitment,
            proposalId,
            anchorHeight,
            alphaV
        )

    override suspend fun decomposeWeight(weight: Long): List<Long> =
        JSONArray(rustBackend().decomposeWeightJson(weight)).toLongList()

    override suspend fun generateDelegationInputs(
        senderSeed: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): DelegationInputsResult =
        JSONObject(
            rustBackend().generateDelegationInputsJson(
                senderSeed,
                hotkeySeed,
                networkId,
                accountIndex
            )
        ).toDelegationInputsResult()

    override suspend fun generateDelegationInputsWithFvk(
        fvkBytes: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        seedFingerprint: ByteArray
    ): DelegationInputsResult =
        JSONObject(
            rustBackend().generateDelegationInputsWithFvkJson(
                fvkBytes,
                hotkeySeed,
                networkId,
                seedFingerprint
            )
        ).toDelegationInputsResult()

    override suspend fun extractOrchardFvkFromUfvk(ufvk: String, networkId: Int): ByteArray =
        rustBackend().extractOrchardFvkFromUfvk(ufvk, networkId)

    override suspend fun extractNcRoot(treeStateBytes: ByteArray): ByteArray =
        rustBackend().extractNcRoot(treeStateBytes)

    override suspend fun verifyWitness(witnessJson: String): Boolean =
        when (val result = rustBackend().verifyWitness(witnessJson)) {
            0 -> false
            1 -> true
            else -> error("verifyWitness failed with result=$result")
        }

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

    override suspend fun getRoundState(roundId: String): JniRoundState? =
        votingDb.getRoundState(roundId)

    override suspend fun listRounds(): List<JniRoundSummary> =
        votingDb.listRounds().asList()

    override suspend fun getBundleCount(roundId: String): Int =
        votingDb.getBundleCount(roundId)

    override suspend fun getVotes(roundId: String): List<JniVoteRecord> =
        votingDb.getVotes(roundId).asList()

    override suspend fun clearRound(roundId: String) =
        votingDb.clearRound(roundId)

    override suspend fun deleteSkippedBundles(
        roundId: String,
        keepCount: Int
    ): Long = votingDb.deleteSkippedBundles(roundId, keepCount)

    override suspend fun setupBundles(
        roundId: String,
        notesJson: String
    ): JniBundleSetupResult =
        votingDb.setupBundles(roundId, notesJson)

    override suspend fun generateHotkey(
        roundId: String,
        seed: ByteArray
    ): JniVotingHotkey =
        votingDb.generateHotkey(roundId, seed)

    override suspend fun buildGovernancePczt(
        roundId: String,
        bundleIndex: Int,
        ufvk: String,
        networkId: Int,
        accountIndex: Int,
        notesJson: String,
        walletSeed: ByteArray,
        hotkeySeed: ByteArray,
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
                hotkeySeed,
                seedFingerprint,
                roundName,
                addressIndex
            )
        ).toGovernancePcztResult()

    override suspend fun storeWitnesses(
        roundId: String,
        bundleIndex: Int,
        witnessesJson: String
    ) = votingDb.storeWitnesses(roundId, bundleIndex, witnessesJson)

    override suspend fun precomputeDelegationPir(
        roundId: String,
        bundleIndex: Int,
        pirServerUrl: String,
        networkId: Int,
        notesJson: String
    ): DelegationPirPrecomputeResult =
        JSONObject(
            votingDb.precomputeDelegationPirJson(
                roundId,
                bundleIndex,
                pirServerUrl,
                networkId,
                notesJson
            )
        ).toDelegationPirPrecomputeResult()

    override suspend fun buildAndProveDelegation(
        roundId: String,
        bundleIndex: Int,
        pirServerUrl: String,
        networkId: Int,
        notesJson: String,
        hotkeyRawSeed: ByteArray,
        proofProgress: ((Double) -> Unit)?
    ): DelegationProofResult =
        JSONObject(
            votingDb.buildAndProveDelegationJson(
                roundId,
                bundleIndex,
                pirServerUrl,
                networkId,
                notesJson,
                hotkeyRawSeed,
                proofProgress?.asVotingProgressCallback()
            )
        ).toDelegationProofResult()

    override suspend fun getDelegationSubmission(
        roundId: String,
        bundleIndex: Int,
        senderSeed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): DelegationSubmissionResult =
        JSONObject(
            votingDb.getDelegationSubmissionJson(
                roundId,
                bundleIndex,
                senderSeed,
                networkId,
                accountIndex
            )
        ).toDelegationSubmissionResult()

    override suspend fun getDelegationSubmissionWithKeystoneSig(
        roundId: String,
        bundleIndex: Int,
        keystoneSig: ByteArray,
        keystoneSighash: ByteArray
    ): DelegationSubmissionResult =
        JSONObject(
            votingDb.getDelegationSubmissionWithKeystoneSigJson(
                roundId,
                bundleIndex,
                keystoneSig,
                keystoneSighash
            )
        ).toDelegationSubmissionResult()

    override suspend fun storeKeystoneSignature(
        roundId: String,
        bundleIndex: Int,
        sig: ByteArray,
        sighash: ByteArray,
        rk: ByteArray
    ) = votingDb.storeKeystoneSignature(roundId, bundleIndex, sig, sighash, rk)

    override suspend fun getKeystoneSignatures(roundId: String): List<KeystoneSignatureRecord> =
        JSONArray(votingDb.getKeystoneSignaturesJson(roundId)).toList { obj ->
            KeystoneSignatureRecord(
                bundleIndex = obj.getCheckedInt("bundle_index"),
                sig = obj.getHexBytes("sig"),
                sighash = obj.getHexBytes("sighash", PCZT_HASH_BYTES),
                rk = obj.getHexBytes("rk", PCZT_HASH_BYTES)
            )
        }

    override suspend fun storeDelegationTxHash(roundId: String, bundleIndex: Int, txHash: String) =
        votingDb.storeDelegationTxHash(roundId, bundleIndex, txHash)

    override suspend fun getDelegationTxHash(
        roundId: String,
        bundleIndex: Int
    ): VotingTxHashLookup =
        runExpectedMissingRowLookup {
            votingDb.getDelegationTxHash(roundId, bundleIndex).toTxHashLookup()
        } ?: VotingTxHashLookup.NotFound

    override suspend fun storeVoteTxHash(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        txHash: String
    ) = votingDb.storeVoteTxHash(roundId, bundleIndex, proposalId, txHash)

    override suspend fun markVoteSubmitted(roundId: String, bundleIndex: Int, proposalId: Int) =
        votingDb.markVoteSubmitted(roundId, bundleIndex, proposalId)

    override suspend fun getVoteTxHash(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int
    ): VotingTxHashLookup =
        runExpectedMissingRowLookup {
            votingDb.getVoteTxHash(roundId, bundleIndex, proposalId).toTxHashLookup()
        } ?: VotingTxHashLookup.NotFound

    override suspend fun storeCommitmentBundle(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        bundleJson: String,
        vcTreePosition: Long
    ) = votingDb.storeCommitmentBundle(
        roundId,
        bundleIndex,
        proposalId,
        bundleJson,
        vcTreePosition
    )

    override suspend fun getCommitmentBundle(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int
    ): CommitmentBundleRecord? =
        runExpectedMissingRowLookup {
            votingDb
                .getCommitmentBundleJson(roundId, bundleIndex, proposalId)
                ?.let { JSONObject(it).toCommitmentBundleRecord() }
        }

    override suspend fun clearRecoveryState(roundId: String) =
        votingDb.clearRecoveryState(roundId)

    override suspend fun recordShareDelegation(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int,
        sentToUrls: List<String>,
        nullifier: ByteArray,
        submitAt: Long
    ) = votingDb.recordShareDelegation(
        roundId,
        bundleIndex,
        proposalId,
        shareIndex,
        JSONArray(sentToUrls).toString(),
        nullifier,
        submitAt
    )

    override suspend fun getShareDelegations(roundId: String): List<ShareDelegationRecord> =
        parseShareDelegations(votingDb.getShareDelegationsJson(roundId))

    override suspend fun getUnconfirmedDelegations(roundId: String): List<ShareDelegationRecord> =
        parseShareDelegations(votingDb.getUnconfirmedDelegationsJson(roundId))

    override suspend fun markShareConfirmed(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int
    ) = votingDb.markShareConfirmed(roundId, bundleIndex, proposalId, shareIndex)

    override suspend fun addSentServers(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int,
        newUrls: List<String>
    ) = votingDb.addSentServers(
        roundId,
        bundleIndex,
        proposalId,
        shareIndex,
        JSONArray(newUrls).toString()
    )

    override suspend fun syncVoteTree(roundId: String, nodeUrl: String): Long =
        votingDb.syncVoteTree(roundId, nodeUrl)

    override suspend fun resetTreeClient(roundId: String) =
        votingDb.resetTreeClient(roundId)

    override suspend fun storeVanPosition(roundId: String, bundleIndex: Int, position: Int) =
        votingDb.storeVanPosition(roundId, bundleIndex, position)

    override suspend fun generateVanWitnessJson(
        roundId: String,
        bundleIndex: Int,
        anchorHeight: Int
    ): String =
        votingDb.generateVanWitnessJson(roundId, bundleIndex, anchorHeight)

    override suspend fun storeTreeState(roundId: String, treeStateBytes: ByteArray) =
        votingDb.storeTreeState(roundId, treeStateBytes)

    override suspend fun generateNoteWitnesses(
        roundId: String,
        bundleIndex: Int,
        walletDbPath: String,
        notesJson: String
    ): String =
        votingDb.generateNoteWitnessesJson(roundId, bundleIndex, walletDbPath, notesJson)

    override suspend fun buildVoteCommitment(
        roundId: String,
        bundleIndex: Int,
        hotkeySeed: ByteArray,
        proposalId: Int,
        choice: Int,
        numOptions: Int,
        witnessJson: String,
        vanPosition: Int,
        anchorHeight: Int,
        networkId: Int,
        singleShare: Boolean,
        proofProgress: ((Double) -> Unit)?
    ): VoteCommitmentResult =
        JSONObject(
            votingDb.buildVoteCommitmentJson(
                roundId,
                bundleIndex,
                hotkeySeed,
                proposalId,
                choice,
                numOptions,
                witnessJson,
                vanPosition,
                anchorHeight,
                networkId,
                singleShare,
                proofProgress?.asVotingProgressCallback()
            )
        ).toVoteCommitmentResult()

    override suspend fun encryptShares(roundId: String, shares: List<Long>): List<WireEncryptedShare> =
        parseWireEncryptedShares(
            votingDb.encryptSharesJson(roundId, JSONArray(shares).toString())
        )
}

private fun <T> JSONArray.toList(transform: (org.json.JSONObject) -> T): List<T> =
    (0 until length()).map { index ->
        transform(getJSONObject(index))
    }

private fun JSONArray.toLongList(): List<Long> =
    (0 until length()).map { index -> getLong(index) }

private fun org.json.JSONObject.getCheckedInt(name: String): Int =
    Math.toIntExact(getLong(name))

private fun JSONObject.toGovernancePcztResult() =
    GovernancePcztResult(
        pcztBytes = getHexBytes("pczt_bytes"),
        rk = getHexBytes("rk", PCZT_HASH_BYTES),
        sighash = getHexBytes("pczt_sighash", PCZT_HASH_BYTES),
        actionIndex = getCheckedInt("action_index")
    )

private fun JSONObject.toDelegationPirPrecomputeResult() =
    DelegationPirPrecomputeResult(
        cachedCount = getLong("cached_count"),
        fetchedCount = getLong("fetched_count")
    )

private fun JSONObject.toDelegationProofResult() =
    DelegationProofResult(
        proof = getHexBytes("proof"),
        publicInputs = getJSONArray("public_inputs").toHexList(),
        nfSigned = getHexBytes("nf_signed"),
        cmxNew = getHexBytes("cmx_new"),
        govNullifiers = getJSONArray("gov_nullifiers").toHexList(),
        vanComm = getHexBytes("van_comm"),
        rk = getHexBytes("rk", PCZT_HASH_BYTES)
    )

private fun JSONObject.toDelegationSubmissionResult() =
    DelegationSubmissionResult(
        proof = getHexBytes("proof"),
        rk = getHexBytes("rk", PCZT_HASH_BYTES),
        spendAuthSig = getHexBytes("spend_auth_sig"),
        sighash = getHexBytes("sighash", PCZT_HASH_BYTES),
        nfSigned = getHexBytes("nf_signed"),
        cmxNew = getHexBytes("cmx_new"),
        govComm = getHexBytes("gov_comm"),
        govNullifiers = getJSONArray("gov_nullifiers").toHexList(),
        alpha = getHexBytes("alpha"),
        voteRoundId = getString("vote_round_id")
    )

private fun JSONObject.toCommitmentBundleRecord() =
    CommitmentBundleRecord(
        bundleJson = getString("bundle_json"),
        vcTreePosition = getLong("vc_tree_position")
    )

private fun JSONObject.toVoteCommitmentResult(): VoteCommitmentResult {
    val encSharesJson = getJSONArray("enc_shares").toString()
    return VoteCommitmentResult(
        vanNullifier = getHexBytes("van_nullifier"),
        voteAuthorityNoteNew = getHexBytes("vote_authority_note_new"),
        voteCommitment = getHexBytes("vote_commitment"),
        rVpk = getHexBytes("r_vpk_bytes"),
        alphaV = getHexBytes("alpha_v"),
        anchorHeight = getCheckedInt("anchor_height"),
        encSharesJson = encSharesJson,
        rawBundleJson = toString()
    )
}

private fun JSONObject.toDelegationInputsResult() =
    DelegationInputsResult(
        fvkBytes = getHexBytes("fvk_bytes"),
        gDNewX = getHexBytes("g_d_new_x"),
        pkDNewX = getHexBytes("pk_d_new_x"),
        hotkeyRawAddress = getHexBytes("hotkey_raw_address"),
        hotkeyPublicKey = getHexBytes("hotkey_public_key"),
        hotkeyAddress = getString("hotkey_address"),
        seedFingerprint = getHexBytes("seed_fingerprint", PCZT_HASH_BYTES)
    )

private fun parseWireEncryptedShares(json: String): List<WireEncryptedShare> =
    JSONArray(json).toList { obj ->
        WireEncryptedShare(
            c1 = obj.getHexBytes("c1"),
            c2 = obj.getHexBytes("c2"),
            shareIndex = obj.getCheckedInt("share_index")
        )
    }

private fun parseShareDelegations(json: String): List<ShareDelegationRecord> =
    JSONArray(json).toList { obj ->
        val sentToUrls = obj.getJSONArray("sent_to_urls")
        ShareDelegationRecord(
            roundId = obj.getString("round_id"),
            bundleIndex = obj.getCheckedInt("bundle_index"),
            proposalId = obj.getCheckedInt("proposal_id"),
            shareIndex = obj.getCheckedInt("share_index"),
            sentToUrls = (0 until sentToUrls.length()).map(sentToUrls::getString),
            nullifier = obj.getHexBytes("nullifier"),
            confirmed = obj.getBoolean("confirmed"),
            submitAt = obj.getLong("submit_at"),
            createdAt = obj.getLong("created_at")
        )
    }

private fun JSONArray.toHexList(): List<ByteArray> =
    (0 until length()).map { index -> getString(index).fromHex() }

private fun ((Double) -> Unit).asVotingProgressCallback() =
    VotingProofProgressCallback { progress -> invoke(progress) }

@Suppress("TooGenericExceptionCaught")
private suspend fun <T> runExpectedMissingRowLookup(block: suspend () -> T): T? =
    try {
        block()
    } catch (exception: RuntimeException) {
        // Recovery lookups are cache probes. Defense-in-depth
        // to avoid having no rows be propagated as exceptions.
        if (exception.isQueryReturnedNoRows()) {
            null
        } else {
            throw exception
        }
    }

private fun Throwable.isQueryReturnedNoRows(): Boolean =
    generateSequence(this) { throwable -> throwable.cause }
        .any { throwable ->
            throwable.message
                ?.contains("Query returned no rows", ignoreCase = true) == true
        }

private fun String?.toTxHashLookup(): VotingTxHashLookup =
    if (this == null) {
        VotingTxHashLookup.NotFound
    } else {
        VotingTxHashLookup.Present(this)
    }

private fun JSONObject.getHexBytes(
    name: String,
    expectedSize: Int? = null
): ByteArray {
    val bytes = getString(name).fromHex()

    require(expectedSize == null || bytes.size == expectedSize) {
        "$name must be $expectedSize bytes, got ${bytes.size}"
    }

    return bytes
}
