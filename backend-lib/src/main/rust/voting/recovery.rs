use super::db::*;
use super::helpers::*;
use super::json::*;
use super::*;

// =============================================================================
// M. Recovery state
// =============================================================================
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeDelegationTxHash<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    tx_hash: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .store_delegation_tx_hash(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                &java_string_to_rust(env, &tx_hash)?,
            )
            .map_err(|e| anyhow!("store_delegation_tx_hash: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getDelegationTxHash<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let tx_hash = optional_recovery_lookup(
            handle.db.get_delegation_tx_hash(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
            ),
            "get_delegation_tx_hash",
        )?;
        match tx_hash {
            Some(value) => Ok(env.new_string(value)?.into_raw()),
            None => Ok(std::ptr::null_mut()),
        }
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeVoteTxHash<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
    tx_hash: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .store_vote_tx_hash(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
                &java_string_to_rust(env, &tx_hash)?,
            )
            .map_err(|e| anyhow!("store_vote_tx_hash: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_markVoteSubmitted<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .mark_vote_submitted(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
            )
            .map_err(|e| anyhow!("mark_vote_submitted: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getVoteTxHash<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let tx_hash = optional_recovery_lookup(
            handle.db.get_vote_tx_hash(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
            ),
            "get_vote_tx_hash",
        )?;
        match tx_hash {
            Some(value) => Ok(env.new_string(value)?.into_raw()),
            None => Ok(std::ptr::null_mut()),
        }
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

fn optional_recovery_lookup<T, E>(
    result: Result<Option<T>, E>,
    label: &str,
) -> anyhow::Result<Option<T>>
where
    E: std::fmt::Display,
{
    // These JNI methods are cache probes: a missing row is an expected NotFound
    // state even when the lower storage layer reports it as QueryReturnedNoRows.
    match result {
        Ok(value) => Ok(value),
        Err(error) if is_query_returned_no_rows(&error) => Ok(None),
        Err(error) => Err(anyhow!("{label}: {error}")),
    }
}

fn is_query_returned_no_rows(error: &impl std::fmt::Display) -> bool {
    error
        .to_string()
        .to_ascii_lowercase()
        .contains("query returned no rows")
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeCommitmentBundle<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
    bundle_json: JString<'local>,
    vc_tree_position: jlong,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .store_commitment_bundle(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
                &java_string_to_rust(env, &bundle_json)?,
                jlong_to_u64(vc_tree_position, "vc_tree_position")?,
            )
            .map_err(|e| anyhow!("store_commitment_bundle: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getCommitmentBundleJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let record = optional_recovery_lookup(
            handle.db.get_commitment_bundle(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
            ),
            "get_commitment_bundle",
        )?;
        match record {
            Some((bundle_json, vc_tree_position)) => json_to_jstring(
                env,
                &JsonCommitmentBundleRecord {
                    bundle_json,
                    vc_tree_position,
                },
            ),
            None => Ok(std::ptr::null_mut()),
        }
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn optional_recovery_lookup_maps_missing_rows_to_none() {
        let result: anyhow::Result<Option<String>> =
            optional_recovery_lookup(Err("Query returned no rows"), "get_vote_tx_hash");

        assert!(result.unwrap().is_none());
    }

    #[test]
    fn optional_recovery_lookup_keeps_unexpected_errors_fatal() {
        let result: anyhow::Result<Option<String>> =
            optional_recovery_lookup(Err("database is locked"), "get_vote_tx_hash");

        let error = result.unwrap_err().to_string();
        assert!(error.contains("get_vote_tx_hash"));
        assert!(error.contains("database is locked"));
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_clearRecoveryState<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .clear_recovery_state(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("clear_recovery_state: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeKeystoneSignature<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    sig: JByteArray<'local>,
    sighash: JByteArray<'local>,
    rk: JByteArray<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .store_keystone_signature(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                &java_bytes(env, &sig, "sig")?,
                &java_bytes(env, &sighash, "sighash")?,
                &java_bytes(env, &rk, "rk")?,
            )
            .map_err(|e| anyhow!("store_keystone_signature: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getKeystoneSignaturesJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let signatures: Vec<JsonKeystoneSignature> = handle
            .db
            .get_keystone_signatures(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("get_keystone_signatures: {}", e))?
            .into_iter()
            .map(|s| JsonKeystoneSignature {
                bundle_index: s.bundle_index,
                sig: hex_enc(&s.sig),
                sighash: hex_enc(&s.sighash),
                rk: hex_enc(&s.rk),
            })
            .collect();
        json_to_jstring(env, &signatures)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}
