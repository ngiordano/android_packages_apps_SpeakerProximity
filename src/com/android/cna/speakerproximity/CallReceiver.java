package com.android.cna.speakerproximity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class CallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"active", false)) {
			return;
		}

		context.startService(new Intent(context, SensorService.class));
	}
}
