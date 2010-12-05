package me.guillaumin.android.osmtracker.layout;

import java.text.DecimalFormat;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.listener.StillImageOnClickListener;
import me.guillaumin.android.osmtracker.listener.TextNoteOnClickListener;
import me.guillaumin.android.osmtracker.listener.ToggleRecordOnCheckedChangeListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Layout for the GPS Status image and misc
 * action buttons.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class GpsStatusRecord extends LinearLayout implements Listener, LocationListener {
	
	private final static String TAG = GpsStatusRecord.class.getSimpleName();
	
	/**
	 * Formatter for accuracy display.
	 */
	private final static DecimalFormat ACCURACY_FORMAT = new DecimalFormat("0");
	
	/**
	 * Keeps matching between satellite indicator bars to draw, and numbers
	 * of satellites for each bars;
	 */
	private final static int[] SAT_INDICATOR_TRESHOLD = {2, 3, 4, 6, 8};
	
	/**
	 * Containing activity
	 */
	private TrackLogger activity;
	
	/**
	 * Reference to LocationManager
	 */
	private LocationManager lmgr;
	
	/**
	 * Is GPS active ?
	 */
	private boolean gpsActive = false;

	public GpsStatusRecord(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.gpsstatus_record, this, true);

		if (context instanceof TrackLogger) {
			activity = (TrackLogger) context;
			// Register listeners
			((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack)).setOnCheckedChangeListener(new ToggleRecordOnCheckedChangeListener(activity));;
			((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setOnClickListener(new VoiceRecOnClickListener(activity));
			((Button) findViewById(R.id.gpsstatus_record_btnStillImage)).setOnClickListener(new StillImageOnClickListener(activity));
			((Button) findViewById(R.id.gpsstatus_record_btnTextNote)).setOnClickListener(new TextNoteOnClickListener(activity));
			
			// Disable by default the buttons
			findViewById(R.id.gpsstatus_record_toggleTrack).setEnabled(false);
			setButtonsEnabled(false);
			
			lmgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}		

	}
	
	public void requestLocationUpdates(boolean request) {
		if (request) {
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			lmgr.addGpsStatusListener(this);
		} else {
			lmgr.removeUpdates(this);
			lmgr.removeGpsStatusListener(this);
		}
	}

	/**
	 * Enables or disable the buttons.
	 * 
	 * @param enabled
	 *            If true, enable the buttons, otherwise disable them.
	 */
	public void setButtonsEnabled(boolean enabled) {
		findViewById(R.id.gpsstatus_record_btnVoiceRecord).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_btnStillImage).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_btnTextNote).setEnabled(enabled);
		
	}

	@Override
	public void onGpsStatusChanged(int event) {
		// Update GPS Status image according to event
		ImageView imgSatIndicator = (ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator);

		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_0);
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_unknown);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_off);
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			GpsStatus status = lmgr.getGpsStatus(null);

			// Count active satellites
			int satCount = 0;
			for (GpsSatellite sat:status.getSatellites()) {
				satCount++;
			}
			
			// Count how many bars should we draw
			int nbBars = 0;
			for (int i=0; i<SAT_INDICATOR_TRESHOLD.length; i++) {
				if (satCount >= SAT_INDICATOR_TRESHOLD[i]) {
					nbBars = i;
				}
			}
			Log.v(TAG, "Found " + satCount + " satellites. Will draw " + nbBars + " bars.");			
			imgSatIndicator.setImageResource(getResources().getIdentifier("drawable/sat_indicator_" + nbBars, null, OSMTracker.class.getPackage().getName()));
			break;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(TAG, "Location received " + location);
		if (! gpsActive) {
			gpsActive = true;
			// GPS activated, activate UI
			activity.onGpsEnabled();
		}
		
		TextView tvAccuracy = (TextView) findViewById(R.id.gpsstatus_record_tvAccuracy);
		if (location.hasAccuracy()) {
			tvAccuracy.setText(ACCURACY_FORMAT.format(location.getAccuracy()) + getResources().getString(R.string.various_unit_meters));
		} else {
			tvAccuracy.setText("");
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " disabled");
		gpsActive = false;
		((ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator)).setImageResource(R.drawable.sat_indicator_off);
		((TextView) findViewById(R.id.gpsstatus_record_tvAccuracy)).setText("");
		activity.onGpsDisabled();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " enabled");
		((ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator)).setImageResource(R.drawable.sat_indicator_unknown);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Update provider status image according to status
		Log.d(TAG, "Location provider " + provider + " status changed to: " + status);
		ImageView imgSatIndicator = (ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator);
		TextView tvAccuracy = (TextView) findViewById(R.id.gpsstatus_record_tvAccuracy);
		
		switch (status) {
		// Don't do anything for status AVAILABLE, as this event occurs frequently,
		// changing the graphics cause flickering .
		case LocationProvider.OUT_OF_SERVICE:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_off);
			tvAccuracy.setText("");
			gpsActive = false;
			activity.onGpsDisabled();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_unknown);
			tvAccuracy.setText("");
			gpsActive = false;
			break;
		}

	}

}