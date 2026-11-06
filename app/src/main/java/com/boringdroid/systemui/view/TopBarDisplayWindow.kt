package com.boringdroid.systemui.view

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.ExternalDisplayHelper
import com.boringdroid.systemui.utils.ExternalDisplayStatus

/**
 * Compact external-display panel opened from the control center or the top-bar status icon.
 *
 * The service is provided by vendor.openfde.display. On builds without it the panel still
 * renders and explains that the service is not ready instead of failing.
 */
class TopBarDisplayWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    companion object {
        const val DISPLAY_WINDOW_RADIUS = 16f
        const val DISPLAY_WINDOW_SHADOW = 80
        const val TAG = "TopBarDisplayWindow"

        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val SETTINGS_SUB_SETTINGS = "com.android.settings.SubSettings"
        private const val EXTERNAL_DISPLAY_FRAGMENT =
            "com.android.settings.connecteddevice.display.TabbedDisplayPreferenceFragment"
        private const val EXTRA_SHOW_FRAGMENT = ":settings:show_fragment"
        private const val EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title"
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        private const val EXTERNAL_DISPLAY_PREFERENCE_KEY = "external_display_settings"
    }

    private var displayToggleLayout: LinearLayout? = null
    private var displayToggle: ImageView? = null
    private var displayName: TextView? = null
    private var displayState: TextView? = null
    private var displayReason: TextView? = null
    private var displaySettings: TextView? = null

    private var displayStatus = ExternalDisplayStatus(false, false, false)

    /** Invoked whenever the queried display state changes, so the top bar icon can refresh. */
    var onDisplayStatusChanged: (() -> Unit)? = null

    override fun showPopupWindow() {
        super.showPopupWindow()
        runWindowAnim(WindowGravity.top, true)
        initViews()
        refreshDisplayState()
    }

    private fun initViews() {
        displayToggleLayout = mContentView?.findViewById(R.id.display_toggle_ll)
        displayToggle = mContentView?.findViewById(R.id.display_toggle_iv)
        displayName = mContentView?.findViewById(R.id.display_name_tv)
        displayState = mContentView?.findViewById(R.id.display_state_tv)
        displayReason = mContentView?.findViewById(R.id.display_reason_tv)
        displaySettings = mContentView?.findViewById(R.id.display_settings_tv)

        displayToggleLayout?.setOnClickListener(this)
        // The circle is a child of the clickable layout. Give it its own listener so a
        // tap directly on the monitor icon always toggles the display as well.
        displayToggle?.setOnClickListener(this)
        displaySettings?.setOnClickListener(this)
    }

    private fun refreshDisplayState() {
        displayStatus = ExternalDisplayHelper.queryStatus()
        Log.d(
            TAG,
            "refreshDisplayState serviceReady=${displayStatus.serviceReady} " +
                "supported=${displayStatus.supported} enabled=${displayStatus.enabled}"
        )
        updateDisplayUi()
        onDisplayStatusChanged?.invoke()
    }

    private fun toggleExternalDisplay() {
        if (!displayStatus.serviceReady) {
            refreshDisplayState()
            return
        }

        val open = !displayStatus.enabled
        if (open && !displayStatus.supported) {
            updateDisplayUi()
            return
        }

        val success = if (open) {
            ExternalDisplayHelper.openExternalDisplay()
        } else {
            ExternalDisplayHelper.closeExternalDisplay()
        }
        refreshDisplayState()

        if (success == null) {
            Toast.makeText(
                getContext(),
                R.string.external_display_service_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        } else if (!success) {
            Toast.makeText(
                getContext(),
                if (open) R.string.external_display_open_failed
                else R.string.external_display_close_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateDisplayUi() {
        val context = getContext()
        // Keep the switch usable while the display is open even if the backend no longer
        // reports it as supported, otherwise the user could not turn it off again.
        val toggleEnabled = displayStatus.serviceReady &&
            (displayStatus.supported || displayStatus.enabled)
        val enabled = displayStatus.enabled
        val tooltip = when {
            !displayStatus.serviceReady ->
                context.getString(R.string.external_display_service_unavailable)
            !displayStatus.supported && !displayStatus.enabled ->
                context.getString(R.string.external_display_not_connected)
            enabled -> context.getString(R.string.external_display_toggle_tooltip_enabled)
            else -> context.getString(R.string.external_display_toggle_tooltip_connected)
        }

        displayToggle?.apply {
            isEnabled = toggleEnabled
            isSelected = toggleEnabled && enabled
            imageTintList = ColorStateList.valueOf(
                if (toggleEnabled && enabled) Color.WHITE else Color.parseColor("#FF1D1D1F")
            )
            imageAlpha = if (toggleEnabled) 255 else 90
            tooltipText = tooltip
            contentDescription = tooltip
        }
        displayToggleLayout?.apply {
            isEnabled = toggleEnabled
            tooltipText = tooltip
            contentDescription = tooltip
        }
        displayName?.apply {
            setTextColor(
                if (toggleEnabled) Color.parseColor("#FF1D1D1F")
                else Color.parseColor("#661D1D1F")
            )
            tooltipText = tooltip
        }
        displayState?.apply {
            visibility = if (toggleEnabled) View.VISIBLE else View.GONE
            text = context.getString(
                if (enabled) R.string.external_display_state_on
                else R.string.external_display_state_off
            )
            tooltipText = tooltip
        }
        displayReason?.apply {
            visibility = if (toggleEnabled) View.GONE else View.VISIBLE
            text = context.getString(
                if (!displayStatus.serviceReady) R.string.external_display_service_unavailable
                else R.string.external_display_not_connected
            )
            tooltipText = tooltip
        }

        // "Display settings" is only actionable after the external display is enabled.
        val settingsEnabled = displayStatus.enabled
        val settingsTooltip = context.getString(
            if (settingsEnabled) R.string.external_display_settings_tooltip
            else R.string.external_display_settings_tooltip_disabled
        )
        displaySettings?.apply {
            isClickable = settingsEnabled
            alpha = if (settingsEnabled) 1f else 0.4f
            tooltipText = settingsTooltip
            contentDescription = settingsTooltip
        }
    }

    override fun onClick(v: View?) {
        if (v == displayToggleLayout || v == displayToggle) {
            toggleExternalDisplay()
        } else if (v == displaySettings) {
            if (!displayStatus.enabled) {
                return
            }
            dismiss()
            openExternalDisplaySettings()
        }
    }

    /**
     * AOSP Settings exposes the external-display page as a fragment hosted by SubSettings.
     * Try that page first and fall back to the "Connected devices" page if SubSettings cannot
     * be launched on a particular build.
     */
    private fun openExternalDisplaySettings() {
        val opened = try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(SETTINGS_PACKAGE, SETTINGS_SUB_SETTINGS)
                putExtra(EXTRA_SHOW_FRAGMENT, EXTERNAL_DISPLAY_FRAGMENT)
                putExtra(
                    EXTRA_SHOW_FRAGMENT_TITLE,
                    getContext().getString(R.string.external_display_title)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getContext().startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open external display settings page", e)
            false
        }

        if (!opened) {
            openConnectedDevicesSettings()
        }
    }

    /**
     * Fallback: AOSP Settings maps ACTION_BLUETOOTH_SETTINGS to
     * Settings$ConnectedDeviceDashboardActivity, i.e. the "Connected devices" page.
     */
    private fun openConnectedDevicesSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Highlight/scroll to the External display entry when the page opens.
            intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, EXTERNAL_DISPLAY_PREFERENCE_KEY)
            getContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Connected devices settings activity not found", e)
            Toast.makeText(
                getContext(),
                R.string.external_display_settings_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open connected devices settings", e)
            Toast.makeText(
                getContext(),
                R.string.external_display_settings_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
