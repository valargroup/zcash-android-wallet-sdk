use super::db::*;
use super::helpers::*;
use super::json::*;
use super::progress::*;
use super::*;

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_buildGovernancePcztJsonNative<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    ufvk: JString<'local>,
    network_id: jint,
    account_index: jint,
    notes_json: JString<'local>,
    wallet_seed: JByteArray<'local>,
    seed_fingerprint: JByteArray<'local>,
    round_name: JString<'local>,
    address_index: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let network = network_from_id(network_id)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let account_index = jint_to_u32(account_index, "account_index")?;
        let address_index = jint_to_u32(address_index, "address_index")?;
        let ufvk_str = java_string_to_rust(env, &ufvk)?;
        let fvk_bytes = orchard_fvk_bytes(&ufvk_str, network)?;

        let seed_bytes =
            java_secret_bytes_at_least(env, &wallet_seed, "walletSeed", PROTOCOL_FIELD_BYTES)?;
        let derived_fvk_bytes =
            orchard_fvk_bytes_from_wallet_seed(seed_bytes.expose_secret(), network, account_index)?;
        if derived_fvk_bytes != fvk_bytes {
            return Err(anyhow!(
                "ufvk does not match walletSeed for network_id={network_id} account_index={account_index}"
            ));
        }
        let hotkey_raw_address = hotkey_orchard_raw_address_from_wallet_seed(
            seed_bytes.expose_secret(),
            network,
            account_index,
            address_index,
        )?;
        let seed_fingerprint = java_bytes32(env, &seed_fingerprint, "seedFingerprint")?;

        let json_notes: Vec<JsonNoteInfo> = json_from_jstring(env, &notes_json, "notesJson")?;
        let notes: Vec<NoteInfo> = json_notes
            .into_iter()
            .map(NoteInfo::try_from)
            .collect::<anyhow::Result<_>>()?;
        let bundle_notes = bundled_notes_for_index(&notes, bundle_index)?;

        let round_id = java_string_to_rust(env, &round_id)?;
        require_persisted_bundle_notes(&db, &round_id, bundle_index, &bundle_notes)?;
        let round_name = java_string_to_rust(env, &round_name)?;
        let pczt = build_paired_governance_pczt(
            &db,
            &round_id,
            bundle_index,
            &bundle_notes,
            &fvk_bytes,
            &hotkey_raw_address,
            network,
            &seed_fingerprint,
            account_index,
            &round_name,
            address_index,
        )?;
        update_round_phase_forward(&db, &round_id, RoundPhase::DelegationConstructed)?;

        json_to_jstring(env, &JsonGovernancePczt::try_from(pczt)?)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

fn governance_pczt_output_paired_with_signed_action(
    governance_pczt: &GovernancePczt,
) -> anyhow::Result<bool> {
    let pczt = pczt::Pczt::parse(&governance_pczt.pczt_bytes)
        .map_err(|e| anyhow!("failed to parse governance PCZT: {e:?}"))?;
    let action = pczt
        .orchard()
        .actions()
        .get(governance_pczt.action_index)
        .ok_or_else(|| {
            anyhow!(
                "governance PCZT action_index {} is out of range for {} actions",
                governance_pczt.action_index,
                pczt.orchard().actions().len()
            )
        })?;

    Ok(action.output().cmx().as_slice() == governance_pczt.cmx_new.as_slice())
}

fn build_paired_governance_pczt(
    db: &VotingDb,
    round_id: &str,
    bundle_index: u32,
    bundle_notes: &[NoteInfo],
    fvk_bytes: &[u8],
    hotkey_raw_address: &[u8],
    network: Network,
    seed_fingerprint: &[u8; PROTOCOL_FIELD_BYTES],
    account_index: u32,
    round_name: &str,
    address_index: u32,
) -> anyhow::Result<GovernancePczt> {
    let mut last_unpaired = None;
    for attempt in 1..=MAX_GOVERNANCE_PCZT_PAIRING_ATTEMPTS {
        let candidate = db
            .build_governance_pczt(
                round_id,
                bundle_index,
                bundle_notes,
                fvk_bytes,
                hotkey_raw_address,
                nu6_branch_id(),
                network.coin_type(),
                seed_fingerprint,
                account_index,
                round_name,
                address_index,
            )
            .map_err(|e| anyhow!("build_governance_pczt: {}", e))?;
        if governance_pczt_output_paired_with_signed_action(&candidate)? {
            if attempt > 1 {
                tracing::info!(
                    "voting delegation: rebuilt governance PCZT after unpaired action layout \
                     (bundle_index={}, attempts={})",
                    bundle_index,
                    attempt
                );
            }
            return Ok(candidate);
        }
        last_unpaired = Some(candidate);
    }

    let action_index = last_unpaired
        .as_ref()
        .map(|pczt| pczt.action_index.to_string())
        .unwrap_or_else(|| "unknown".to_string());
    Err(anyhow!(
        "build_governance_pczt produced unpaired governance output after {} attempts \
         (last action_index={action_index})",
        MAX_GOVERNANCE_PCZT_PAIRING_ATTEMPTS
    ))
}

const MAX_GOVERNANCE_PCZT_PAIRING_ATTEMPTS: u32 = 16;

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_extractPcztSighashNative<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    pczt_bytes: JByteArray<'local>,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let bytes = java_bytes(env, &pczt_bytes, "pcztBytes")?;
        let sighash = voting::action::extract_pczt_sighash(&bytes)
            .map_err(|e| anyhow!("extract_pczt_sighash: {}", e))?;
        Ok(env.byte_array_from_slice(&sighash)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_extractSpendAuthSigNative<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    signed_pczt_bytes: JByteArray<'local>,
    action_index: jint,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let bytes = java_bytes(env, &signed_pczt_bytes, "signedPcztBytes")?;
        let action_index = jint_to_usize(action_index, "action_index")?;
        let sig = voting::action::extract_spend_auth_sig(&bytes, action_index)
            .map_err(|e| anyhow!("extract_spend_auth_sig: {}", e))?;
        Ok(env.byte_array_from_slice(&sig)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_storeWitnesses<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    witnesses_json: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let round_id = java_string_to_rust(env, &round_id)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let json_witnesses: Vec<JsonWitnessData> =
            json_from_jstring(env, &witnesses_json, "witnessesJson")?;
        let witnesses = json_witnesses
            .into_iter()
            .map(WitnessData::try_from)
            .collect::<anyhow::Result<Vec<_>>>()?;
        let conn = db.conn();
        replace_bundle_witnesses(&conn, &round_id, &db.wallet_id(), bundle_index, &witnesses)?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

fn connect_pir_client(pir_url: &str) -> anyhow::Result<voting::PirClientBlocking> {
    voting::PirClientBlocking::with_transport(pir_url, Arc::new(voting::HyperTransport::new()))
        .map_err(|e| anyhow!("connect to PIR server failed: {}", e))
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_precomputeDelegationPirJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    pir_server_url: JString<'local>,
    network_id: jint,
    notes_json: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let network_id = network_id_from_jint(network_id)?;
        let json_notes: Vec<JsonNoteInfo> = json_from_jstring(env, &notes_json, "notesJson")?;
        let notes = json_notes
            .into_iter()
            .map(NoteInfo::try_from)
            .collect::<anyhow::Result<Vec<_>>>()?;
        let pir_url = java_string_to_rust(env, &pir_server_url)?;
        let pir_client = connect_pir_client(&pir_url)?;

        let result = db
            .precompute_delegation_pir(
                &java_string_to_rust(env, &round_id)?,
                bundle_index,
                &notes,
                &pir_client,
                network_id,
            )
            .map_err(|e| anyhow!("precompute_delegation_pir: {}", e))?;

        json_to_jstring(env, &JsonDelegationPirPrecomputeResult::from(result))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_buildAndProveDelegationJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    pir_server_url: JString<'local>,
    network_id: jint,
    notes_json: JString<'local>,
    hotkey_raw_seed: JByteArray<'local>,
    progress_callback: JObject<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let network = network_from_id(network_id)?;
        let bundle_index = jint_to_u32(bundle_index, "bundle_index")?;
        let network_id = network_id_from_jint(network_id)?;
        let seed_bytes = java_secret_bytes_at_least(
            env,
            &hotkey_raw_seed,
            "hotkeyRawSeed",
            PROTOCOL_FIELD_BYTES,
        )?;
        let hotkey_raw_address =
            hotkey_orchard_raw_address(seed_bytes.expose_secret(), network, 0)?;

        let json_notes: Vec<JsonNoteInfo> = json_from_jstring(env, &notes_json, "notesJson")?;
        let notes = json_notes
            .into_iter()
            .map(NoteInfo::try_from)
            .collect::<anyhow::Result<Vec<_>>>()?;
        let round_id = java_string_to_rust(env, &round_id)?;
        let bundle_notes = bundled_notes_for_index(&notes, bundle_index)?;
        require_persisted_bundle_notes(&db, &round_id, bundle_index, &bundle_notes)?;

        let pir_url = java_string_to_rust(env, &pir_server_url)?;
        let pir_client = connect_pir_client(&pir_url)?;
        let reporter = progress_reporter_from_callback(env, &progress_callback)?;

        let result = db
            .build_and_prove_delegation(
                &round_id,
                bundle_index,
                &notes,
                &hotkey_raw_address,
                &pir_client,
                network_id,
                reporter.as_ref(),
            )
            .map_err(|e| anyhow!("build_and_prove_delegation: {}", e))?;

        json_to_jstring(env, &JsonDelegationProofResult::from(result))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getDelegationSubmissionJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    sender_seed: JByteArray<'local>,
    network_id: jint,
    account_index: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let seed =
            java_secret_bytes_at_least(env, &sender_seed, "senderSeed", PROTOCOL_FIELD_BYTES)?;
        let data = db
            .get_delegation_submission(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                seed.expose_secret(),
                network_id_from_jint(network_id)?,
                jint_to_u32(account_index, "account_index")?,
            )
            .map_err(|e| anyhow!("get_delegation_submission: {}", e))?;
        json_to_jstring(env, &JsonDelegationSubmission::from(data))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_getDelegationSubmissionWithKeystoneSigJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    keystone_sig: JByteArray<'local>,
    keystone_sighash: JByteArray<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let db = db_from_handle(db_handle)?;
        let data = db
            .get_delegation_submission_with_keystone_sig(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                &java_bytes_exact(env, &keystone_sig, "keystoneSig", 64)?,
                &java_bytes_exact(
                    env,
                    &keystone_sighash,
                    "keystoneSighash",
                    PROTOCOL_FIELD_BYTES,
                )?,
            )
            .map_err(|e| anyhow!("get_delegation_submission_with_keystone_sig: {}", e))?;
        json_to_jstring(env, &JsonDelegationSubmission::from(data))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

#[cfg(test)]
mod tests {
    use super::*;
    use orchard::keys::{FullViewingKey, Scope, SpendAuthorizingKey, SpendingKey};
    use voting::types::VotingRoundParams;

    #[test]
    fn extract_spend_auth_sig_accepts_signed_governance_pczt() {
        let spending_key = SpendingKey::from_bytes([0x42; 32]).expect("valid spending key");
        let fvk = FullViewingKey::from(&spending_key);
        let hotkey_spending_key = SpendingKey::from_bytes([0x43; 32]).expect("valid hotkey");
        let hotkey_fvk = FullViewingKey::from(&hotkey_spending_key);
        let hotkey_address = hotkey_fvk
            .address_at(0u32, Scope::External)
            .to_raw_address_bytes()
            .to_vec();
        let result = voting::action::build_governance_pczt(
            &[note_info()],
            &round_params(),
            &fvk.to_bytes().to_vec(),
            &hotkey_address,
            nu6_branch_id(),
            Network::TestNetwork.coin_type(),
            &[0xAA; 32],
            0,
            "Test Round",
        )
        .expect("governance PCZT");

        let pczt = pczt::Pczt::parse(&result.pczt_bytes).expect("parse PCZT");
        let mut signer = pczt::roles::signer::Signer::new(pczt).expect("signer");
        let spend_authorizing_key = SpendAuthorizingKey::from(&spending_key);
        signer
            .sign_orchard(result.action_index, &spend_authorizing_key)
            .expect("sign orchard action");
        let signed_pczt = signer.finish().serialize();
        let sig =
            voting::action::extract_spend_auth_sig(&signed_pczt, result.action_index).unwrap();

        assert_ne!(sig, [0u8; 64]);
    }

    fn note_info() -> NoteInfo {
        NoteInfo {
            commitment: vec![1; PROTOCOL_FIELD_BYTES],
            nullifier: vec![2; PROTOCOL_FIELD_BYTES],
            value: 15_000_000,
            position: 0,
            diversifier: vec![0; 11],
            rho: vec![0; PROTOCOL_FIELD_BYTES],
            rseed: vec![0; PROTOCOL_FIELD_BYTES],
            scope: 0,
            ufvk_str: String::new(),
        }
    }

    fn round_params() -> VotingRoundParams {
        VotingRoundParams {
            vote_round_id: "0101010101010101010101010101010101010101010101010101010101010101"
                .to_string(),
            snapshot_height: 100_000,
            ea_pk: vec![0xEA; PROTOCOL_FIELD_BYTES],
            nc_root: vec![0x01; PROTOCOL_FIELD_BYTES],
            nullifier_imt_root: vec![0x02; PROTOCOL_FIELD_BYTES],
        }
    }
}
