package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.internal.jni.VotingRustBackend
import cash.z.ecc.android.sdk.internal.model.voting.FfiRoundState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Suppress("TooManyFunctions", "LongParameterList")
class TypesafeVotingBackendImpl : TypesafeVotingBackend {
    override suspend fun openVotingDb(dbPath: String): Long =
        io {
            RustBackend.loadLibrary()
            VotingRustBackend.openVotingDb(dbPath).also { dbHandle ->
                check(dbHandle != 0L) {
                    "openVotingDb failed for dbPath=$dbPath"
                }
            }
        }

    override suspend fun closeVotingDb(dbHandle: Long) =
        io { VotingRustBackend.closeVotingDb(dbHandle) }

    override suspend fun setWalletId(dbHandle: Long, walletId: String) =
        io {
            check(VotingRustBackend.setWalletId(dbHandle, walletId)) {
                "setWalletId failed"
            }
        }

    override suspend fun initRound(
        dbHandle: Long,
        roundId: String,
        snapshotHeight: Long,
        eaPK: ByteArray,
        ncRoot: ByteArray,
        nullifierIMTRoot: ByteArray,
        sessionJson: String?
    ) = io {
        check(
            VotingRustBackend.initRound(
                dbHandle,
                roundId,
                snapshotHeight,
                eaPK,
                ncRoot,
                nullifierIMTRoot,
                sessionJson
            )
        ) {
            "initRound failed for roundId=$roundId"
        }
    }

    override suspend fun getRoundState(dbHandle: Long, roundId: String): FfiRoundState? =
        io { VotingRustBackend.getRoundState(dbHandle, roundId) }

    override suspend fun listRoundsJson(dbHandle: Long): String =
        io { VotingRustBackend.listRoundsJson(dbHandle) }

    override suspend fun getVotes(dbHandle: Long, roundId: String): List<VoteRecord> =
        io {
            val arr = JSONArray(VotingRustBackend.getVotesJson(dbHandle, roundId))
            (0 until arr.length()).map { index ->
                val obj = arr.getJSONObject(index)
                VoteRecord(
                    proposalId = obj.getInt("proposal_id"),
                    bundleIndex = obj.getInt("bundle_index"),
                    choice = obj.getInt("choice"),
                    submitted = obj.getBoolean("submitted")
                )
            }
        }

    override suspend fun clearRound(dbHandle: Long, roundId: String) =
        io {
            check(VotingRustBackend.clearRound(dbHandle, roundId)) {
                "clearRound failed for roundId=$roundId"
            }
        }

    override suspend fun deleteSkippedBundles(
        dbHandle: Long,
        roundId: String,
        keepCount: Int
    ): Long =
        io {
            VotingRustBackend
                .deleteSkippedBundles(dbHandle, roundId, keepCount)
                .also { deletedRows ->
                    check(deletedRows >= 0) {
                        "deleteSkippedBundles failed for roundId=$roundId keepCount=$keepCount"
                    }
                }
        }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
