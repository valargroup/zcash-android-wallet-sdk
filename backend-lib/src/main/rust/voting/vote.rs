use super::db::*;
use super::helpers::*;
use super::json::*;
use super::progress::*;
use super::*;

// =============================================================================
// H. Share encryption
// =============================================================================

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_encryptSharesJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    shares_json: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let shares: Vec<u64> = json_from_jstring(env, &shares_json, "sharesJson")?;
        let encrypted = handle
            .db
            .encrypt_shares(&java_string_to_rust(env, &round_id)?, &shares)
            .map_err(|e| anyhow!("encrypt_shares: {}", e))?;
        let json_shares: Vec<JsonWireEncryptedShare> = encrypted
            .into_iter()
            .map(WireEncryptedShare::from)
            .map(JsonWireEncryptedShare::from)
            .collect();
        json_to_jstring(env, &json_shares)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

// =============================================================================
// I. Vote commitment (ZKP2)
// =============================================================================

/// Build the vote commitment proof for one proposal choice.
///
/// [witnessJson]   JSON-encoded VanWitness returned by [generateVanWitnessJson]
/// [singleShare]   true for single-share mode (test/dev only)
///
/// Returns JSON-encoded VoteCommitmentBundle (wire-safe — no secret share fields).
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_buildVoteCommitmentJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    round_id: JString<'local>,
    bundle_index: jint,
    hotkey_seed: JByteArray<'local>,
    proposal_id: jint,
    choice: jint,
    num_options: jint,
    witness_json: JString<'local>,
    van_position: jint,
    anchor_height: jint,
    network_id: jint,
    single_share: jboolean,
    progress_callback: JObject<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let seed = java_bytes_at_least(env, &hotkey_seed, "hotkey_seed", PROTOCOL_FIELD_BYTES)?;
        let reporter = progress_reporter_from_callback(env, &progress_callback)?;

        // Extract auth_path from VanWitness JSON (24 × 32-byte sibling hashes).
        let van: JsonVanWitness = json_from_jstring(env, &witness_json, "witnessJson")?;
        let auth_path: Vec<[u8; 32]> = van
            .auth_path
            .iter()
            .enumerate()
            .map(|(i, h)| {
                let bytes = hex_dec(h, &format!("auth_path[{i}]"))?;
                require_32(bytes, &format!("auth_path[{i}]"))
            })
            .collect::<anyhow::Result<_>>()?;

        let bundle = handle
            .db
            .build_vote_commitment(
                &java_string_to_rust(env, &round_id)?,
                jint_to_u32(bundle_index, "bundle_index")?,
                &seed,
                network_id_from_jint(network_id)?,
                jint_to_u32(proposal_id, "proposal_id")?,
                jint_to_u32(choice, "choice")?,
                jint_to_u32(num_options, "num_options")?,
                &auth_path,
                jint_to_u32(van_position, "van_position")?,
                jint_to_u32(anchor_height, "anchor_height")?,
                single_share == JNI_TRUE,
                reporter.as_ref(),
            )
            .map_err(|e| anyhow!("build_vote_commitment: {}", e))?;

        json_to_jstring(env, &JsonVoteCommitmentBundle::from(&bundle))
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

// =============================================================================
// J. Share payloads
// =============================================================================

/// Build share payloads for distribution to tally-server helpers.
///
/// [encSharesJson]    JSON array of WireEncryptedShare (c1/c2/share_index)
///                    extracted from the VoteCommitmentBundle.enc_shares field.
/// [commitmentJson]   Full JSON-encoded VoteCommitmentBundle.
/// [vcTreePosition]   Position of the vote commitment leaf in the VC tree
///                    (known after the cast-vote TX is confirmed on chain).
///
/// Returns JSON array of SharePayload.
#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_buildSharePayloadsJson<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    enc_shares_json: JString<'local>,
    commitment_json: JString<'local>,
    vote_decision: jint,
    num_options: jint,
    vc_tree_position: jlong,
    single_share_mode: jboolean,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        // Deserialize enc_shares (wire-safe public components)
        let json_shares: Vec<JsonWireEncryptedShare> =
            json_from_jstring(env, &enc_shares_json, "encSharesJson")?;
        let enc_shares: Vec<WireEncryptedShare> = json_shares
            .into_iter()
            .map(WireEncryptedShare::try_from)
            .collect::<anyhow::Result<_>>()?;

        // Deserialize VoteCommitmentBundle from JSON (reconstruct wire-safe version)
        let json_bundle: JsonVoteCommitmentBundle =
            json_from_jstring(env, &commitment_json, "commitmentJson")?;
        let commitment = VoteCommitmentBundle {
            van_nullifier: require_len(
                hex_dec(&json_bundle.van_nullifier, "van_nullifier")?,
                "van_nullifier",
                PROTOCOL_FIELD_BYTES,
            )?,
            vote_authority_note_new: require_len(
                hex_dec(
                    &json_bundle.vote_authority_note_new,
                    "vote_authority_note_new",
                )?,
                "vote_authority_note_new",
                PROTOCOL_FIELD_BYTES,
            )?,
            vote_commitment: require_len(
                hex_dec(&json_bundle.vote_commitment, "vote_commitment")?,
                "vote_commitment",
                PROTOCOL_FIELD_BYTES,
            )?,
            proposal_id: json_bundle.proposal_id,
            proof: hex_dec(&json_bundle.proof, "proof")?,
            enc_shares: Vec::new(), // wire-only path — not used by build_share_payloads
            anchor_height: json_bundle.anchor_height,
            vote_round_id: json_bundle.vote_round_id,
            shares_hash: require_len(
                hex_dec(&json_bundle.shares_hash, "shares_hash")?,
                "shares_hash",
                PROTOCOL_FIELD_BYTES,
            )?,
            share_blinds: json_bundle
                .share_blinds
                .iter()
                .enumerate()
                .map(|(i, h)| {
                    let field = format!("share_blinds[{i}]");
                    require_len(hex_dec(h, &field)?, &field, PROTOCOL_FIELD_BYTES)
                })
                .collect::<anyhow::Result<_>>()?,
            share_comms: json_bundle
                .share_comms
                .iter()
                .enumerate()
                .map(|(i, h)| {
                    let field = format!("share_comms[{i}]");
                    require_len(hex_dec(h, &field)?, &field, PROTOCOL_FIELD_BYTES)
                })
                .collect::<anyhow::Result<_>>()?,
            r_vpk_bytes: require_len(
                hex_dec(&json_bundle.r_vpk_bytes, "r_vpk_bytes")?,
                "r_vpk_bytes",
                PROTOCOL_FIELD_BYTES,
            )?,
            alpha_v: require_len(
                hex_dec(&json_bundle.alpha_v, "alpha_v")?,
                "alpha_v",
                PROTOCOL_FIELD_BYTES,
            )?,
        };

        // Note: build_share_payloads is a pure function (no VotingDb needed).
        let payloads: Vec<JsonSharePayload> = voting::vote_commitment::build_share_payloads(
            &enc_shares,
            &commitment,
            jint_to_u32(vote_decision, "vote_decision")?,
            jint_to_u32(num_options, "num_options")?,
            jlong_to_u64(vc_tree_position, "vc_tree_position")?,
            single_share_mode == JNI_TRUE,
        )
        .map_err(|e| anyhow!("build_share_payloads: {}", e))?
        .into_iter()
        .map(JsonSharePayload::from)
        .collect();

        json_to_jstring(env, &payloads)
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}

// =============================================================================
// K. Cast vote signature
// =============================================================================

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_signCastVote<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hotkey_seed: JByteArray<'local>,
    network_id: jint,
    round_id: JString<'local>,
    r_vpk: JByteArray<'local>,
    van_nullifier: JByteArray<'local>,
    van_new: JByteArray<'local>,
    vote_commitment: JByteArray<'local>,
    proposal_id: jint,
    anchor_height: jint,
    alpha_v: JByteArray<'local>,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let sig = voting::vote_commitment::sign_cast_vote(
            &java_bytes_at_least(env, &hotkey_seed, "hotkey_seed", PROTOCOL_FIELD_BYTES)?,
            network_id_from_jint(network_id)?,
            &java_string_to_rust(env, &round_id)?,
            &java_bytes_exact(env, &r_vpk, "r_vpk", PROTOCOL_FIELD_BYTES)?,
            &java_bytes_exact(env, &van_nullifier, "van_nullifier", PROTOCOL_FIELD_BYTES)?,
            &java_bytes_exact(env, &van_new, "van_new", PROTOCOL_FIELD_BYTES)?,
            &java_bytes_exact(
                env,
                &vote_commitment,
                "vote_commitment",
                PROTOCOL_FIELD_BYTES,
            )?,
            jint_to_u32(proposal_id, "proposal_id")?,
            jint_to_u32(anchor_height, "anchor_height")?,
            &java_bytes_exact(env, &alpha_v, "alpha_v", PROTOCOL_FIELD_BYTES)?,
        )
        .map_err(|e| anyhow!("sign_cast_vote: {}", e))?;
        Ok(env.byte_array_from_slice(&sig.vote_auth_sig)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, std::ptr::null_mut())
}
