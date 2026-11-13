package com.smart_bdash.mobile.analytics.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.smart_bdash.mobile.analytics.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * <pre>
 * FCM トークンを暗号化して保持するストレージクラス.
 *
 * ・トークンをAES-256-GCM 鍵でトークンを暗号化し、通常の SharedPreferences に保存する.
 *   鍵は KeyStore 内から取り出せないため、Root 化端末でファイルを読み取られても値は保護される.
 * ・旧バージョンで保存されたトークン(平文 prefs / ESP)は、初回アクセス時に自動で移行する.
 *   移行が完了した旧データは削除する.
 * ・復号できないデータ(バックアップ復元による鍵の不一致等)は破棄し、null を返す.
 *   お客様アプリから storeFCMToken() メソッドを呼び出して再度トークンが渡されれば復旧する.
 * </pre>
 */
final class NotificationTokenStorage {
    private static final String PREFS_NAME = "com.smart_bdash.mobile.analytics.notification.secure_v2";
    private static final String KEY_TOKEN = "regId";
    private static final String KEY_LEGACY_MIGRATED = "legacyMigrated";
    private static final String KEY_LEGACY_MIGRATION_FAILURES = "legacyMigrationFailures";

    private static final String FORMAT_PREFIX = "v1:";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "com.smart_bdash.mobile.analytics.notification.token_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    /** 旧々バージョン: 平文でトークンを保存していた prefs(通知設定など他の値は引き続き利用中) */
    private static final String LEGACY_PLAIN_PREFS_NAME = "com.smart_bdash.mobile.analytics.notification";
    /** 旧バージョン: ESP でトークンを保存していた prefs */
    private static final String LEGACY_SECURE_PREFS_NAME = "com.smart_bdash.mobile.analytics.notification.secure";
    private static final String LEGACY_KEY_REG_ID = "regId";

    /**
     * 旧 ESP の読み出しに失敗した場合に再試行する最大回数(アプリ起動単位).
     * Keystore の一時的な不調でトークンを失わないよう、即座には破棄しない.
     */
    private static final int MAX_LEGACY_MIGRATION_ATTEMPTS = 3;

    private final Context appContext;
    private final SharedPreferences prefs;

    /** Keystore への問い合わせを減らすためのキャッシュ */
    private SecretKey cachedKey;
    /** 移行判定はプロセス内で 1 回だけ行う(失敗時は次回起動時に再試行) */
    private boolean legacyMigrationChecked = false;

    NotificationTokenStorage(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存されているトークンを返す.
     * @return トークン. 未保存、または復号できない場合は null
     */
    @Nullable
    synchronized String getToken() {
        migrateLegacyIfNeeded();
        return readToken();
    }

    /**
     * トークンを暗号化して保存する. null / 空文字の場合は削除する.
     * @param token FCM トークン
     */
    synchronized void saveToken(@Nullable String token) {
        if (TextUtils.isEmpty(token)) {
            clear();
            return;
        }
        if (writeToken(token)) {
            // 新しいトークンが保存できた時点で旧データは不要になるため、移行を完了扱いにする
            finishLegacyMigration();
        } else {
            LogUtil.s("[b-dash] >>failed to store token");
        }
    }

    /**
     * 保存されているトークンを削除する.
     */
    synchronized void clear() {
        prefs.edit().remove(KEY_TOKEN).commit();
    }

    /**
     * 復号したトークンを読み出す.
     * @return 復号したトークン. 未保存・復号不可の場合は null
     */
    @Nullable
    private String readToken() {
        String stored = prefs.getString(KEY_TOKEN, null);
        if (stored == null) {
            return null;
        }
        try {
            return decrypt(stored);
        } catch (BadPaddingException | IllegalArgumentException e) {
            // BadPaddingException は GCM の認証失敗(AEADBadTagException)を含む
            // データの改ざん・破損、またはバックアップ復元などで鍵が一致しない.
            // 再試行しても復号できないため破棄する
            LogUtil.out(e);
            clear();
            return null;
        } catch (GeneralSecurityException | IOException | RuntimeException e) {
            // Keystore の一時的な不調の可能性があるため、データは残して次回に再試行する
            LogUtil.out(e);
            return null;
        }
    }

    /**
     * 新たなトークンを上書き
     * @return 保存に成功した場合 true
     */
    private boolean writeToken(@NonNull String token) {
        try {
            prefs.edit().putString(KEY_TOKEN, encrypt(token)).commit();
            return true;
        } catch (GeneralSecurityException | IOException | RuntimeException e) {
            LogUtil.out(e);
        }

        // 鍵が壊れている可能性があるため、鍵を作り直して 1 回だけ再試行する.
        // (新しいトークンで上書きするため、旧鍵で暗号化されたデータは不要)
        resetKey();
        try {
            prefs.edit().putString(KEY_TOKEN, encrypt(token)).commit();
            return true;
        } catch (GeneralSecurityException | IOException | RuntimeException e) {
            LogUtil.out(e);
            return false;
        }
    }

    /**
     * トークンを暗号化
     * @param plain 平文トークン
     * @return 暗号化されたトークン
     */
    private String encrypt(@NonNull String plain) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        // IV は Keystore が自動生成する(呼び出し側からの IV 指定は Keystore の既定で禁止されている)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] payload = ByteBuffer.allocate(iv.length + cipherText.length)
                .put(iv)
                .put(cipherText)
                .array();
        return FORMAT_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP);
    }

    /**
     * トークンを復号化
     * @param stored 保存済みのトークン
     * @return 復号化されたトークン
     */
    private String decrypt(@NonNull String stored) throws GeneralSecurityException, IOException {
        if (!stored.startsWith(FORMAT_PREFIX)) {
            throw new IllegalArgumentException("Unknown token format");
        }
        byte[] payload = Base64.decode(stored.substring(FORMAT_PREFIX.length()), Base64.NO_WRAP);
        if (payload.length <= GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid token payload");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                new GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_LENGTH));
        byte[] plain = cipher.doFinal(payload, GCM_IV_LENGTH, payload.length - GCM_IV_LENGTH);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        if (cachedKey != null) {
            return cachedKey;
        }
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        Key key = keyStore.getKey(KEY_ALIAS, null);
        if (key instanceof SecretKey) {
            cachedKey = (SecretKey) key;
            return cachedKey;
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        cachedKey = generator.generateKey();
        return cachedKey;
    }

    private void resetKey() {
        cachedKey = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }
        } catch (GeneralSecurityException | IOException | RuntimeException e) {
            LogUtil.out(e);
        }
    }

    // ============================================================
    // 旧データ移行(移行期間終了後に削除可能)
    // ============================================================

    private enum MigrationResult {
        /** 移行完了(移行対象なしを含む). 旧データを削除してよい */
        DONE,
        /** 一時的な失敗. 旧データを残し、次回起動時に再試行する */
        RETRY
    }

    /**
     * 旧バージョンで保存されたトークンを新保存先へ移行する.
     * 優先順位は旧実装の getRegistrationId() と同じく ESP → 平文 prefs.
     */
    private void migrateLegacyIfNeeded() {
        if (legacyMigrationChecked) {
            return;
        }
        legacyMigrationChecked = true;

        if (prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) {
            return;
        }

        if (migrateLegacy() == MigrationResult.DONE) {
            finishLegacyMigration();
        }
    }

    private MigrationResult migrateLegacy() {
        if (prefs.contains(KEY_TOKEN)) {
            // 既に新保存先にトークンがある(新しいトークンが保存済み)ため、旧データは不要
            return MigrationResult.DONE;
        }

        String token = null;

        // 1. 旧 ESP
        if (legacyPrefsFileExists(LEGACY_SECURE_PREFS_NAME)) {
            try {
                token = readLegacySecureToken();
            } catch (Exception e) {
                LogUtil.out(e);
                return onLegacyMigrationFailed();
            }
        }

        // 2. 旧平文 prefs(ESP にトークンが無い場合のみ. 旧実装と同じ優先順位)
        if (TextUtils.isEmpty(token)) {
            token = appContext.getSharedPreferences(LEGACY_PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(LEGACY_KEY_REG_ID, null);
        }

        if (TextUtils.isEmpty(token)) {
            return MigrationResult.DONE; // 移行対象なし
        }

        // 3. 新保存先へ書き込み、正しく読み戻せることを確認してから旧データを削除する
        if (!writeToken(token) || !token.equals(readToken())) {
            LogUtil.s("[b-dash] >>legacy token migration: verification failed");
            clear();
            return onLegacyMigrationFailed();
        }

        LogUtil.s("[b-dash] >>legacy token migrated");
        return MigrationResult.DONE;
    }

    /**
     * 旧 ESP からトークンを読み出す.
     *  生成パラメータ(ファイル名 / MasterKey / 暗号スキーム)は旧実装と完全に一致させている.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    private String readLegacySecureToken() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        SharedPreferences legacy = EncryptedSharedPreferences.create(
                appContext,
                LEGACY_SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        return legacy.getString(LEGACY_KEY_REG_ID, null);
    }

    /**
     * 移行失敗回数を記録する. 上限に達した場合は移行を諦め、旧データを破棄する.
     */
    private MigrationResult onLegacyMigrationFailed() {
        int failures = prefs.getInt(KEY_LEGACY_MIGRATION_FAILURES, 0) + 1;
        if (failures < MAX_LEGACY_MIGRATION_ATTEMPTS) {
            prefs.edit().putInt(KEY_LEGACY_MIGRATION_FAILURES, failures).commit();
            LogUtil.s("[b-dash] >>legacy token migration failed. retry next launch (" + failures + ")");
            return MigrationResult.RETRY;
        }
        LogUtil.s("[b-dash] >>legacy token migration gave up");
        return MigrationResult.DONE;
    }

    /**
     * 旧データを削除し、移行完了フラグを立てる.
     */
    private void finishLegacyMigration() {
        if (prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) {
            return;
        }

        // 平文 prefs は通知設定など他の値を引き続き利用しているため、トークンのキーだけ削除する
        appContext.getSharedPreferences(LEGACY_PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(LEGACY_KEY_REG_ID).commit();

        // 旧 ESP ファイルは丸ごと削除する.
        // ※ MasterKey(_androidx_security_master_key_)はホストアプリが ESP で共用している
        //   可能性があるため、SDK 側では削除しない
        deleteLegacyPrefsFile(LEGACY_SECURE_PREFS_NAME);

        prefs.edit()
                .putBoolean(KEY_LEGACY_MIGRATED, true)
                .remove(KEY_LEGACY_MIGRATION_FAILURES)
                .commit();
        legacyMigrationChecked = true;
    }

    private File legacyPrefsFile(String name) {
        return new File(appContext.getApplicationInfo().dataDir, "shared_prefs/" + name + ".xml");
    }

    private boolean legacyPrefsFileExists(String name) {
        return legacyPrefsFile(name).exists();
    }

    /**
     * prefs ファイルを削除する.
     *  File#delete() だけではプロセス内の SharedPreferences キャッシュが残るため、
     *  先に内容をクリアしてからファイルを削除する.
     */
    private void deleteLegacyPrefsFile(String name) {
        if (!legacyPrefsFileExists(name)) {
            return;
        }
        try {
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit();
            appContext.deleteSharedPreferences(name);
        } catch (Exception e) {
            // 削除に失敗しても致命的ではない(内容はクリア済み)
            LogUtil.out(e);
        }
    }
}
