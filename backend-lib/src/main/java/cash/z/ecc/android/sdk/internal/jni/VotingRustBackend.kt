package cash.z.ecc.android.sdk.internal.jni

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.model.voting.JniBundleSetupResult
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundState
import cash.z.ecc.android.sdk.internal.model.voting.JniRoundSummary
import cash.z.ecc.android.sdk.internal.model.voting.JniVoteRecord
import cash.z.ecc.android.sdk.internal.model.voting.JniVotingHotkey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Keep
fun interface VotingProofProgressCallback {
    @Keep
    fun onProgress(progress: Double)
}

@Keep
@Suppress("TooManyFunctions", "LongParameterList")
class VotingRustBackend private constructor() {
    @Throws(RuntimeException::class)
    suspend fun computeShareNullifier(
        voteCommitment: ByteArray,
        shareIndex: Int,
        blind: ByteArray
    ): ByteArray =
        withContext(Dispatchers.IO) {
            computeShareNullifierNative(voteCommitment, shareIndex, blind)
        }

    @Throws(RuntimeException::class)
    suspend fun computeBundleSetup(notesJson: String): JniBundleSetupResult =
        withContext(Dispatchers.IO) {
            computeBundleSetupNative(notesJson)
                ?: error("computeBundleSetup returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun warmProvingCaches() =
        withContext(Dispatchers.IO) {
            warmProvingCachesNative()
        }

    @Throws(RuntimeException::class)
    suspend fun ballotDivisorZatoshi(): Long =
        withContext(Dispatchers.IO) {
            ballotDivisorZatoshiNative()
        }

    @Throws(RuntimeException::class)
    suspend fun decomposeWeightJson(weight: Long): String =
        withContext(Dispatchers.IO) {
            decomposeWeightJsonNative(weight)
                ?: error("decomposeWeight returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun generateDelegationInputsJson(
        senderSeed: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): String =
        withContext(Dispatchers.IO) {
            generateDelegationInputsJsonNative(senderSeed, hotkeySeed, networkId, accountIndex)
                ?: error("generateDelegationInputs returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun generateDelegationInputsWithFvkJson(
        fvkBytes: ByteArray,
        hotkeySeed: ByteArray,
        networkId: Int,
        seedFingerprint: ByteArray
    ): String =
        withContext(Dispatchers.IO) {
            generateDelegationInputsWithFvkJsonNative(
                fvkBytes,
                hotkeySeed,
                networkId,
                seedFingerprint
            ) ?: error("generateDelegationInputsWithFvk returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun extractOrchardFvkFromUfvk(
        ufvk: String,
        networkId: Int
    ): ByteArray =
        withContext(Dispatchers.IO) {
            extractOrchardFvkFromUfvkNative(ufvk, networkId)
                ?: error("extractOrchardFvkFromUfvk returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun extractNcRoot(treeStateBytes: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            extractNcRootNative(treeStateBytes)
                ?: error("extractNcRoot returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun verifyWitness(witnessJson: String): Int =
        withContext(Dispatchers.IO) {
            verifyWitnessNative(witnessJson)
        }

    @Throws(RuntimeException::class)
    suspend fun getWalletNotesJson(
        walletDbPath: String,
        snapshotHeight: Long,
        networkId: Int,
        accountUuidBytes: ByteArray
    ): String =
        withContext(Dispatchers.IO) {
            getWalletNotesJsonNative(
                walletDbPath,
                snapshotHeight,
                networkId,
                accountUuidBytes
            ) ?: error("getWalletNotes returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun buildSharePayloadsJson(
        encSharesJson: String,
        commitmentJson: String,
        voteDecision: Int,
        numOptions: Int,
        vcTreePosition: Long,
        singleShareMode: Boolean
    ): String =
        withContext(Dispatchers.IO) {
            buildSharePayloadsJsonNative(
                encSharesJson,
                commitmentJson,
                voteDecision,
                numOptions,
                vcTreePosition,
                singleShareMode
            ) ?: error("buildSharePayloads returned null")
        }

    @Throws(RuntimeException::class)
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
    ): ByteArray =
        withContext(Dispatchers.IO) {
            signCastVoteNative(
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
            ) ?: error("signCastVote returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun extractPcztSighash(pcztBytes: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            extractPcztSighashNative(pcztBytes)
                ?: error("extractPcztSighash returned null")
        }

    @Throws(RuntimeException::class)
    suspend fun extractSpendAuthSig(
        signedPcztBytes: ByteArray,
        actionIndex: Int
    ): ByteArray =
        withContext(Dispatchers.IO) {
            extractSpendAuthSigNative(signedPcztBytes, actionIndex)
                ?: error("extractSpendAuthSig returned null")
        }

    suspend fun openVotingDb(dbPath: String, walletId: String): VotingDb =
        withContext(SdkDispatchers.DATABASE_IO) {
            openVotingDbNative(dbPath, walletId).let { dbHandle ->
                check(dbHandle != 0L) {
                    "openVotingDb failed for dbPath=$dbPath"
                }
                VotingDb(dbHandle)
            }
        }

    @Suppress("TooManyFunctions", "LongParameterList")
    class VotingDb internal constructor(
        private var dbHandle: Long?
    ) {
        private val accessMutex = Mutex()

        suspend fun close() {
            accessMutex.withLock {
                dbHandle?.let { handle ->
                    withContext(SdkDispatchers.DATABASE_IO) {
                        closeVotingDbNative(handle)
                    }
                    dbHandle = null
                }
            }
        }

        @Throws(RuntimeException::class)
        suspend fun initRound(
            roundId: String,
            snapshotHeight: Long,
            eaPK: ByteArray,
            ncRoot: ByteArray,
            nullifierIMTRoot: ByteArray,
            sessionJson: String?
        ) = withHandle { handle ->
            initRoundNative(
                handle,
                roundId,
                snapshotHeight,
                eaPK,
                ncRoot,
                nullifierIMTRoot,
                sessionJson
            )
        }

        @Throws(RuntimeException::class)
        suspend fun getRoundState(roundId: String): JniRoundState? =
            withHandle { handle -> getRoundStateNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun listRounds(): Array<JniRoundSummary> =
            withHandle { handle -> listRoundsNative(handle) }

        @Throws(RuntimeException::class)
        suspend fun getBundleCount(roundId: String): Int =
            withHandle { handle ->
                getBundleCountNative(handle, roundId).also { count ->
                    check(count >= 0) {
                        "getBundleCount failed for roundId=$roundId"
                    }
                }
            }

        @Throws(RuntimeException::class)
        suspend fun getVotes(roundId: String): Array<JniVoteRecord> =
            withHandle { handle -> getVotesNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun clearRound(roundId: String) =
            withHandle { handle -> clearRoundNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun deleteSkippedBundles(
            roundId: String,
            keepCount: Int
        ): Long =
            withHandle { handle -> deleteSkippedBundlesNative(handle, roundId, keepCount) }

        @Throws(RuntimeException::class)
        suspend fun setupBundles(
            roundId: String,
            notesJson: String
        ): JniBundleSetupResult =
            withHandle { handle ->
                setupBundlesNative(handle, roundId, notesJson)
                    ?: error("setupBundles returned null for roundId=$roundId")
            }

        @Throws(RuntimeException::class)
        suspend fun generateHotkey(
            roundId: String,
            seed: ByteArray
        ): JniVotingHotkey =
            withHandle { handle ->
                generateHotkeyNative(handle, roundId, seed)
                    ?: error("generateHotkey returned null for roundId=$roundId")
            }

        @Throws(RuntimeException::class)
        suspend fun buildGovernancePcztJson(
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
        ): String =
            withHandle { handle ->
                buildGovernancePcztJsonNative(
                    handle,
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
                ) ?: error("buildGovernancePczt returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun storeWitnesses(
            roundId: String,
            bundleIndex: Int,
            witnessesJson: String
        ) = withHandle { handle ->
            check(storeWitnessesNative(handle, roundId, bundleIndex, witnessesJson)) {
                "storeWitnesses failed for roundId=$roundId bundleIndex=$bundleIndex"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun precomputeDelegationPirJson(
            roundId: String,
            bundleIndex: Int,
            pirServerUrl: String,
            networkId: Int,
            notesJson: String
        ): String =
            withHandle { handle ->
                precomputeDelegationPirJsonNative(
                    handle,
                    roundId,
                    bundleIndex,
                    pirServerUrl,
                    networkId,
                    notesJson
                ) ?: error("precomputeDelegationPir returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun buildAndProveDelegationJson(
            roundId: String,
            bundleIndex: Int,
            pirServerUrl: String,
            networkId: Int,
            notesJson: String,
            hotkeyRawSeed: ByteArray,
            proofProgress: VotingProofProgressCallback?
        ): String =
            withHandle { handle ->
                buildAndProveDelegationJsonNative(
                    handle,
                    roundId,
                    bundleIndex,
                    pirServerUrl,
                    networkId,
                    notesJson,
                    hotkeyRawSeed,
                    proofProgress
                ) ?: error("buildAndProveDelegation returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun getDelegationSubmissionJson(
            roundId: String,
            bundleIndex: Int,
            senderSeed: ByteArray,
            networkId: Int,
            accountIndex: Int
        ): String =
            withHandle { handle ->
                getDelegationSubmissionJsonNative(
                    handle,
                    roundId,
                    bundleIndex,
                    senderSeed,
                    networkId,
                    accountIndex
                ) ?: error("getDelegationSubmission returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun getDelegationSubmissionWithKeystoneSigJson(
            roundId: String,
            bundleIndex: Int,
            keystoneSig: ByteArray,
            keystoneSighash: ByteArray
        ): String =
            withHandle { handle ->
                getDelegationSubmissionWithKeystoneSigJsonNative(
                    handle,
                    roundId,
                    bundleIndex,
                    keystoneSig,
                    keystoneSighash
                ) ?: error("getDelegationSubmissionWithKeystoneSig returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun syncVoteTree(roundId: String, nodeUrl: String): Long =
            withHandle { handle -> syncVoteTreeNative(handle, roundId, nodeUrl) }

        @Throws(RuntimeException::class)
        suspend fun resetTreeClient(roundId: String) =
            withHandle { handle ->
                check(resetTreeClientNative(handle, roundId)) {
                    "resetTreeClient failed for roundId=$roundId"
                }
            }

        @Throws(RuntimeException::class)
        suspend fun storeVanPosition(
            roundId: String,
            bundleIndex: Int,
            position: Int
        ) = withHandle { handle ->
            check(storeVanPositionNative(handle, roundId, bundleIndex, position)) {
                "storeVanPosition failed for roundId=$roundId bundleIndex=$bundleIndex"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun generateVanWitnessJson(
            roundId: String,
            bundleIndex: Int,
            anchorHeight: Int
        ): String =
            withHandle { handle ->
                generateVanWitnessJsonNative(handle, roundId, bundleIndex, anchorHeight)
                    ?: error("generateVanWitness returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun storeTreeState(
            roundId: String,
            treeStateBytes: ByteArray
        ) = withHandle { handle ->
            check(storeTreeStateNative(handle, roundId, treeStateBytes)) {
                "storeTreeState failed for roundId=$roundId"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun generateNoteWitnessesJson(
            roundId: String,
            bundleIndex: Int,
            walletDbPath: String,
            notesJson: String
        ): String =
            withHandle { handle ->
                generateNoteWitnessesJsonNative(
                    handle,
                    roundId,
                    bundleIndex,
                    walletDbPath,
                    notesJson
                ) ?: error("generateNoteWitnesses returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun buildVoteCommitmentJson(
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
            proofProgress: VotingProofProgressCallback?
        ): String =
            withHandle { handle ->
                buildVoteCommitmentJsonNative(
                    handle,
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
                    proofProgress
                ) ?: error("buildVoteCommitment returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun encryptSharesJson(roundId: String, sharesJson: String): String =
            withHandle { handle ->
                encryptSharesJsonNative(handle, roundId, sharesJson)
                    ?: error("encryptShares returned null")
            }

        @Throws(RuntimeException::class)
        suspend fun storeDelegationTxHash(
            roundId: String,
            bundleIndex: Int,
            txHash: String
        ) = withHandle { handle ->
            check(storeDelegationTxHashNative(handle, roundId, bundleIndex, txHash)) {
                "storeDelegationTxHash failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun getDelegationTxHash(
            roundId: String,
            bundleIndex: Int
        ): String? =
            withHandle { handle -> getDelegationTxHashNative(handle, roundId, bundleIndex) }

        @Throws(RuntimeException::class)
        suspend fun storeVoteTxHash(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            txHash: String
        ) = withHandle { handle ->
            check(storeVoteTxHashNative(handle, roundId, bundleIndex, proposalId, txHash)) {
                "storeVoteTxHash failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun markVoteSubmitted(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ) = withHandle { handle ->
            check(markVoteSubmittedNative(handle, roundId, bundleIndex, proposalId)) {
                "markVoteSubmitted failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun getVoteTxHash(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ): String? =
            withHandle { handle ->
                getVoteTxHashNative(handle, roundId, bundleIndex, proposalId)
            }

        @Throws(RuntimeException::class)
        suspend fun storeCommitmentBundle(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            bundleJson: String,
            vcTreePosition: Long
        ) = withHandle { handle ->
            check(
                storeCommitmentBundleNative(
                    handle,
                    roundId,
                    bundleIndex,
                    proposalId,
                    bundleJson,
                    vcTreePosition
                )
            ) {
                "storeCommitmentBundle failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun getCommitmentBundleJson(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ): String? =
            withHandle { handle ->
                getCommitmentBundleJsonNative(handle, roundId, bundleIndex, proposalId)
            }

        @Throws(RuntimeException::class)
        suspend fun clearRecoveryState(roundId: String) =
            withHandle { handle ->
                check(clearRecoveryStateNative(handle, roundId)) {
                    "clearRecoveryState failed"
                }
            }

        @Throws(RuntimeException::class)
        suspend fun storeKeystoneSignature(
            roundId: String,
            bundleIndex: Int,
            sig: ByteArray,
            sighash: ByteArray,
            rk: ByteArray
        ) = withHandle { handle ->
            check(storeKeystoneSignatureNative(handle, roundId, bundleIndex, sig, sighash, rk)) {
                "storeKeystoneSignature failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun getKeystoneSignaturesJson(roundId: String): String =
            withHandle { handle -> getKeystoneSignaturesJsonNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun recordShareDelegation(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int,
            sentToUrlsJson: String,
            nullifier: ByteArray,
            submitAt: Long
        ) = withHandle { handle ->
            check(
                recordShareDelegationNative(
                    handle,
                    roundId,
                    bundleIndex,
                    proposalId,
                    shareIndex,
                    sentToUrlsJson,
                    nullifier,
                    submitAt
                )
            ) {
                "recordShareDelegation failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun getShareDelegationsJson(roundId: String): String =
            withHandle { handle -> getShareDelegationsJsonNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun getUnconfirmedDelegationsJson(roundId: String): String =
            withHandle { handle -> getUnconfirmedDelegationsJsonNative(handle, roundId) }

        @Throws(RuntimeException::class)
        suspend fun markShareConfirmed(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int
        ) = withHandle { handle ->
            check(markShareConfirmedNative(handle, roundId, bundleIndex, proposalId, shareIndex)) {
                "markShareConfirmed failed"
            }
        }

        @Throws(RuntimeException::class)
        suspend fun addSentServers(
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int,
            newUrlsJson: String
        ) = withHandle { handle ->
            check(
                addSentServersNative(
                    handle,
                    roundId,
                    bundleIndex,
                    proposalId,
                    shareIndex,
                    newUrlsJson
                )
            ) {
                "addSentServers failed"
            }
        }

        private suspend fun <T> withHandle(block: (Long) -> T): T =
            accessMutex.withLock {
                val handle =
                    checkNotNull(dbHandle) {
                        "Voting DB handle is closed"
                    }
                withContext(SdkDispatchers.DATABASE_IO) {
                    block(handle)
                }
            }
    }

    companion object {
        suspend fun new(): VotingRustBackend {
            RustBackend.loadLibrary()

            return VotingRustBackend()
        }

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun computeShareNullifierNative(
            voteCommitment: ByteArray,
            shareIndex: Int,
            blind: ByteArray
        ): ByteArray

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun warmProvingCachesNative()

        @JvmStatic
        @JvmName("ballotDivisorZatoshi")
        @Throws(RuntimeException::class)
        private external fun ballotDivisorZatoshiNative(): Long

        @JvmStatic
        @JvmName("decomposeWeightJson")
        @Throws(RuntimeException::class)
        private external fun decomposeWeightJsonNative(weight: Long): String?

        @JvmStatic
        @JvmName("generateDelegationInputsJson")
        @Throws(RuntimeException::class)
        private external fun generateDelegationInputsJsonNative(
            senderSeed: ByteArray,
            hotkeySeed: ByteArray,
            networkId: Int,
            accountIndex: Int
        ): String?

        @JvmStatic
        @JvmName("generateDelegationInputsWithFvkJson")
        @Throws(RuntimeException::class)
        private external fun generateDelegationInputsWithFvkJsonNative(
            fvkBytes: ByteArray,
            hotkeySeed: ByteArray,
            networkId: Int,
            seedFingerprint: ByteArray
        ): String?

        @JvmStatic
        @JvmName("extractOrchardFvkFromUfvk")
        @Throws(RuntimeException::class)
        private external fun extractOrchardFvkFromUfvkNative(
            ufvk: String,
            networkId: Int
        ): ByteArray?

        @JvmStatic
        @JvmName("extractNcRoot")
        @Throws(RuntimeException::class)
        private external fun extractNcRootNative(treeStateBytes: ByteArray): ByteArray?

        @JvmStatic
        @JvmName("verifyWitness")
        @Throws(RuntimeException::class)
        private external fun verifyWitnessNative(witnessJson: String): Int

        @JvmStatic
        @JvmName("getWalletNotesJson")
        @Throws(RuntimeException::class)
        private external fun getWalletNotesJsonNative(
            walletDbPath: String,
            snapshotHeight: Long,
            networkId: Int,
            accountUuidBytes: ByteArray
        ): String?

        @JvmStatic
        @JvmName("buildSharePayloadsJson")
        @Throws(RuntimeException::class)
        private external fun buildSharePayloadsJsonNative(
            encSharesJson: String,
            commitmentJson: String,
            voteDecision: Int,
            numOptions: Int,
            vcTreePosition: Long,
            singleShareMode: Boolean
        ): String?

        @JvmStatic
        @JvmName("signCastVote")
        @Throws(RuntimeException::class)
        private external fun signCastVoteNative(
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
        ): ByteArray?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun openVotingDbNative(dbPath: String, walletId: String): Long

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun closeVotingDbNative(dbHandle: Long)

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun initRoundNative(
            dbHandle: Long,
            roundId: String,
            snapshotHeight: Long,
            eaPK: ByteArray,
            ncRoot: ByteArray,
            nullifierIMTRoot: ByteArray,
            sessionJson: String?
        )

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getRoundStateNative(dbHandle: Long, roundId: String): JniRoundState?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun listRoundsNative(dbHandle: Long): Array<JniRoundSummary>

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getBundleCountNative(dbHandle: Long, roundId: String): Int

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getVotesNative(dbHandle: Long, roundId: String): Array<JniVoteRecord>

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun clearRoundNative(dbHandle: Long, roundId: String)

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun deleteSkippedBundlesNative(
            dbHandle: Long,
            roundId: String,
            keepCount: Int
        ): Long

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun computeBundleSetupNative(notesJson: String): JniBundleSetupResult?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun setupBundlesNative(
            dbHandle: Long,
            roundId: String,
            notesJson: String
        ): JniBundleSetupResult?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun generateHotkeyNative(
            dbHandle: Long,
            roundId: String,
            seed: ByteArray
        ): JniVotingHotkey?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun buildGovernancePcztJsonNative(
            dbHandle: Long,
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
        ): String?

        @JvmStatic
        @JvmName("storeWitnesses")
        @Throws(RuntimeException::class)
        private external fun storeWitnessesNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            witnessesJson: String
        ): Boolean

        @JvmStatic
        @JvmName("precomputeDelegationPirJson")
        @Throws(RuntimeException::class)
        private external fun precomputeDelegationPirJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            pirServerUrl: String,
            networkId: Int,
            notesJson: String
        ): String?

        @JvmStatic
        @JvmName("buildAndProveDelegationJson")
        @Throws(RuntimeException::class)
        private external fun buildAndProveDelegationJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            pirServerUrl: String,
            networkId: Int,
            notesJson: String,
            hotkeyRawSeed: ByteArray,
            proofProgress: VotingProofProgressCallback?
        ): String?

        @JvmStatic
        @JvmName("getDelegationSubmissionJson")
        @Throws(RuntimeException::class)
        private external fun getDelegationSubmissionJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            senderSeed: ByteArray,
            networkId: Int,
            accountIndex: Int
        ): String?

        @JvmStatic
        @JvmName("getDelegationSubmissionWithKeystoneSigJson")
        @Throws(RuntimeException::class)
        private external fun getDelegationSubmissionWithKeystoneSigJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            keystoneSig: ByteArray,
            keystoneSighash: ByteArray
        ): String?

        @JvmStatic
        @JvmName("syncVoteTree")
        @Throws(RuntimeException::class)
        private external fun syncVoteTreeNative(
            dbHandle: Long,
            roundId: String,
            nodeUrl: String
        ): Long

        @JvmStatic
        @JvmName("resetTreeClient")
        @Throws(RuntimeException::class)
        private external fun resetTreeClientNative(dbHandle: Long, roundId: String): Boolean

        @JvmStatic
        @JvmName("storeVanPosition")
        @Throws(RuntimeException::class)
        private external fun storeVanPositionNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            position: Int
        ): Boolean

        @JvmStatic
        @JvmName("generateVanWitnessJson")
        @Throws(RuntimeException::class)
        private external fun generateVanWitnessJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            anchorHeight: Int
        ): String?

        @JvmStatic
        @JvmName("storeTreeState")
        @Throws(RuntimeException::class)
        private external fun storeTreeStateNative(
            dbHandle: Long,
            roundId: String,
            treeStateBytes: ByteArray
        ): Boolean

        @JvmStatic
        @JvmName("generateNoteWitnessesJson")
        @Throws(RuntimeException::class)
        private external fun generateNoteWitnessesJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            walletDbPath: String,
            notesJson: String
        ): String?

        @JvmStatic
        @JvmName("buildVoteCommitmentJson")
        @Throws(RuntimeException::class)
        private external fun buildVoteCommitmentJsonNative(
            dbHandle: Long,
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
            proofProgress: VotingProofProgressCallback?
        ): String?

        @JvmStatic
        @JvmName("encryptSharesJson")
        @Throws(RuntimeException::class)
        private external fun encryptSharesJsonNative(
            dbHandle: Long,
            roundId: String,
            sharesJson: String
        ): String?

        @JvmStatic
        @JvmName("storeDelegationTxHash")
        @Throws(RuntimeException::class)
        private external fun storeDelegationTxHashNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            txHash: String
        ): Boolean

        @JvmStatic
        @JvmName("getDelegationTxHash")
        @Throws(RuntimeException::class)
        private external fun getDelegationTxHashNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int
        ): String?

        @JvmStatic
        @JvmName("storeVoteTxHash")
        @Throws(RuntimeException::class)
        private external fun storeVoteTxHashNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            txHash: String
        ): Boolean

        @JvmStatic
        @JvmName("markVoteSubmitted")
        @Throws(RuntimeException::class)
        private external fun markVoteSubmittedNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ): Boolean

        @JvmStatic
        @JvmName("getVoteTxHash")
        @Throws(RuntimeException::class)
        private external fun getVoteTxHashNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ): String?

        @JvmStatic
        @JvmName("storeCommitmentBundle")
        @Throws(RuntimeException::class)
        private external fun storeCommitmentBundleNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            bundleJson: String,
            vcTreePosition: Long
        ): Boolean

        @JvmStatic
        @JvmName("getCommitmentBundleJson")
        @Throws(RuntimeException::class)
        private external fun getCommitmentBundleJsonNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int
        ): String?

        @JvmStatic
        @JvmName("clearRecoveryState")
        @Throws(RuntimeException::class)
        private external fun clearRecoveryStateNative(dbHandle: Long, roundId: String): Boolean

        @JvmStatic
        @JvmName("storeKeystoneSignature")
        @Throws(RuntimeException::class)
        private external fun storeKeystoneSignatureNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            sig: ByteArray,
            sighash: ByteArray,
            rk: ByteArray
        ): Boolean

        @JvmStatic
        @JvmName("getKeystoneSignaturesJson")
        @Throws(RuntimeException::class)
        private external fun getKeystoneSignaturesJsonNative(
            dbHandle: Long,
            roundId: String
        ): String

        @JvmStatic
        @JvmName("recordShareDelegation")
        @Throws(RuntimeException::class)
        private external fun recordShareDelegationNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int,
            sentToUrlsJson: String,
            nullifier: ByteArray,
            submitAt: Long
        ): Boolean

        @JvmStatic
        @JvmName("getShareDelegationsJson")
        @Throws(RuntimeException::class)
        private external fun getShareDelegationsJsonNative(
            dbHandle: Long,
            roundId: String
        ): String

        @JvmStatic
        @JvmName("getUnconfirmedDelegationsJson")
        @Throws(RuntimeException::class)
        private external fun getUnconfirmedDelegationsJsonNative(
            dbHandle: Long,
            roundId: String
        ): String

        @JvmStatic
        @JvmName("markShareConfirmed")
        @Throws(RuntimeException::class)
        private external fun markShareConfirmedNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int
        ): Boolean

        @JvmStatic
        @JvmName("addSentServers")
        @Throws(RuntimeException::class)
        private external fun addSentServersNative(
            dbHandle: Long,
            roundId: String,
            bundleIndex: Int,
            proposalId: Int,
            shareIndex: Int,
            newUrlsJson: String
        ): Boolean

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun extractPcztSighashNative(pcztBytes: ByteArray): ByteArray?

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun extractSpendAuthSigNative(
            signedPcztBytes: ByteArray,
            actionIndex: Int
        ): ByteArray?
    }
}
