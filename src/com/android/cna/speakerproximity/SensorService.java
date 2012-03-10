package com.android.cna.speakerproximity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class SensorService extends Service {
	private static Context			ctx;
	private static TelephonyManager	telephony;
	private MyPhoneStateListener	phoneListener;

	@Override
	public void onStart(Intent intent, int startId) {

		ctx = getApplicationContext();

		if (phoneListener == null) {
			phoneListener = new MyPhoneStateListener(ctx, this);
		}

		if (telephony == null) {
			telephony = (TelephonyManager) ctx
					.getSystemService(Context.TELEPHONY_SERVICE);
		}

		telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
