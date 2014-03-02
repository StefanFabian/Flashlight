package stefan.fabian.tools.flashlight;

import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
	private final static int TOGGLE = 0, FORCE_ON = 1, FORCE_OFF = 2;
	private static boolean b_on = false;
	private static float f_brightness = -1F;
	private Camera cam;
	private SharedPreferences pref;
    private SurfaceHolder surfaceHolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		f_brightness = getWindow().getAttributes().screenBrightness;
		init();
	}

    /**
     * Initializes the flashlight app by loading the preferences and initializing the camera if existent
     */
	private void init() {
		if (pref == null)
			pref = getSharedPreferences("FLASHLIGHT_SF_PREF", 0);
		if (!pref.getBoolean("no_cam", false))  {
            try {
                cam = Camera.open();
                if (cam == null) {
                    Toast.makeText(this, "No camera found!\nUsing Display instead!", Toast.LENGTH_LONG).show();
                    Editor editor = pref.edit();
                    editor.putBoolean("no_cam", true);
                    editor.commit();
                }
            } catch (RuntimeException e) {
                Toast.makeText(this, "Exception:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		menu.findItem(R.id.preview_mode).setChecked(pref.getBoolean("preview_mode", false));
		menu.findItem(R.id.force_display_light).setChecked(pref.getBoolean("force_display_light", false));
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.preview_mode).setVisible(!(pref.getBoolean("force_display_light", false)||pref.getBoolean("no_cam", false)));
		menu.findItem(R.id.force_display_light).setVisible(!pref.getBoolean("no_cam", false));
        menu.findItem(R.id.preview_mode).setChecked(pref.getBoolean("preview_mode", false));
        menu.findItem(R.id.force_display_light).setChecked(pref.getBoolean("force_display_light", false));
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if (item.getItemId() == R.id.preview_mode) {
			Editor editor = pref.edit();
			editor.putBoolean("preview_mode", !pref.getBoolean("preview_mode", false));
			editor.commit();
			item.setChecked(pref.getBoolean("preview_mode", false));
            toggleLight(FORCE_OFF);
		} else if (item.getItemId() == R.id.force_display_light) {
			Editor editor = pref.edit();
			editor.putBoolean("force_display_light", !pref.getBoolean("force_display_light", false));
			editor.commit();
			item.setChecked(pref.getBoolean("force_display_light", false));
			toggleLight(FORCE_OFF);
		} else if (item.getItemId() == R.id.help) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.help);
			builder.setMessage(R.string.help_message);
			builder.show();
		} else if (item.getItemId() == R.id.about) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.about_title);
			builder.setMessage(R.string.about_message);
			builder.show();
		}
		return true;
	}
	
	public void toggleLight(View v) { b_on = !b_on; toggleLight(TOGGLE); }
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void toggleLight(int mode) {
		Window window = getWindow();
		if (pref.getBoolean("no_cam", false) || pref.getBoolean("force_display_light", false)) {
			if (mode != FORCE_ON && (!b_on || mode == FORCE_OFF)) {
				findViewById(R.id.window).setBackgroundColor(Color.TRANSPARENT);
				findViewById(R.id.FlashlightButton).setVisibility(ImageView.VISIBLE);
				window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				    if (getActionBar() != null)
					    getActionBar().show();
                }
				WindowManager.LayoutParams layout = window.getAttributes();
				layout.screenBrightness = f_brightness;
				window.setAttributes(layout);
			} else {
				findViewById(R.id.window).setBackgroundColor(Color.WHITE);
				findViewById(R.id.FlashlightButton).setVisibility(ImageView.INVISIBLE);
				window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        findViewById(R.id.window).setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
				    if (getActionBar() != null)
					    getActionBar().hide();
                }
				WindowManager.LayoutParams layout = window.getAttributes();
				layout.screenBrightness = 1F;
				window.setAttributes(layout);
			}
			return;
		}
        if (cam == null) init();
        if (cam == null) return;
		Parameters p = cam.getParameters();
		if (mode != FORCE_ON && (!b_on || mode == FORCE_OFF)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            p.setFlashMode(Parameters.FLASH_MODE_OFF);
            cam.setParameters(p);
			if (pref.getBoolean("preview_mode", false)) {
				cam.stopPreview();
                cam.release();
                cam = null;
            }
			((ImageView)findViewById(R.id.FlashlightButton)).setImageResource(R.drawable.flashlight_icon_off);
		} else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            p.setFlashMode(Parameters.FLASH_MODE_TORCH);
            cam.setParameters(p);
			if (pref.getBoolean("preview_mode", false)) {
				cam.startPreview();
            }
			((ImageView)findViewById(R.id.FlashlightButton)).setImageResource(R.drawable.flashlight_icon_on);
            if (!pref.getBoolean("asked_if_is_compatible", false)) {
                Editor editor = pref.edit();
                editor.putBoolean("asked_if_is_compatible", true);
                editor.commit();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.is_working_title);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, R.string.great, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNeutralButton(R.string.is_not_working, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        toggleLight(FORCE_OFF);
                        Editor editor = pref.edit();
                        editor.putBoolean("preview_mode", true);
                        editor.commit();
                        toggleLight(FORCE_ON);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.is_working_title);
                        builder.setMessage(R.string.is_working_message2);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, R.string.great, Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder.setNegativeButton(R.string.still_not_working, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, R.string.what_a_shame, Toast.LENGTH_SHORT).show();
                                toggleLight(FORCE_OFF);
                                Editor editor = pref.edit();
                                editor.putBoolean("preview_mode", false);
                                editor.putBoolean("force_display_light", true);
                                editor.commit();
                                toggleLight(FORCE_ON);
                            }
                        });
                        builder.show();
                    }
                });
                builder.show();
            }
		}
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	public void onResume() {
		super.onResume();
		if( cam == null ) {
			init();
		}
		toggleLight(TOGGLE);
	}
	
	public void onPause() {
		super.onPause();
		toggleLight(FORCE_OFF);
	}
	
	public void onStop() {
		super.onStop();
		if( cam != null ) {
			cam.stopPreview();
			cam.release();
			cam = null;
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
		if( cam != null ) {
			cam.stopPreview();
			cam.release();
			cam = null;
		}
	}


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {}

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        this.surfaceHolder = surfaceHolder;
        if (cam != null) {
            try {
                cam.setPreviewDisplay(this.surfaceHolder);
            } catch (IOException e) {
                Toast.makeText(this, "Exception occured!\nYou can report this to me if you want.\nMessage:\n" + e.getMessage(), Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = null;
    }
}
