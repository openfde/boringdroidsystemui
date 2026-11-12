/*
 * Copyright (C) 2026 The Openfde Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vendor.openfde.display;

import vendor.openfde.display.IOpenfdeDisplayCallback;

/**
 * Java client view of vendor.openfde.display.IOpenfdeDisplay V3.
 *
 * The vendor interface is declared with @VintfStability. That annotation is
 * intentionally omitted here because the plugin is built as an app instead of
 * an aidl_interface module, but the method order must stay identical to V3:
 * the first four methods, then registerCallback.
 */
interface IOpenfdeDisplay {
    boolean openExternalDisplay();
    boolean closeExternalDisplay();
    boolean getExternalDisplayState();
    boolean isExternalDisplaySupported();
    void registerCallback(IOpenfdeDisplayCallback callback);
}
