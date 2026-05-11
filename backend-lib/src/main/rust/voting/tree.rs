use super::db::*;
use super::helpers::*;
use super::json::*;
use super::*;

// =============================================================================
// H. Vote commitment tree (VAN witness)
// =============================================================================

/// Synchronise the per-round vote commitment tree from the chain node.
///
/// Returns the latest synced block height as a Long, or -1 on error.
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_syncVoteTree<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    node_url: JString<'local>,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let height = handle
            .tree_sync
            .sync(
                &handle.db,
                &java_string_to_rust(env, &round_id)?,
                &java_string_to_rust(env, &node_url)?,
            )
            .map_err(|e| anyhow!("sync_vote_tree: {}", e))?;
        Ok(height as jlong)
    });
    unwrap_exc_or(&mut env, res, -1)
}

/// Store the VAN leaf position after the delegation TX is confirmed.
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeVanPosition<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    position: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let position = jint_to_u32(position, "position")?;
        handle
            .db
            .store_van_position(
                &java_string_to_rust(env, &round_id)?,
                bundle_index,
                position,
            )
            .map_err(|e| anyhow!("store_van_position: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

/// Generate the Merkle witness for the VAN note. Must be called after [syncVoteTree].
///
/// Returns JSON-encoded VanWitness, or null on error.
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_generateVanWitnessJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    anchor_height: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let anchor_height = jint_to_u32(anchor_height, "anchor_height")?;
        let witness = handle
            .tree_sync
            .generate_van_witness(
                &handle.db,
                &java_string_to_rust(env, &round_id)?,
                bundle_index,
                anchor_height,
            )
            .map_err(|e| anyhow!("generate_van_witness: {}", e))?;
        json_to_jstring(env, &JsonVanWitness::from(witness))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_resetTreeClient<
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
            .tree_sync
            .reset(&java_string_to_rust(env, &round_id)?)
            .map_err(|e| anyhow!("reset_tree_client: {}", e))?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}
