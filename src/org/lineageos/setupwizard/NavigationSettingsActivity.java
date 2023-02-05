/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static org.lineageos.internal.util.DeviceKeysConstants.KEY_MASK_APP_SWITCH;
import static org.lineageos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.airbnb.lottie.LottieAnimationView;

import lineageos.providers.LineageSettings;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class NavigationSettingsActivity extends BaseSetupWizardActivity {

    private static final String KEY_NAV_BAR_INVERSE = "sysui_nav_bar_inverse";

    private SetupWizardApp mSetupWizardApp;

    private String mSelection = NAV_BAR_MODE_GESTURAL_OVERLAY;

    private CheckBox mHideGesturalHint;
    private CheckBox mInvertLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSetupWizardApp = (SetupWizardApp) getApplication();
        boolean navBarEnabled = false;
        if (mSetupWizardApp.getSettingsBundle().containsKey(DISABLE_NAV_KEYS)) {
            navBarEnabled = mSetupWizardApp.getSettingsBundle().getBoolean(DISABLE_NAV_KEYS);
        }

        int deviceKeys = getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys);
        boolean hasHomeKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        getGlifLayout().setDescriptionText(getString(R.string.navigation_summary));
        setNextText(R.string.next);

        int available = 3;
        // Hide unavailable navigation modes
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            findViewById(R.id.radio_gesture).setVisibility(View.GONE);
            ((RadioButton) findViewById(R.id.radio_sw_keys)).setChecked(true);
            available--;
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            findViewById(R.id.radio_sw_keys).setVisibility(View.GONE);
            available--;
        }

        // Hide this page if the device has hardware keys but didn't enable navbar
        // or if there's <= 1 available navigation modes
        if (!navBarEnabled && hasHomeKey || available <= 1) {
            mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY,
                    NAV_BAR_MODE_3BUTTON_OVERLAY);
            finishAction(RESULT_OK);
        }

        final LottieAnimationView navigationIllustration =
                findViewById(R.id.navigation_illustration);
        final RadioGroup radioGroup = findViewById(R.id.navigation_radio_group);
        mHideGesturalHint = findViewById(R.id.hide_navigation_hint);
        mInvertLayout = findViewById(R.id.invert_layout);

        hideInvertCheckbox();

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_gesture:
                    mSelection = NAV_BAR_MODE_GESTURAL_OVERLAY;
                    navigationIllustration
                            .setAnimation(R.raw.lottie_system_nav_fully_gestural);
                    revealHintCheckbox();
                    hideInvertCheckbox();
                    break;
                case R.id.radio_sw_keys:
                    mSelection = NAV_BAR_MODE_3BUTTON_OVERLAY;
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_3_button);
                    hideHintCheckBox();
                    revealInvertCheckbox();
                    break;
            }

            navigationIllustration.playAnimation();
        });
    }

    private void revealHintCheckbox() {
        mHideGesturalHint.animate().cancel();

        if (mHideGesturalHint.getVisibility() == View.VISIBLE) {
            return;
        }

        mHideGesturalHint.setVisibility(View.VISIBLE);
        mHideGesturalHint.setAlpha(0.0f);
        mHideGesturalHint.animate()
                .translationY(0)
                .alpha(1.0f)
                .setListener(null);
    }

    private void hideHintCheckBox() {
        if (mHideGesturalHint.getVisibility() == View.INVISIBLE) {
            return;
        }

        mHideGesturalHint.animate()
                .translationY(-mHideGesturalHint.getHeight())
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mHideGesturalHint.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void revealInvertCheckbox() {
        mInvertLayout.animate().cancel();

        if (mInvertLayout.getVisibility() == View.VISIBLE) {
            return;
        }

        mInvertLayout.setVisibility(View.VISIBLE);
        mInvertLayout.setAlpha(0.0f);
        mInvertLayout.animate()
            .translationY(0)
            .alpha(1.0f)
            .setListener(null);
    }

    private void hideInvertCheckbox() {
        if (mInvertLayout.getVisibility() == View.INVISIBLE) {
            return;
        }

        mInvertLayout.animate()
            .translationY(-mInvertLayout.getHeight())
            .alpha(0.0f)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mInvertLayout.setVisibility(View.INVISIBLE);
                }
            });
    }

    @Override
    protected void onNextPressed() {
        mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY, mSelection);
        boolean hideHint = mHideGesturalHint.isChecked();
        LineageSettings.System.putIntForUser(getContentResolver(),
                LineageSettings.System.NAVIGATION_BAR_HINT, hideHint ? 0 : 1,
                UserHandle.USER_CURRENT);
        boolean invertLayout = mInvertLayout.isChecked();
        Settings.Secure.putIntForUser(getContentResolver(),
                KEY_NAV_BAR_INVERSE, invertLayout ? 1 : 0,
                UserHandle.USER_CURRENT);
        super.onNextPressed();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_navigation;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_navigation;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_navigation;
    }
}
