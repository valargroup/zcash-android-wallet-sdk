use super::db::*;
use super::helpers::*;
use super::json::*;
use super::*;

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_initRound<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    snapshot_height: jlong,
    ea_pk: JByteArray<'local>,
    nc_root: JByteArray<'local>,
    nullifier_imt_root: JByteArray<'local>,
    session_json: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let params = voting::types::VotingRoundParams {
            vote_round_id: java_string_to_rust(env, &round_id)?,
            snapshot_height: jlong_to_u64(snapshot_height, "snapshot_height")?,
            ea_pk: java_bytes_exact(env, &ea_pk, "ea_pk", PROTOCOL_FIELD_BYTES)?,
            nc_root: java_bytes_exact(env, &nc_root, "nc_root", PROTOCOL_FIELD_BYTES)?,
            nullifier_imt_root: java_bytes_exact(
                env,
                &nullifier_imt_root,
                "nullifier_imt_root",
                PROTOCOL_FIELD_BYTES,
            )?,
        };
        let session = java_nullable_string_to_rust(env, &session_json)?;
        handle
            .db
            .init_round(&params, session.as_deref())
            .map_err(|e| anyhow!("init_round: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getRoundState<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        match handle
            .db
            .get_round_state(&java_string_to_rust(env, &round_id)?)
        {
            Ok(state) => make_ffi_round_state(env, state),
            Err(VotingError::InvalidInput { .. }) => Ok(JObject::null().into_raw()),
            Err(e) => Err(anyhow!("get_round_state: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, JObject::null().into_raw())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_listRoundsJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let rounds: Vec<JsonRoundSummary> = handle
            .db
            .list_rounds()
            .map_err(|e| anyhow!("list_rounds: {}", e))?
            .into_iter()
            .map(JsonRoundSummary::from)
            .collect();
        json_to_jstring(env, &rounds)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getVotesJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let votes: Vec<JsonVoteRecord> = handle
            .db
            .get_votes(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("get_votes: {}", e))?
            .into_iter()
            .map(JsonVoteRecord::from)
            .collect();
        json_to_jstring(env, &votes)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_clearRound<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        handle
            .db
            .clear_round(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("clear_round: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_deleteSkippedBundles<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    keep_count: jint,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let deleted_rows = handle
            .db
            .delete_skipped_bundles(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(keep_count, "keep_count")?,
            )
            .map_err(|e| anyhow!("delete_skipped_bundles: {}", e))?;
        Ok(deleted_rows as jlong)
    });
    unwrap_exc_or(&mut env, res, -1)
}
