use super::*;

pub(super) struct VotingDatabaseHandle {
    pub(super) db: Arc<VotingDb>,
}

pub(super) fn handle_from_jlong(handle: jlong) -> anyhow::Result<&'static VotingDatabaseHandle> {
    if handle == 0 {
        return Err(anyhow!("VotingDatabaseHandle is null"));
    }

    // SAFETY: The pointer is allocated by openVotingDb with Box::into_raw and
    // remains valid until closeVotingDb receives the same handle.
    Ok(unsafe { &*(handle as *const VotingDatabaseHandle) })
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_openVotingDb<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_path: JString<'local>,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let path = java_string_to_rust(env, &db_path)?;
        let db = VotingDb::open(&path).map_err(|e| anyhow!("VotingDb::open failed: {}", e))?;
        Ok(Box::into_raw(Box::new(VotingDatabaseHandle { db: Arc::new(db) })) as jlong)
    });
    unwrap_exc_or(&mut env, res, 0)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_closeVotingDb<
    'local,
>(
    mut _env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
) {
    if db_handle != 0 {
        // SAFETY: The handle must be a pointer returned by openVotingDb.
        unsafe { drop(Box::from_raw(db_handle as *mut VotingDatabaseHandle)) };
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_VotingRustBackend_setWalletId<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_handle: jlong,
    wallet_id: JString<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let handle = handle_from_jlong(db_handle)?;
        let id = java_string_to_rust(env, &wallet_id)?;
        handle.db.set_wallet_id(&id);
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}
