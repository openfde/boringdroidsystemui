package com.boringdroid.systemui.utils

import android.os.ServiceManager
import android.util.Log
import vendor.openfde.display.IOpenfdeDisplay

/**
 * Current external-display state used by the top bar and the display popup.
 *
 * [supported] means the backend can host an external display (for X11 this requires
 * two monitors in extended, non-mirror mode). [enabled] is the software switch state.
 */
data class ExternalDisplayStatus(
    val serviceReady: Boolean,
    val supported: Boolean,
    val enabled: Boolean,
) {
    val available: Boolean
        get() = serviceReady && (supported || enabled)
}

/**
 * Small facade around vendor.openfde.display.IOpenfdeDisplay/default.
 *
 * The service is queried on demand until the broadcast-based state notification is
 * wired up. Every caller handles an absent service or a dead binder gracefully.
 */
object ExternalDisplayHelper {
    private const val TAG = "ExternalDisplayHelper"
    const val SERVICE_NAME = "vendor.openfde.display.IOpenfdeDisplay/default"

    private var displayService: IOpenfdeDisplay? = null

    private fun getService(): IOpenfdeDisplay? {
        displayService?.let { return it }
        return try {
            // getService() does not block; waitForService() would hang the UI thread
            // forever on builds where the service has not been integrated.
            val binder = ServiceManager.getService(SERVICE_NAME)
            if (binder == null) {
                Log.e(TAG, "OpenfdeDisplay AIDL service not found")
                null
            } else {
                IOpenfdeDisplay.Stub.asInterface(binder).also { displayService = it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get OpenfdeDisplay AIDL service", e)
            null
        }
    }

    fun queryStatus(): ExternalDisplayStatus {
        val service = getService() ?: return ExternalDisplayStatus(false, false, false)
        return try {
            val supported = try {
                service.isExternalDisplaySupported()
            } catch (e: Exception) {
                // A V1 AIDL service only has the first three methods. Keep working
                // instead of treating the whole service as unavailable.
                Log.w(TAG, "isExternalDisplaySupported unavailable, assuming supported", e)
                true
            }
            ExternalDisplayStatus(true, supported, service.getExternalDisplayState())
        } catch (e: Exception) {
            Log.e(TAG, "queryStatus failed", e)
            displayService = null
            ExternalDisplayStatus(false, false, false)
        }
    }

    /** Returns null when the service or the Binder transaction is unavailable. */
    fun openExternalDisplay(): Boolean? {
        val service = getService() ?: return null
        return try {
            service.openExternalDisplay().also {
                Log.i(TAG, "openExternalDisplay -> $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openExternalDisplay failed", e)
            displayService = null
            null
        }
    }

    /** Returns null when the service or the Binder transaction is unavailable. */
    fun closeExternalDisplay(): Boolean? {
        val service = getService() ?: return null
        return try {
            service.closeExternalDisplay().also {
                Log.i(TAG, "closeExternalDisplay -> $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeExternalDisplay failed", e)
            displayService = null
            null
        }
    }
}
