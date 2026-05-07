//! JNI bindings for the zcash_voting crate.

use std::ptr;

use anyhow::anyhow;
use jni::{
    JNIEnv,
    objects::{JByteArray, JClass},
    sys::{jbyteArray, jint},
};
use zcash_voting as voting;

use crate::utils::{self, catch_unwind, exception::unwrap_exc_or};

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
        let share_index =
            u32::try_from(share_index).map_err(|_| anyhow!("shareIndex must be non-negative"))?;
        let vote_commitment = utils::java_bytes_to_rust(env, &vote_commitment)?;
        let blind = utils::java_bytes_to_rust(env, &blind)?;

        let nullifier =
            voting::share_tracking::compute_share_nullifier(&vote_commitment, share_index, &blind)
                .map_err(|e| anyhow!("compute_share_nullifier failed: {}", e))?;

        Ok(utils::rust_bytes_to_java(env, &nullifier)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}
