use super::db::*;
use super::helpers::*;
use super::json::*;
use super::*;

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_recordShareDelegation<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
    share_index: jint,
    sent_to_urls_json: JString<'local>,
    nullifier: JByteArray<'local>,
    submit_at: jlong,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let sent_to_urls: Vec<String> =
            json_from_jstring(env, &sent_to_urls_json, "sentToUrlsJson")?;
        handle
            .db
            .record_share_delegation(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
                jint_to_u32(share_index, "share_index")?,
                &sent_to_urls,
                &java_bytes_exact(env, &nullifier, "nullifier", PROTOCOL_FIELD_BYTES)?,
                jlong_to_u64(submit_at, "submit_at")?,
            )
            .map_err(|e| anyhow!("record_share_delegation: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getShareDelegationsJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let records: Vec<JsonShareDelegationRecord> = handle
            .db
            .get_share_delegations(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("get_share_delegations: {}", e))?
            .into_iter()
            .map(JsonShareDelegationRecord::from)
            .collect();
        json_to_jstring(env, &records)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_markShareConfirmed<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
    share_index: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .mark_share_confirmed(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
                jint_to_u32(share_index, "share_index")?,
            )
            .map_err(|e| anyhow!("mark_share_confirmed: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_addSentServers<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    proposal_id: jint,
    share_index: jint,
    new_urls_json: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let new_urls: Vec<String> = json_from_jstring(env, &new_urls_json, "newUrlsJson")?;
        handle
            .db
            .add_sent_servers(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                jint_to_u32(proposal_id, "proposal_id")?,
                jint_to_u32(share_index, "share_index")?,
                &new_urls,
            )
            .map_err(|e| anyhow!("add_sent_servers: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_computeShareNullifierNative<
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
            &java_fixed_bytes::<VOTE_COMMITMENT_BYTES>(env, &vote_commitment, "voteCommitment")?,
            jint_to_u32(share_index, "share_index")?,
            &java_fixed_bytes::<BLIND_BYTES>(env, &blind, "blind")?,
        )
        .map_err(|e| anyhow!("compute_share_nullifier: {}", e))?;
        let nullifier = fixed_bytes::<SHARE_NULLIFIER_BYTES>(nullifier, "shareNullifier")?;
        Ok(env.byte_array_from_slice(&nullifier)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}
