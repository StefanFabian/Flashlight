package stefan.fabian.tools.flashlight;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
	private SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
                if (!Flashlight.Init(getWindow(), pref.getBoolean("preview_mode", false), pref.getBoolean("no_cam", false) || pref.getBoolean("force_display_light", false))) {
                    Toast.makeText(this, "No camera found!\nUsing Display instead!", Toast.LENGTH_LONG).show();
                    Editor editor = pref.edit();
                    editor.putBoolean("no_cam", true);
                    editor.commit();
                }
            } catch (RuntimeException e) {
                Toast.makeText(this, "Exception:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
		}
        if (pref.getBoolean("light_on_start", false)) {
            Flashlight.ToggleLight(this, Flashlight.FORCE_ON);
            updateWidget();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		menu.findItem(R.id.preview_mode).setChecked(pref.getBoolean("preview_mode", false));
		menu.findItem(R.id.force_display_light).setChecked(pref.getBoolean("force_display_light", false));
        menu.findItem(R.id.light_on_start).setChecked(pref.getBoolean("light_on_start", false));
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
        // Not really sure why I set these values twice
		menu.findItem(R.id.preview_mode).setVisible(!(pref.getBoolean("force_display_light", false) || pref.getBoolean("no_cam", false)));
		menu.findItem(R.id.force_display_light).setVisible(!pref.getBoolean("no_cam", false));
        menu.findItem(R.id.preview_mode).setChecked(pref.getBoolean("preview_mode", false));
        menu.findItem(R.id.force_display_light).setChecked(pref.getBoolean("force_display_light", false));
        menu.findItem(R.id.light_on_start).setChecked(pref.getBoolean("light_on_start", false));
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if (item.getItemId() == R.id.preview_mode) {
			Editor editor = pref.edit();
			editor.putBoolean("preview_mode", !pref.getBoolean("preview_mode", false));
			editor.commit();
			item.setChecked(pref.getBoolean("preview_mode", false));
            Flashlight.setCompatibilityMode(this, pref.getBoolean("preview_mode", false));
		} else if (item.getItemId() == R.id.force_display_light) {
			Editor editor = pref.edit();
			editor.putBoolean("force_display_light", !pref.getBoolean("force_display_light", false));
			editor.commit();
			item.setChecked(pref.getBoolean("force_display_light", false));
            Flashlight.setUseDisplayLight(this, pref.getBoolean("no_cam", false) || pref.getBoolean("force_display_light", false));
		} else if (item.getItemId() == R.id.light_on_start) {
            Editor editor = pref.edit();
            editor.putBoolean("light_on_start", !pref.getBoolean("light_on_start", false));
            editor.commit();
            item.setChecked(pref.getBoolean("light_on_start", false));
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

    /**
     * Callback for the Flashlight ImageView
     * @param v The ImageView View
     */
    public void toggleLight (View v) {
        if (!pref.getBoolean("asked_if_is_compatible", false)) {
            SharedPreferences.Editor editor = pref.edit();
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
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("preview_mode", true);
                    editor.commit();
                    Flashlight.setCompatibilityMode(MainActivity.this, true);
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
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("preview_mode", false);
                            editor.putBoolean("force_display_light", true);
                            editor.commit();
                            Flashlight.setUseDisplayLight(MainActivity.this, true);
                        }
                    });
                    builder.show();
                }
            });
            builder.show();
        }
        if (Flashlight.ToggleLight(this, Flashlight.TOGGLE)) {
            ((ImageView) findViewById(R.id.FlashlightButton)).setImageResource(Flashlight.IsOn() ? R.drawable.flashlight_icon_on : R.drawable.flashlight_icon_off);
            updateWidget();
        }
    }

	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SurfaceHolder surfaceHolder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();
        surfaceHolder.addCallback(this);
        if (getIntent().getAction().equals("FORCE_ON")) {
            Flashlight.ToggleLight(this, Flashlight.FORCE_ON);
            updateWidget();
        }
    }
	
    @Override
	public void onResume() {
		super.onResume();
        Flashlight.Init(getWindow(), pref.getBoolean("preview_mode", false), pref.getBoolean("no_cam", false) || pref.getBoolean("force_display_light", false));
        ((ImageView) findViewById(R.id.FlashlightButton)).setImageResource(Flashlight.IsOn() ? R.drawable.flashlight_icon_on : R.drawable.flashlight_icon_off);
	}

    @Override
	public void onDestroy() {
		super.onDestroy();
        Flashlight.close();
	}


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            Flashlight.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Toast.makeText(this, "Exception occured!\nYou can report this to me if you want.\nMessage:\n" + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    private void updateWidget() {
        Intent intent = new Intent(this, FlashlightWidget.class);
        intent.setAction("Update");
        sendBroadcast(intent);
    }
}
