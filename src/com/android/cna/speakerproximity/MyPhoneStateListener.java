package com.android.cna.speakerproximity;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class MyPhoneStateListener extends PhoneStateListener {

	private final Context					ctx;
	private final SPApp						app;
	private final SharedPreferences			prefs;
	private final SharedPreferences.Editor	prefsEditor;
	private final SensorService				sensorService;
	private final AudioManager				audiomanager;
	private final PowerManager				pm;
	private PowerManager.WakeLock		wl;

	private boolean							phoneWasCovered					= false;
	private int								conference						= -1;

	private final static String				LAST_STATE						= "LastState";
	private final static String				LAST_PROXIMITY_STATE			= "LastProximityState";
	private final static String				LAST_CONFERENCE_STATE			= "LastConferenceState";
	private final static String				SPEAKER_SETTING_BEFORE			= "SpeakerSettingBefore";
	public static final int					PROXIMITY_SCREEN_OFF_WAKE_LOCK	= 32;

	public MyPhoneStateListener(Context context, SensorService sensorService) {
		super();
		ctx = context;
		app = SPApp.getInstance();
		this.sensorService = sensorService;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		prefsEditor = prefs.edit();
		app.setSensorManager((SensorManager) ctx
				.getSystemService(Context.SENSOR_SERVICE));
		audiomanager = (AudioManager) ctx
				.getSystemService(Context.AUDIO_SERVICE);
		pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);

		if (prefs.getBoolean("handleScreenOff", true)) {
			try {
				wl = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SpeakerProximity");
			} catch (Exception e) {
				wl = null;
				SPApp.log("can't get wakelock to turn display off, sorry");
			}
		}
		app.setProximityListener(new SensorEventListener() {
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {

				if (prefs.getFloat(LAST_PROXIMITY_STATE, -1) == event.values[0]) {
					return;
				} else {
					prefsEditor.putFloat(LAST_PROXIMITY_STATE, event.values[0]);
					prefsEditor.commit();
				}

				final float proxVal = event.values[0];
				final int sensorType = event.sensor.getType();
				final String sensorName = event.sensor.getName();

				SPApp.log("SensorEvent: Type[" + sensorType + "] Name["
						+ sensorName + "] value(0)[" + proxVal + "]");
				final boolean headsetOff = prefs.getBoolean("headset", true) ? (!app
						.isHeadsetConnected() && !audiomanager
						.isWiredHeadsetOn())
						: true;
				SPApp.log("HeadsetSetting=" + prefs.getBoolean("headset", true)
						+ " isHeadsetConnected()=" + app.isHeadsetConnected()
						+ " isWiredHeadsetOn()="
						+ audiomanager.isWiredHeadsetOn()
						+ " headtes_off_eval=" + headsetOff);

				final float init = prefs.getFloat("calibration_init", -1.0f);
				final float covered = prefs.getFloat("calibration_covered",
						-1.0f);
				final float uncovered = prefs.getFloat("calibration_uncovered",
						-1.0f);

				if (sensorType == Sensor.TYPE_PROXIMITY && headsetOff) {
					if (proxVal == init && !phoneWasCovered
							&& !prefs.getBoolean("speakerStart", false)) {
						SPApp.log("ProximityEvent[" + proxVal + "]");
						phoneWasCovered = true;
						return;
					} else if (proxVal == covered) {
						audiomanager.setSpeakerphoneOn(false);
						SPApp.log("Covered ProximityEvent[" + proxVal + "]");
					} else if (proxVal == uncovered) {
						audiomanager.setSpeakerphoneOn(true);
						SPApp.log("Free ProximityEvent[" + proxVal + "]");
					} else {
						SPApp
								.log("ProximityEvent[" + proxVal
										+ "] not handled");
					}
				} else {
					SPApp.log("headset is in use");
				}
			}
		});

	}

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		SPApp
				.log("active="
						+ prefs.getBoolean("active", false)
						+ " Has proximity sensor="
						+ ((app.getSensorManager().getDefaultSensor(
								Sensor.TYPE_PROXIMITY) == null) ? "no proximity sensor detected"
								: app.getSensorManager().getDefaultSensor(
										Sensor.TYPE_PROXIMITY).getName()));

		if ((state == prefs.getInt(LAST_STATE, -1))) {
			return;
		} else if (!prefs.getBoolean("active", false)) {
			return;
		}

		prefsEditor.putInt(LAST_STATE, state);
		prefsEditor.commit();

		switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				app.setInCall(false);
				app.getSensorManager().unregisterListener(
						app.getProximityListener());

				if (app.getOrientationListener() != null) {
					app.getSensorManager().unregisterListener(
							app.getOrientationListener());
				}

				audiomanager.setSpeakerphoneOn(prefs.getBoolean(
						SPEAKER_SETTING_BEFORE, false));
				phoneWasCovered = false;
				SPApp.log("Phone gets IDLE");

				if (wl != null) {

					if (wl.isHeld()) {
						wl.release();
					}
				}

				if (app.getHeadSetPlugReceiver() != null) {
					ctx.unregisterReceiver(app.getHeadSetPlugReceiver());
				}
				if (app.getBluetoothConnectReceiver() != null) {
					ctx.unregisterReceiver(app.getBluetoothConnectReceiver());
				}
				if (app.getBluetoothDisconnectReceiver() != null) {
					ctx
							.unregisterReceiver(app
									.getBluetoothDisconnectReceiver());
				}

				sensorService.stopSelf();

				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				app.setInCall(true);

				prefsEditor.putBoolean(SPEAKER_SETTING_BEFORE, audiomanager
						.isSpeakerphoneOn());
				prefsEditor.commit();

				if (!app.registerProximityListener()) {
					SPApp.log("No proximity sensor available");
				}
				SPApp.log("Phone is picked up");

				if (prefs.getBoolean("handleScreenOff", true)) {
					if (wl != null) {

						if (!wl.isHeld()) {
							wl.acquire();
						}
					}
				}

				if (prefs.getBoolean("headset", true)) {
					app.setHeadSetPlugReceiver(new HeadSetPlugReceiver());
					ctx
							.registerReceiver(
									app.getHeadSetPlugReceiver(),
									new IntentFilter(
											android.content.Intent.ACTION_HEADSET_PLUG));
				}

				if (prefs.getBoolean("conferenceCall", true)) {
					app.setOrientationListener(new SensorEventListener() {
						@Override
						public void onAccuracyChanged(Sensor sensor,
								int accuracy) {
						}

						@Override
						public void onSensorChanged(SensorEvent event) {

							final float z = event.values[2];
							conference = (z < -8 && z > -10) ? 1 : 0;

							final int lastConf = prefs.getInt(
									LAST_CONFERENCE_STATE, -1);
							if (lastConf != -1 && conference != -1
									&& lastConf == conference) {
								return;
							} else {
								prefsEditor.putInt(LAST_CONFERENCE_STATE,
										conference);
								prefsEditor.commit();
							}
							if ((conference == 1) ? true : false) {
								SPApp.log("changed to upsidedown and flat "
										+ "event[0]=" + event.values[0]
										+ " event[1]=" + event.values[1]
										+ " event[2]=" + event.values[2]);
								app.unregisterProximityListener();
							} else {
								SPApp.log(" not anymore upsidedown and flat "
										+ "event[0]=" + event.values[0]
										+ " event[1]=" + event.values[1]
										+ " event[2]=" + event.values[2]);
								app.registerProximityListener();
							}
						}
					});
					app.getSensorManager().registerListener(
							app.getOrientationListener(),
							app.getSensorManager().getDefaultSensor(
									Sensor.TYPE_ACCELEROMETER),
							SensorManager.SENSOR_DELAY_UI);
				}
				break;
		}
	}
}
