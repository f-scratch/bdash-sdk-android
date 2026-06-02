package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.f_scratch.bdash.mobile.analytics.variant.LogicReleaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * b->dash SDK の Release Build 向けテスト
 */
@RunWith(AndroidJUnit4.class)
public class CoreReleaseTest {

    public Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * <pre>本番向けリリース確認用の統合テスト
     * 期待値
     *   ・オールグリーンになれば納品用として下記のチェックはされている
     *      - バージョン番号
     *      - ビジターID 仕様
     *      - 本番環境向けコンフィグ
     *        - 通信先が本番であること
     *        - デバッグモードでないこと
     *        - デモモードでないこと
     * </pre>
     */
    @Test
    public void testSdkForRelease() throws Exception {
        LogicReleaseTest.init(getContext());

        LogicReleaseTest.testCase_versionCheck();
        LogicReleaseTest.testCase_visitorId();
        LogicReleaseTest.testCase_buildVariants();
    }

    /**
     * <pre>開発環境向け統合テスト
     * 期待値
     *   ・開発環境(STG環境)の設定が適用されている場合、オールグリーンとなる
     *      - ビジターID 仕様
     *      - 本番環境向けコンフィグ
     *        - 通信先がSTG環境であること
     *        - デバッグモードのフラグがONであること
     *        - デモモードのフラグがONであること
     * </pre>
     */
    @Test
    public void testSdkForDevelop() throws Exception {
        LogicReleaseTest.init(getContext());

        LogicReleaseTest.testCase_visitorId();
        LogicReleaseTest.testCase_buildVariantsByDevelop();
    }
}