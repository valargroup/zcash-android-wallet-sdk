use super::helpers::*;
use super::json::*;
use super::*;

const HOTKEY_ACCOUNT_INDEX: u32 = 0;

fn derive_spending_key(
    network: Network,
    seed: &[u8],
    account_index: u32,
) -> anyhow::Result<UnifiedSpendingKey> {
    let account = zip32::AccountId::try_from(account_index)
        .map_err(|_| anyhow!("account_index must be < 2^31, got {}", account_index))?;
    UnifiedSpendingKey::from_seed(&network, seed, account)
        .map_err(|e| anyhow!("failed to derive UnifiedSpendingKey: {}", e))
}

fn delegation_inputs_json(
    fvk_bytes: Vec<u8>,
    hotkey_seed: &[u8],
    network_id: jint,
    seed_fingerprint: Vec<u8>,
) -> anyhow::Result<JsonDelegationInputs> {
    let network = network_from_id(network_id)?;
    let hotkey_raw_address =
        hotkey_orchard_raw_address(hotkey_seed, network, HOTKEY_ACCOUNT_INDEX)?;
    let hotkey_raw_address_arr: [u8; ORCHARD_RAW_ADDRESS_BYTES] = hotkey_raw_address
        .as_slice()
        .try_into()
        .map_err(|_| anyhow!("hotkey_raw_address must be {ORCHARD_RAW_ADDRESS_BYTES} bytes"))?;
    let (g_d_new_x, pk_d_new_x) =
        voting::action::derive_hotkey_x_coords_from_raw_address(&hotkey_raw_address_arr)
            .map_err(|e| anyhow!("derive_hotkey_x_coords: {}", e))?;
    let hotkey = voting::hotkey::generate_hotkey(hotkey_seed)
        .map_err(|e| anyhow!("generate_hotkey: {}", e))?;

    Ok(JsonDelegationInputs {
        fvk_bytes: hex_enc(&fvk_bytes),
        g_d_new_x: hex_enc(g_d_new_x.as_slice()),
        pk_d_new_x: hex_enc(pk_d_new_x.as_slice()),
        hotkey_raw_address: hex_enc(&hotkey_raw_address),
        hotkey_public_key: hex_enc(&hotkey.public_key),
        hotkey_address: hotkey.address,
        seed_fingerprint: hex_enc(&seed_fingerprint),
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_warmProvingCachesNative<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) {
    let res = catch_unwind(&mut env, |_env| {
        voting::warm_proving_caches();
        Ok(())
    });
    unwrap_exc_or(&mut env, res, ())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_ballotDivisorZatoshi<
    'local,
>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jlong {
    voting::BALLOT_DIVISOR as jlong
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_decomposeWeightJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    weight: jlong,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let parts = voting::decompose::decompose_weight(jlong_to_u64(weight, "weight")?);
        json_to_jstring(env, &parts)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_generateDelegationInputsJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    sender_seed: JByteArray<'local>,
    hotkey_seed: JByteArray<'local>,
    network_id: jint,
    account_index: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let network = network_from_id(network_id)?;
        let sender_seed =
            java_secret_bytes_at_least(env, &sender_seed, "senderSeed", PROTOCOL_FIELD_BYTES)?;
        let hotkey_seed =
            java_secret_bytes_at_least(env, &hotkey_seed, "hotkeySeed", PROTOCOL_FIELD_BYTES)?;
        let sender_usk = derive_spending_key(
            network,
            sender_seed.expose_secret(),
            jint_to_u32(account_index, "account_index")?,
        )?;
        let sender_fvk = sender_usk
            .to_unified_full_viewing_key()
            .orchard()
            .ok_or_else(|| anyhow!("sender UFVK is missing Orchard component"))?
            .to_bytes()
            .to_vec();
        let seed_fingerprint =
            zip32::fingerprint::SeedFingerprint::from_seed(sender_seed.expose_secret())
                .ok_or_else(|| anyhow!("failed to compute seed fingerprint"))?
                .to_bytes()
                .to_vec();
        let inputs = delegation_inputs_json(
            sender_fvk,
            hotkey_seed.expose_secret(),
            network_id,
            seed_fingerprint,
        )?;
        json_to_jstring(env, &inputs)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_generateDelegationInputsWithFvkJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    fvk_bytes: JByteArray<'local>,
    hotkey_seed: JByteArray<'local>,
    network_id: jint,
    seed_fingerprint: JByteArray<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let fvk = java_bytes_exact(env, &fvk_bytes, "fvkBytes", ORCHARD_FVK_BYTES)?;
        let hotkey_seed =
            java_secret_bytes_at_least(env, &hotkey_seed, "hotkeySeed", PROTOCOL_FIELD_BYTES)?;
        let seed_fingerprint = java_bytes_exact(
            env,
            &seed_fingerprint,
            "seedFingerprint",
            PROTOCOL_FIELD_BYTES,
        )?;
        let inputs = delegation_inputs_json(
            fvk,
            hotkey_seed.expose_secret(),
            network_id,
            seed_fingerprint,
        )?;
        json_to_jstring(env, &inputs)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_extractOrchardFvkFromUfvk<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    ufvk: JString<'local>,
    network_id: jint,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let network = network_from_id(network_id)?;
        let bytes = orchard_fvk_bytes(&java_string_to_rust(env, &ufvk)?, network)?;
        Ok(env.byte_array_from_slice(&bytes)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_extractNcRoot<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    tree_state_bytes: JByteArray<'local>,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        use prost::Message;
        use zcash_client_backend::proto::service::TreeState;

        let bytes = java_bytes(env, &tree_state_bytes, "treeStateBytes")?;
        let tree_state =
            TreeState::decode(bytes.as_slice()).map_err(|e| anyhow!("decode TreeState: {}", e))?;
        let orchard_ct = tree_state
            .orchard_tree()
            .map_err(|e| anyhow!("parse orchard_tree: {}", e))?;
        let nc_root = orchard_ct.root().to_bytes();
        Ok(env.byte_array_from_slice(&nc_root)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_verifyWitness<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    witness_json: JString<'local>,
) -> jint {
    let res = catch_unwind(&mut env, |env| {
        let json_witness: JsonWitnessData = json_from_jstring(env, &witness_json, "witnessJson")?;
        let witness = WitnessData::try_from(json_witness)?;
        let valid = voting::witness::verify_witness(&witness)
            .map_err(|e| anyhow!("verify_witness: {}", e))?;
        Ok(if valid { 1 } else { 0 })
    });
    unwrap_exc_or(&mut env, res, -1)
}
