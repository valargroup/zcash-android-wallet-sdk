package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.voting.JniBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundState
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.JniVoteRecord
import cash.z.ecc.android.sdk.internal.model.voting.JniVotingHotkey

@Suppress("TooManyFunctions", "LongParameterList")
internal interface TypesafeVotingBackend {
    suspend fun openVotingDb(dbPath: String, walletId: String): TypesafeVotingDb

    suspend fun computeShareNullifier(
        voteCommitment: ByteArray,
        shareIndex: Int,
        blind: ByteArray
    ): ByteArray

    suspend fun computeBundleSetup(notesJson: String): JniBundleSetupResult

    suspend fun warmProvingCaches()

    suspend fun ballotDivisorZatoshi(): Long

    suspend fun extractPcztSighash(pcztBytes: ByteArray): ByteArray

    suspend fun extractSpendAuthSig(
        signedPcztBytes: ByteArray,
        actionIndex: Int
    ): ByteArray

    suspend fun getWalletNotes(
        walletDbPath: String,
        snapshotHeight: Long,
        networkId: Int,
        accountUuidBytes: ByteArray
    ): String

    suspend fun buildSharePayloadsJson(
        encSharesJson: String,
        commitmentJson: String,
        voteDecision: Int,
        numOptions: Int,
        vcTreePosition: Long,
        singleShareMode: Boolean = false
    ): String

    suspend fun signCastVote(
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
    ): ByteArray

    suspend fun decomposeWeight(weight: Long): List<Long>

    suspend fun generateDelegationInputs(
        senderSeed: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): DelegationInputsResult

    suspend fun generateDelegationInputsWithFvk(
        fvkBytes: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        seedFingerprint: ByteArray
    ): DelegationInputsResult

    suspend fun extractOrchardFvkFromUfvk(ufvk: String, networkId: Int): ByteArray

    suspend fun extractNcRoot(treeStateBytes: ByteArray): ByteArray

    suspend fun verifyWitness(witnessJson: String): Boolean
}

@Suppress("TooManyFunctions", "LongParameterList")
internal interface TypesafeVotingDb {
    suspend fun close()

    suspend fun initRound(
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    )

    suspend fun getRoundState(roundId: String): JniRoundState?

    suspend fun listRounds(): List<JniRoundSummary>

    suspend fun getBundleCount(roundId: String): Int

    suspend fun getVotes(roundId: String): List<JniVoteRecord>

    suspend fun clearRound(roundId: String)

    suspend fun deleteSkippedBundles(
        roundId: String,
        keepCount: Int
    ): Long

    suspend fun setupBundles(
        roundId: String,
        notesJson: String
    ): JniBundleSetupResult

    suspend fun generateHotkey(
        roundId: String,
        seed: ByteArray
    ): JniVotingHotkey

    suspend fun buildGovernancePczt(
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
    ): GovernancePcztResult

    suspend fun storeWitnesses(
        roundId: String,
        bundleIndex: Int,
        witnessesJson: String
    )

    suspend fun precomputeDelegationPir(
        roundId: String,
        bundleIndex: Int,
        pirServerUrl: String,
        networkId: Int,
        notesJson: String
    ): DelegationPirPrecomputeResult

    suspend fun buildAndProveDelegation(
        roundId: String,
        bundleIndex: Int,
        pirServerUrl: String,
        networkId: Int,
        notesJson: String,
        hotkeyRawSeed: ByteArray,
        proofProgress: ((Double) -> Unit)? = null
    ): DelegationProofResult

    suspend fun getDelegationSubmission(
        roundId: String,
        bundleIndex: Int,
        senderSeed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): DelegationSubmissionResult

    suspend fun getDelegationSubmissionWithKeystoneSig(
        roundId: String,
        bundleIndex: Int,
        keystoneSig: ByteArray,
        keystoneSighash: ByteArray
    ): DelegationSubmissionResult

    suspend fun storeKeystoneSignature(
        roundId: String,
        bundleIndex: Int,
        sig: ByteArray,
        sighash: ByteArray,
        rk: ByteArray
    )

    suspend fun getKeystoneSignatures(roundId: String): List<KeystoneSignatureRecord>

    suspend fun storeDelegationTxHash(roundId: String, bundleIndex: Int, txHash: String)

    suspend fun getDelegationTxHash(roundId: String, bundleIndex: Int): VotingTxHashLookup

    suspend fun storeVoteTxHash(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        txHash: String
    )

    suspend fun markVoteSubmitted(roundId: String, bundleIndex: Int, proposalId: Int)

    suspend fun getVoteTxHash(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int
    ): VotingTxHashLookup

    suspend fun storeCommitmentBundle(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        bundleJson: String,
        vcTreePosition: Long
    )

    suspend fun getCommitmentBundle(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int
    ): CommitmentBundleRecord?

    suspend fun clearRecoveryState(roundId: String)

    suspend fun recordShareDelegation(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int,
        sentToUrls: List<String>,
        nullifier: ByteArray,
        submitAt: Long
    )

    suspend fun getShareDelegations(roundId: String): List<ShareDelegationRecord>

    suspend fun getUnconfirmedDelegations(roundId: String): List<ShareDelegationRecord>

    suspend fun markShareConfirmed(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int
    )

    suspend fun addSentServers(
        roundId: String,
        bundleIndex: Int,
        proposalId: Int,
        shareIndex: Int,
        newUrls: List<String>
    )

    suspend fun syncVoteTree(roundId: String, nodeUrl: String): Long

    suspend fun resetTreeClient(roundId: String = "")

    suspend fun storeVanPosition(roundId: String, bundleIndex: Int, position: Int)

    suspend fun generateVanWitnessJson(
        roundId: String,
        bundleIndex: Int,
        anchorHeight: Int
    ): String

    suspend fun storeTreeState(roundId: String, treeStateBytes: ByteArray)

    suspend fun generateNoteWitnesses(
        roundId: String,
        bundleIndex: Int,
        walletDbPath: String,
        notesJson: String
    ): String

    suspend fun buildVoteCommitment(
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
        singleShare: Boolean = false,
        proofProgress: ((Double) -> Unit)? = null
    ): VoteCommitmentResult

    suspend fun encryptShares(roundId: String, shares: List<Long>): List<WireEncryptedShare>
}

internal data class GovernancePcztResult(
    val pcztBytes: ByteArray,
    val rk: ByteArray,
    val sighash: ByteArray,
    val actionIndex: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GovernancePcztResult) return false
        return pcztBytes.contentEquals(other.pcztBytes) &&
            rk.contentEquals(other.rk) &&
            sighash.contentEquals(other.sighash) &&
            actionIndex == other.actionIndex
    }

    override fun hashCode(): Int {
        var result = pcztBytes.contentHashCode()
        result = 31 * result + rk.contentHashCode()
        result = 31 * result + sighash.contentHashCode()
        result = 31 * result + actionIndex
        return result
    }
}

internal data class DelegationProofResult(
    val proof: ByteArray,
    val publicInputs: List<ByteArray>,
    val nfSigned: ByteArray,
    val cmxNew: ByteArray,
    val govNullifiers: List<ByteArray>,
    val vanComm: ByteArray,
    val rk: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegationProofResult) return false
        return proof.contentEquals(other.proof)
    }

    override fun hashCode(): Int = proof.contentHashCode()
}

internal data class DelegationPirPrecomputeResult(
    val cachedCount: Long,
    val fetchedCount: Long
)

internal data class VoteCommitmentResult(
    val vanNullifier: ByteArray,
    val voteAuthorityNoteNew: ByteArray,
    val voteCommitment: ByteArray,
    val rVpk: ByteArray,
    val alphaV: ByteArray,
    val anchorHeight: Int,
    val encSharesJson: String,
    val rawBundleJson: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoteCommitmentResult) return false
        return voteCommitment.contentEquals(other.voteCommitment)
    }

    override fun hashCode(): Int = voteCommitment.contentHashCode()
}

internal data class WireEncryptedShare(
    val c1: ByteArray,
    val c2: ByteArray,
    val shareIndex: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WireEncryptedShare) return false
        return c1.contentEquals(other.c1) &&
            c2.contentEquals(other.c2) &&
            shareIndex == other.shareIndex
    }

    override fun hashCode(): Int {
        var result = c1.contentHashCode()
        result = 31 * result + c2.contentHashCode()
        result = 31 * result + shareIndex
        return result
    }
}

internal data class DelegationInputsResult(
    val fvkBytes: ByteArray,
    val gDNewX: ByteArray,
    val pkDNewX: ByteArray,
    val hotkeyRawAddress: ByteArray,
    val hotkeyPublicKey: ByteArray,
    val hotkeyAddress: String,
    val seedFingerprint: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegationInputsResult) return false
        return fvkBytes.contentEquals(other.fvkBytes) &&
            gDNewX.contentEquals(other.gDNewX) &&
            pkDNewX.contentEquals(other.pkDNewX) &&
            hotkeyRawAddress.contentEquals(other.hotkeyRawAddress) &&
            hotkeyPublicKey.contentEquals(other.hotkeyPublicKey) &&
            hotkeyAddress == other.hotkeyAddress &&
            seedFingerprint.contentEquals(other.seedFingerprint)
    }

    override fun hashCode(): Int {
        var result = fvkBytes.contentHashCode()
        result = 31 * result + gDNewX.contentHashCode()
        result = 31 * result + pkDNewX.contentHashCode()
        result = 31 * result + hotkeyRawAddress.contentHashCode()
        result = 31 * result + hotkeyPublicKey.contentHashCode()
        result = 31 * result + hotkeyAddress.hashCode()
        result = 31 * result + seedFingerprint.contentHashCode()
        return result
    }
}

internal data class DelegationSubmissionResult(
    val proof: ByteArray,
    val rk: ByteArray,
    val spendAuthSig: ByteArray,
    val sighash: ByteArray,
    val nfSigned: ByteArray,
    val cmxNew: ByteArray,
    val govComm: ByteArray,
    val govNullifiers: List<ByteArray>,
    val alpha: ByteArray,
    val voteRoundId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegationSubmissionResult) return false
        return sighash.contentEquals(other.sighash)
    }

    override fun hashCode(): Int = sighash.contentHashCode()
}

internal data class KeystoneSignatureRecord(
    val bundleIndex: Int,
    val sig: ByteArray,
    val sighash: ByteArray,
    val rk: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeystoneSignatureRecord) return false
        return bundleIndex == other.bundleIndex &&
            sig.contentEquals(other.sig) &&
            sighash.contentEquals(other.sighash) &&
            rk.contentEquals(other.rk)
    }

    override fun hashCode(): Int {
        var result = bundleIndex
        result = 31 * result + sig.contentHashCode()
        result = 31 * result + sighash.contentHashCode()
        result = 31 * result + rk.contentHashCode()
        return result
    }
}

internal data class CommitmentBundleRecord(
    val bundleJson: String,
    val vcTreePosition: Long
)

internal data class VoteRecord(
    val proposalId: Int,
    val bundleIndex: Int,
    val choice: Int,
    val submitted: Boolean
)

internal sealed interface VotingTxHashLookup {
    data object NotFound : VotingTxHashLookup

    data class Present(
        val txHash: String
    ) : VotingTxHashLookup
}

internal data class ShareDelegationRecord(
    val roundId: String,
    val bundleIndex: Int,
    val proposalId: Int,
    val shareIndex: Int,
    val sentToUrls: List<String>,
    val nullifier: ByteArray,
    val confirmed: Boolean,
    val submitAt: Long,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShareDelegationRecord) return false
        return roundId == other.roundId &&
            bundleIndex == other.bundleIndex &&
            proposalId == other.proposalId &&
            shareIndex == other.shareIndex &&
            sentToUrls == other.sentToUrls &&
            nullifier.contentEquals(other.nullifier) &&
            confirmed == other.confirmed &&
            submitAt == other.submitAt &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = roundId.hashCode()
        result = 31 * result + bundleIndex
        result = 31 * result + proposalId
        result = 31 * result + shareIndex
        result = 31 * result + sentToUrls.hashCode()
        result = 31 * result + nullifier.contentHashCode()
        result = 31 * result + confirmed.hashCode()
        result = 31 * result + submitAt.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
