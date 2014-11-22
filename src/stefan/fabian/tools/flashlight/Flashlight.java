package stefan.fabian.tools.flashlight;

/**
 * Created by Stefan on 21.11.2014.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Provides basic flashlight functionality.
 * Compatibility Mode: Uses a surfaceHolder 1 Px * 1 Px (maybe hidden below an image) as preview display because some phones won't turn on the camera without this.
 */
public class Flashlight {
    public final static int TOGGLE = 0, FORCE_ON = 1, FORCE_OFF = 2;
    private static Camera cam;
    private static Window window;
    private static boolean b_on = false, compatibilityMode = false, useDisplayLight = false;
    private static float f_brightness = -1F;

    /**
     * Another way to initialize the Flashlight class.
     * Please notice however that when initialized with this method the usage of the display as light source is not supported!
     * @param compatibilityMode Whether the compatibility mode should be used or not
     * @return A boolean indicating whether the Flashlight succeeded to initialize or not.
     * @throws java.lang.RuntimeException
     */
    public static boolean Init(boolean compatibilityMode) throws RuntimeException {
        return Init(null, compatibilityMode, false);
    }

    /**
     *  Initializes the Flashlight class
     * @param window The App window (only needed if display light is used)
     * @param compatibilityMode Whether the compatibility mode should be used or not
     * @param useDisplayLight Whether the display should be used as light source.
     * @return A boolean indicating whether the Flashlight succeeded to initialize or not.
     * @throws java.lang.RuntimeException
     */
    public static boolean Init(Window window, boolean compatibilityMode, boolean useDisplayLight) throws RuntimeException {
        Flashlight.window = window;
        if (window != null) f_brightness = window.getAttributes().screenBrightness;
        Flashlight.compatibilityMode = compatibilityMode;
        Flashlight.useDisplayLight = useDisplayLight;
        if (cam != null) return true;
        cam = Camera.open();
        return cam != null;
    }

    /**
     * Sets the preview display. Needed for compatibility mode.
     * @param surfaceHolder The surfaceHolder that will contain the preview.
     * @return True if setting the surfaceHolder was successful. False otherwise
     * @throws IOException
     */
    public static boolean setPreviewDisplay(SurfaceHolder surfaceHolder) throws IOException {
        if (cam != null) {
            cam.setPreviewDisplay(surfaceHolder);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                cam.setPreviewTexture(new SurfaceTexture(0));
            return true;
        }
        return false;
    }

    /**
     * Updates whether the Flashlight should run in compatibiltity mode or not.
     * @param activity Needed to turn the Flashlight off if it was on and back on again after the compatibility mode was set.
     * @param compatibilityMode A new value whether the compatibility mode should be used or not
     */
    public static void setCompatibilityMode(Activity activity, boolean compatibilityMode) {
        boolean wasOn = b_on;
        if (wasOn) ToggleLight(activity, FORCE_OFF);
        Flashlight.compatibilityMode = compatibilityMode;
        if (wasOn) ToggleLight(activity, FORCE_ON);
    }

    /**
     * Updates whether the display should be used as light source.
     * @param activity Needed to turn the Flashlight off if it was on and back on again after the use display mode was set.
     * @param useDisplayLight A new value whether the display should be used as light source.
     */
    public static void setUseDisplayLight(Activity activity, boolean useDisplayLight) {
        boolean wasOn = b_on;
        if (wasOn) ToggleLight(activity, FORCE_OFF);
        Flashlight.useDisplayLight = useDisplayLight;
        if (wasOn) ToggleLight(activity, FORCE_ON);
    }

    public static boolean getUseDisplayLight() { return useDisplayLight; }

    /**
     * Releases the camera.
     */
    public static void ReleaseCamera() {
        if( cam != null ) {
            cam.stopPreview();
            cam.release();
            cam = null;
        }
    }

    /**
     * Closes and releases all resources used by the Flashlight.
     */
    public static void Close() {
        ReleaseCamera();
        window = null;
    }

    /**
     *
     * @return A boolean indicating whether the Flashlight is currently on (true) or off (false).
     */
    public static boolean IsOn() { return b_on; }

    /**
     * Turns the light on or off
     * @param activity Can be null only if flashlight mode is NOT use display light.
     * @param mode An integer that determines whether the light should be turned on, turned off or Toggled.
     * @return A boolean indicating whether the action was successful.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static boolean ToggleLight(Activity activity, int mode) {
        boolean turnOn = false;
        switch (mode) {
            case FORCE_ON:
                turnOn = true;
                break;
            case FORCE_OFF:
                turnOn = false;
                break;
            case TOGGLE:
                turnOn = !b_on;
        }
        if (useDisplayLight && activity != null && window != null) {
            if (turnOn) {
                activity.findViewById(R.id.window).setBackgroundColor(Color.WHITE);
                activity.findViewById(R.id.FlashlightButton).setVisibility(ImageView.INVISIBLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        activity.findViewById(R.id.window).setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    if (activity.getActionBar() != null)
                        activity.getActionBar().hide();
                }
                WindowManager.LayoutParams layout = window.getAttributes();
                layout.screenBrightness = 1F;
                window.setAttributes(layout);
            } else {
                activity.findViewById(R.id.window).setBackgroundColor(Color.TRANSPARENT);
                activity.findViewById(R.id.FlashlightButton).setVisibility(ImageView.VISIBLE);
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (activity.getActionBar() != null)
                        activity.getActionBar().show();
                }
                WindowManager.LayoutParams layout = window.getAttributes();
                layout.screenBrightness = f_brightness;
                window.setAttributes(layout);
            }
            b_on = turnOn;
            return true;
        }
        if (cam == null && activity != null) Init(activity.getWindow(), compatibilityMode, useDisplayLight);
        if (cam == null) return false;
        Camera.Parameters p = cam.getParameters();
        if (turnOn) {
            if (window != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            cam.setParameters(p);
            if (compatibilityMode) {
                cam.startPreview();
            }
        } else {
            if (window != null) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (!compatibilityMode) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                cam.setParameters(p);
            } else {
                ReleaseCamera();
            }
        }
        b_on = turnOn;
        return true;
    }
}
