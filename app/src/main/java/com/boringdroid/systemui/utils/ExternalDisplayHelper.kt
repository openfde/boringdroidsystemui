package com.boringdroid.systemui.utils

import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.ServiceManager
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import vendor.openfde.display.IOpenfdeDisplay
import vendor.openfde.display.IOpenfdeDisplayCallback

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
 * V3 services also expose registerCallback(IOpenfdeDisplayCallback). The callback
 * pushes opened/closed/supported/unsupported events so the top-bar indicator can
 * update without polling. V2 services simply fail to register and fall back to
 * on-demand queries.
 */
object ExternalDisplayHelper {
    private const val TAG = "ExternalDisplayHelper"
    const val SERVICE_NAME = "vendor.openfde.display.IOpenfdeDisplay/default"

    // Keep in sync with Hwc2Device::ExternalDisplayEvent / OpenfdeDisplayTest.
    const val EVENT_OPENED = 1
    const val EVENT_CLOSED = 2
    const val EVENT_SUPPORTED = 3
    const val EVENT_UNSUPPORTED = 4

    interface OnExternalDisplayEventListener {
        fun onExternalDisplayEvent(event: Int)
    }

    private val listeners = CopyOnWriteArrayList<OnExternalDisplayEventListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var displayService: IOpenfdeDisplay? = null
    private var callbackRegistered = false
    private var callbackRegistrationAttempted = false

    private val callback = object : IOpenfdeDisplayCallback.Stub() {
        init {
            // The generated stable V3 Stub does this in its constructor. The app-side
            // AIDL is compiled without @VintfStability, so mark the binder explicitly.
            markVintfStability()
        }

        override fun onExternalDisplayEvent(event: Int) {
            Log.d(TAG, "onExternalDisplayEvent ${eventName(event)} ($event)")
            mainHandler.post {
                listeners.forEach { listener ->
                    try {
                        listener.onExternalDisplayEvent(event)
                    } catch (e: Exception) {
                        Log.e(TAG, "listener failed for event $event", e)
                    }
                }
            }
        }
    }

    fun addListener(listener: OnExternalDisplayEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        ensureCallbackRegistered()
    }

    fun removeListener(listener: OnExternalDisplayEventListener) {
        listeners.remove(listener)
    }

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

    private fun ensureCallbackRegistered() {
        if (callbackRegistered || callbackRegistrationAttempted) {
            return
        }
        val service = getService() ?: return
        callbackRegistrationAttempted = true
        try {
            service.registerCallback(callback)
            callbackRegistered = true
            Log.d(TAG, "registerCallback success")
        } catch (e: DeadObjectException) {
            Log.w(TAG, "registerCallback failed: binder died", e)
            invalidateService()
        } catch (e: Exception) {
            // V2 service: no registerCallback transaction. Keep the query fallback.
            Log.w(TAG, "registerCallback unsupported, using query fallback", e)
        }
    }

    fun queryStatus(): ExternalDisplayStatus {
        val service = getService() ?: return ExternalDisplayStatus(false, false, false)
        ensureCallbackRegistered()
        return try {
            val supported = try {
                service.isExternalDisplaySupported()
            } catch (e: Exception) {
                // A V1/V2 AIDL service may not have the supported method. Keep working
                // instead of treating the whole service as unavailable.
                Log.w(TAG, "isExternalDisplaySupported unavailable, assuming supported", e)
                true
            }
            ExternalDisplayStatus(true, supported, service.getExternalDisplayState())
        } catch (e: Exception) {
            Log.e(TAG, "queryStatus failed", e)
            invalidateService()
            ExternalDisplayStatus(false, false, false)
        }
    }

    /** Returns null when the service or the Binder transaction is unavailable. */
    fun openExternalDisplay(): Boolean? {
        val service = getService() ?: return null
        return try {
            service.openExternalDisplay().also {
                Log.d(TAG, "openExternalDisplay -> $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openExternalDisplay failed", e)
            invalidateService()
            null
        }
    }

    /** Returns null when the service or the Binder transaction is unavailable. */
    fun closeExternalDisplay(): Boolean? {
        val service = getService() ?: return null
        return try {
            service.closeExternalDisplay().also {
                Log.d(TAG, "closeExternalDisplay -> $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeExternalDisplay failed", e)
            invalidateService()
            null
        }
    }

    private fun invalidateService() {
        displayService = null
        callbackRegistered = false
        callbackRegistrationAttempted = false
    }

    private fun eventName(event: Int): String = when (event) {
        EVENT_OPENED -> "OPENED"
        EVENT_CLOSED -> "CLOSED"
        EVENT_SUPPORTED -> "SUPPORTED"
        EVENT_UNSUPPORTED -> "UNSUPPORTED"
        else -> "UNKNOWN"
    }
}
