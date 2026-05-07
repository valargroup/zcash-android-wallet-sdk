package cash.z.ecc.android.sdk.internal.jni

internal object VotingRustBackend {
    suspend fun new(): VotingRustBackend {
        RustBackend.loadLibrary()
        return this
    }

    @JvmStatic
    external fun computeShareNullifier(
        voteCommitment: ByteArray,
        primaryBlind: ByteArray,
        shareIndex: Int
    ): ByteArray?
}
