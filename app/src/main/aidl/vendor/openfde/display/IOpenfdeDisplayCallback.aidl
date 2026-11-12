package vendor.openfde.display;

/**
 * Callback for vendor.openfde.display.IOpenfdeDisplay V3.
 *
 * Event values:
 *   1 - external display opened
 *   2 - external display closed
 *   3 - backend supports opening an external display
 *   4 - backend no longer supports opening an external display
 *
 * @VintfStability is omitted for the app-side build only; the transaction
 * layout (onExternalDisplayEvent at FIRST_CALL_TRANSACTION) matches V3.
 */
interface IOpenfdeDisplayCallback {
    oneway void onExternalDisplayEvent(int event);
}
