package com.android.cna.speakerproximity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HeadSetPlugReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getExtras().getInt("state") == 0) {
			SPApp.getInstance().registerProximityListener();
			SPApp.getInstance().setHeadsetConnected(false);
		} else {
			SPApp.getInstance().unregisterProximityListener();
			SPApp.getInstance().setHeadsetConnected(true);
		}
	}
}
