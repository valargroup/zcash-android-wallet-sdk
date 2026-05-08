use super::helpers::*;
use super::*;

/// Compute the share reveal nullifier from client-known inputs.
///
/// Returns the 32-byte nullifier, or throws a RuntimeException and returns null
/// on malformed inputs.
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_computeShareNullifier<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    vote_commitment: JByteArray<'local>,
    share_index: jint,
    blind: JByteArray<'local>,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let nullifier = voting::share_tracking::compute_share_nullifier(
            &java_bytes_exact(
                env,
                &vote_commitment,
                "vote_commitment",
                PROTOCOL_FIELD_BYTES,
            )?,
            jint_to_u32(share_index, "share_index")?,
            &java_bytes_exact(env, &blind, "blind", PROTOCOL_FIELD_BYTES)?,
        )
        .map_err(|e| anyhow!("compute_share_nullifier: {}", e))?;
        Ok(env.byte_array_from_slice(&nullifier)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}
