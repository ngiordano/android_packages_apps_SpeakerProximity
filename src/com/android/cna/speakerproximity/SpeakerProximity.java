package com.android.cna.speakerproximity;

import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class SpeakerProximity extends ActivityGroup {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainlayout);

		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null) {

			final SharedPreferences.Editor prefsEditor = PreferenceManager
					.getDefaultSharedPreferences(this).edit();
			prefsEditor.putBoolean("active", false);
			prefsEditor.commit();
            mainLayout.addView(getLayoutInflater().inflate(R.layout.error,
                    null, false));

		} else {
			setTitle(getTitle()
					+ " - "
					+ sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
							.getName());
			mainLayout.addView(getViewFromIntent("preferences", new Intent(
					this, PreferenceScreen.class)));
		}

	}

	public View getViewFromIntent(String tag, Intent intent) {

		final Window w = getLocalActivityManager().startActivity(tag, intent);
		final View wd = w != null ? w.getDecorView() : null;
		return wd;
	}
}