package cash.z.ecc.android.sdk.internal.jni

internal object VotingRustBackend {
    @JvmStatic
    external fun computeShareNullifier(
        voteCommitment: ByteArray,
        shareIndex: Int,
        blind: ByteArray
    ): ByteArray?
}
