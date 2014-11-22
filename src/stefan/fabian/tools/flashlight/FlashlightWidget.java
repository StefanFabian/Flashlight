package stefan.fabian.tools.flashlight;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * Created by Stefan on 28.09.14.
 */
public class FlashlightWidget extends AppWidgetProvider {
    private SharedPreferences pref;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for(int id : appWidgetIds) {
            Intent intent = new Intent(context, FlashlightWidget.class);
            intent.setAction("Tapped");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flashlight_widget);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            views.setImageViewResource(R.id.widget_image, R.drawable.flashlight_icon);
            views.setTextViewText(R.id.widget_text, context.getString(Flashlight.IsOn() ? R.string.on : R.string.off));
            appWidgetManager.updateAppWidget(id, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Make sure that we received the signal because the widget was tapped or needs to update (i.e. the light was turned on from the app)
        boolean update = intent.getAction().equals("Update");
        if (intent.getAction().equals("Tapped")) {
            if (pref == null) pref = context.getSharedPreferences("FLASHLIGHT_SF_PREF", 0);
            if (Flashlight.getUseDisplayLight() || !pref.getBoolean("asked_if_is_compatible", false)) {
                Intent intentMain = new Intent(context, MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Needed or else it'll crash
                intentMain.setAction("FORCE_ON");
                context.startActivity(intentMain);
            } else {
                Flashlight.Init(pref.getBoolean("compatibility_mod", false));
                Flashlight.ToggleLight(null, Flashlight.TOGGLE);
                update = true;
            }
        }
        if (update) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flashlight_widget);
            views.setInt(R.id.widget_layout, "setBackgroundResource", Flashlight.IsOn() ? R.drawable.widget_background_on : R.drawable.widget_background);
            views.setTextViewText(R.id.widget_text, context.getString(Flashlight.IsOn() ? R.string.on : R.string.off));
            ComponentName appWidget = new ComponentName(context, FlashlightWidget.class);
            AppWidgetManager awm = AppWidgetManager.getInstance(context);
            awm.updateAppWidget(appWidget, views);
        }
        if (intent.getAction().equals("Tapped")) {
            // Release camera if Flashlight was turned off after a short delay to make sure the change was done.
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
            if (!Flashlight.IsOn())
                Flashlight.ReleaseCamera();
        }
    }
}