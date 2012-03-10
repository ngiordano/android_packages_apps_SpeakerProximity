package com.android.cna.speakerproximity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context ctx, Intent intent) {

		SPApp.getInstance().setHeadsetConnected(false);
		SPApp.log("HeadsetConnected variable has ben reset to false");
	}
}
