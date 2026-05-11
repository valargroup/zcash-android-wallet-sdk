use super::helpers::*;
use super::*;

const NOTE_SCOPE_EXTERNAL: u32 = 0;
const NOTE_SCOPE_INTERNAL: u32 = 1;
const ORCHARD_DIVERSIFIER_BYTES: usize = 11;

pub(super) fn hex_enc(bytes: &[u8]) -> String {
    hex::encode(bytes)
}

pub(super) fn hex_dec(value: &str, field: &str) -> anyhow::Result<Vec<u8>> {
    hex::decode(value).map_err(|e| anyhow!("field '{field}': invalid hex: {e}"))
}

#[derive(Deserialize, Serialize)]
pub(super) struct JsonNoteInfo {
    pub(super) commitment: String,
    pub(super) nullifier: String,
    pub(super) value: u64,
    pub(super) position: u64,
    pub(super) diversifier: String,
    pub(super) rho: String,
    pub(super) rseed: String,
    pub(super) scope: u32,
    pub(super) ufvk_str: String,
}

impl TryFrom<JsonNoteInfo> for NoteInfo {
    type Error = anyhow::Error;

    fn try_from(note: JsonNoteInfo) -> anyhow::Result<Self> {
        let scope = require_note_scope(note.scope)?;

        Ok(NoteInfo {
            commitment: require_len(
                hex_dec(&note.commitment, "commitment")?,
                "commitment",
                PROTOCOL_FIELD_BYTES,
            )?,
            nullifier: require_len(
                hex_dec(&note.nullifier, "nullifier")?,
                "nullifier",
                PROTOCOL_FIELD_BYTES,
            )?,
            value: note.value,
            position: note.position,
            diversifier: require_len(
                hex_dec(&note.diversifier, "diversifier")?,
                "diversifier",
                ORCHARD_DIVERSIFIER_BYTES,
            )?,
            rho: require_len(hex_dec(&note.rho, "rho")?, "rho", PROTOCOL_FIELD_BYTES)?,
            rseed: require_len(
                hex_dec(&note.rseed, "rseed")?,
                "rseed",
                PROTOCOL_FIELD_BYTES,
            )?,
            scope,
            ufvk_str: note.ufvk_str,
        })
    }
}

fn require_note_scope(scope: u32) -> anyhow::Result<u32> {
    match scope {
        NOTE_SCOPE_EXTERNAL | NOTE_SCOPE_INTERNAL => Ok(scope),
        _ => Err(anyhow!(
            "scope must be {NOTE_SCOPE_EXTERNAL} (external) or {NOTE_SCOPE_INTERNAL} (internal), got {scope}"
        )),
    }
}

#[derive(Serialize, Deserialize)]
pub(super) struct JsonWitnessData {
    pub(super) note_commitment: String,
    pub(super) position: u64,
    pub(super) root: String,
    pub(super) auth_path: Vec<String>,
}

impl TryFrom<JsonWitnessData> for WitnessData {
    type Error = anyhow::Error;

    fn try_from(witness: JsonWitnessData) -> anyhow::Result<Self> {
        Ok(WitnessData {
            note_commitment: require_len(
                hex_dec(&witness.note_commitment, "note_commitment")?,
                "note_commitment",
                PROTOCOL_FIELD_BYTES,
            )?,
            position: witness.position,
            root: require_len(
                hex_dec(&witness.root, "root")?,
                "root",
                PROTOCOL_FIELD_BYTES,
            )?,
            auth_path: witness
                .auth_path
                .iter()
                .enumerate()
                .map(|(index, hash)| {
                    let field = format!("auth_path[{index}]");
                    require_len(hex_dec(hash, &field)?, &field, PROTOCOL_FIELD_BYTES)
                })
                .collect::<anyhow::Result<_>>()?,
        })
    }
}

impl From<WitnessData> for JsonWitnessData {
    fn from(witness: WitnessData) -> Self {
        JsonWitnessData {
            note_commitment: hex_enc(&witness.note_commitment),
            position: witness.position,
            root: hex_enc(&witness.root),
            auth_path: witness.auth_path.iter().map(|hash| hex_enc(hash)).collect(),
        }
    }
}

#[derive(Serialize, Deserialize)]
pub(super) struct JsonVanWitness {
    pub(super) auth_path: Vec<String>,
    pub(super) position: u32,
    pub(super) anchor_height: u32,
}

impl From<voting::tree_sync::VanWitness> for JsonVanWitness {
    fn from(witness: voting::tree_sync::VanWitness) -> Self {
        JsonVanWitness {
            auth_path: witness.auth_path.iter().map(|hash| hex_enc(hash)).collect(),
            position: witness.position,
            anchor_height: witness.anchor_height,
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonGovernancePczt {
    pub(super) pczt_bytes: String,
    pub(super) rk: String,
    pub(super) action_index: u32,
    pub(super) pczt_sighash: String,
}

impl TryFrom<GovernancePczt> for JsonGovernancePczt {
    type Error = anyhow::Error;

    fn try_from(pczt: GovernancePczt) -> anyhow::Result<Self> {
        Ok(JsonGovernancePczt {
            pczt_bytes: hex_enc(&pczt.pczt_bytes),
            rk: hex_enc(&pczt.rk),
            action_index: u32::try_from(pczt.action_index)
                .map_err(|_| anyhow!("action_index is too large for u32: {}", pczt.action_index))?,
            pczt_sighash: hex_enc(&pczt.pczt_sighash),
        })
    }
}

#[derive(Serialize)]
pub(super) struct JsonDelegationProofResult {
    pub(super) proof: String,
    pub(super) public_inputs: Vec<String>,
    pub(super) nf_signed: String,
    pub(super) cmx_new: String,
    pub(super) gov_nullifiers: Vec<String>,
    pub(super) van_comm: String,
    pub(super) rk: String,
}

impl From<DelegationProofResult> for JsonDelegationProofResult {
    fn from(result: DelegationProofResult) -> Self {
        JsonDelegationProofResult {
            proof: hex_enc(&result.proof),
            public_inputs: result
                .public_inputs
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            nf_signed: hex_enc(&result.nf_signed),
            cmx_new: hex_enc(&result.cmx_new),
            gov_nullifiers: result
                .gov_nullifiers
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            van_comm: hex_enc(&result.van_comm),
            rk: hex_enc(&result.rk),
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonDelegationPirPrecomputeResult {
    pub(super) cached_count: u32,
    pub(super) fetched_count: u32,
}

impl From<DelegationPirPrecomputeResult> for JsonDelegationPirPrecomputeResult {
    fn from(result: DelegationPirPrecomputeResult) -> Self {
        JsonDelegationPirPrecomputeResult {
            cached_count: result.cached_count,
            fetched_count: result.fetched_count,
        }
    }
}

#[derive(Serialize, Deserialize)]
pub(super) struct JsonWireEncryptedShare {
    pub(super) c1: String,
    pub(super) c2: String,
    pub(super) share_index: u32,
}

impl TryFrom<JsonWireEncryptedShare> for WireEncryptedShare {
    type Error = anyhow::Error;

    fn try_from(share: JsonWireEncryptedShare) -> anyhow::Result<Self> {
        Ok(WireEncryptedShare {
            c1: hex_dec(&share.c1, "c1")?,
            c2: hex_dec(&share.c2, "c2")?,
            share_index: share.share_index,
        })
    }
}

impl From<WireEncryptedShare> for JsonWireEncryptedShare {
    fn from(share: WireEncryptedShare) -> Self {
        JsonWireEncryptedShare {
            c1: hex_enc(&share.c1),
            c2: hex_enc(&share.c2),
            share_index: share.share_index,
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonDelegationInputs {
    pub(super) fvk_bytes: String,
    pub(super) g_d_new_x: String,
    pub(super) pk_d_new_x: String,
    pub(super) hotkey_raw_address: String,
    pub(super) hotkey_public_key: String,
    pub(super) hotkey_address: String,
    pub(super) seed_fingerprint: String,
}

#[derive(Serialize, Deserialize)]
pub(super) struct JsonVoteCommitmentBundle {
    pub(super) van_nullifier: String,
    pub(super) vote_authority_note_new: String,
    pub(super) vote_commitment: String,
    pub(super) proposal_id: u32,
    pub(super) proof: String,
    pub(super) enc_shares: Vec<JsonWireEncryptedShare>,
    pub(super) anchor_height: u32,
    pub(super) vote_round_id: String,
    pub(super) shares_hash: String,
    pub(super) share_blinds: Vec<String>,
    pub(super) share_comms: Vec<String>,
    pub(super) r_vpk_bytes: String,
    pub(super) alpha_v: String,
}

impl From<&VoteCommitmentBundle> for JsonVoteCommitmentBundle {
    fn from(bundle: &VoteCommitmentBundle) -> Self {
        JsonVoteCommitmentBundle {
            van_nullifier: hex_enc(&bundle.van_nullifier),
            vote_authority_note_new: hex_enc(&bundle.vote_authority_note_new),
            vote_commitment: hex_enc(&bundle.vote_commitment),
            proposal_id: bundle.proposal_id,
            proof: hex_enc(&bundle.proof),
            enc_shares: bundle
                .enc_shares
                .iter()
                .map(|share| JsonWireEncryptedShare {
                    c1: hex_enc(&share.c1),
                    c2: hex_enc(&share.c2),
                    share_index: share.share_index,
                })
                .collect(),
            anchor_height: bundle.anchor_height,
            vote_round_id: bundle.vote_round_id.clone(),
            shares_hash: hex_enc(&bundle.shares_hash),
            share_blinds: bundle
                .share_blinds
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            share_comms: bundle
                .share_comms
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            r_vpk_bytes: hex_enc(&bundle.r_vpk_bytes),
            alpha_v: hex_enc(&bundle.alpha_v),
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonSharePayload {
    pub(super) shares_hash: String,
    pub(super) proposal_id: u32,
    pub(super) vote_decision: u32,
    pub(super) enc_share: JsonWireEncryptedShare,
    pub(super) tree_position: u64,
    pub(super) all_enc_shares: Vec<JsonWireEncryptedShare>,
    pub(super) share_comms: Vec<String>,
    pub(super) primary_blind: String,
}

impl From<SharePayload> for JsonSharePayload {
    fn from(payload: SharePayload) -> Self {
        JsonSharePayload {
            shares_hash: hex_enc(&payload.shares_hash),
            proposal_id: payload.proposal_id,
            vote_decision: payload.vote_decision,
            enc_share: JsonWireEncryptedShare {
                c1: hex_enc(&payload.enc_share.c1),
                c2: hex_enc(&payload.enc_share.c2),
                share_index: payload.enc_share.share_index,
            },
            tree_position: payload.tree_position,
            all_enc_shares: payload
                .all_enc_shares
                .iter()
                .map(|share| JsonWireEncryptedShare {
                    c1: hex_enc(&share.c1),
                    c2: hex_enc(&share.c2),
                    share_index: share.share_index,
                })
                .collect(),
            share_comms: payload
                .share_comms
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            primary_blind: hex_enc(&payload.primary_blind),
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonDelegationSubmission {
    pub(super) proof: String,
    pub(super) rk: String,
    pub(super) spend_auth_sig: String,
    pub(super) sighash: String,
    pub(super) nf_signed: String,
    pub(super) cmx_new: String,
    pub(super) gov_comm: String,
    pub(super) gov_nullifiers: Vec<String>,
    pub(super) alpha: String,
    pub(super) vote_round_id: String,
}

impl From<DelegationSubmissionData> for JsonDelegationSubmission {
    fn from(data: DelegationSubmissionData) -> Self {
        JsonDelegationSubmission {
            proof: hex_enc(&data.proof),
            rk: hex_enc(&data.rk),
            spend_auth_sig: hex_enc(&data.spend_auth_sig),
            sighash: hex_enc(&data.sighash),
            nf_signed: hex_enc(&data.nf_signed),
            cmx_new: hex_enc(&data.cmx_new),
            gov_comm: hex_enc(&data.gov_comm),
            gov_nullifiers: data
                .gov_nullifiers
                .iter()
                .map(|value| hex_enc(value))
                .collect(),
            alpha: hex_enc(&data.alpha),
            vote_round_id: data.vote_round_id,
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonCommitmentBundleRecord {
    pub(super) bundle_json: String,
    pub(super) vc_tree_position: u64,
}

#[derive(Serialize)]
pub(super) struct JsonShareDelegationRecord {
    pub(super) round_id: String,
    pub(super) bundle_index: u32,
    pub(super) proposal_id: u32,
    pub(super) share_index: u32,
    pub(super) sent_to_urls: Vec<String>,
    pub(super) nullifier: String,
    pub(super) confirmed: bool,
    pub(super) submit_at: u64,
    pub(super) created_at: u64,
}

impl From<voting::ShareDelegationRecord> for JsonShareDelegationRecord {
    fn from(record: voting::ShareDelegationRecord) -> Self {
        JsonShareDelegationRecord {
            round_id: record.round_id,
            bundle_index: record.bundle_index,
            proposal_id: record.proposal_id,
            share_index: record.share_index,
            sent_to_urls: record.sent_to_urls,
            nullifier: hex_enc(&record.nullifier),
            confirmed: record.confirmed,
            submit_at: record.submit_at,
            created_at: record.created_at,
        }
    }
}

#[derive(Serialize)]
pub(super) struct JsonKeystoneSignature {
    pub(super) bundle_index: u32,
    pub(super) sig: String,
    pub(super) sighash: String,
    pub(super) rk: String,
}

pub(super) fn json_to_jstring<T: Serialize>(
    env: &mut JNIEnv<'_>,
    value: &T,
) -> anyhow::Result<jstring> {
    let s = serde_json::to_string(value).map_err(|e| anyhow!("JSON serialization error: {}", e))?;
    Ok(env.new_string(s)?.into_raw())
}

pub(super) fn json_from_jstring<T: for<'de> Deserialize<'de>>(
    env: &mut JNIEnv<'_>,
    value: &JString<'_>,
    field: &str,
) -> anyhow::Result<T> {
    let s = java_string_to_rust(env, value)?;
    serde_json::from_str(&s).map_err(|e| {
        anyhow!(
            "{field}: JSON parse error at line {}, column {}",
            e.line(),
            e.column()
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn json_note_info_rejects_unknown_scope() {
        let note = JsonNoteInfo {
            commitment: hex::encode([1u8; PROTOCOL_FIELD_BYTES]),
            nullifier: hex::encode([2u8; PROTOCOL_FIELD_BYTES]),
            value: 13_000_000,
            position: 0,
            diversifier: hex::encode([0u8; ORCHARD_DIVERSIFIER_BYTES]),
            rho: hex::encode([0u8; PROTOCOL_FIELD_BYTES]),
            rseed: hex::encode([0u8; PROTOCOL_FIELD_BYTES]),
            scope: 2,
            ufvk_str: String::new(),
        };

        assert!(NoteInfo::try_from(note).is_err());
    }
}
