package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;

/**
 * Main activity. It is not used for the moment, but will be in future releases
 * as a general menu.
 * 
 * @author Nicolas Guillaumn
 * 
 */
public class OSMTracker extends Activity {

	public static final class Preferences {
		/**
		 * Property names
		 */
		public final static String KEY_STORAGE_DIR = "logging.storage.dir";
		public final static String KEY_VOICEREC_DURATION = "voicerec.duration";

		/**
		 * Default values
		 */
		public final static String VAL_STORAGE_DIR = "/osmtracker";
		public final static String VAL_VOICEREC_DURATION = "2";
	};

	/**
	 * Intent for tracking a waypoint
	 */
	public final static String INTENT_TRACK_WP = "me.guillaumin.android.osmtracker.intent.TRACK_WP";

	/**
	 * Intent to start tracking
	 */
	public final static String INTENT_START_TRACKING = "me.guillaumin.android.osmtracker.intent.START_TRACKING";

	/**
	 * Intent to stop tracking
	 */
	public final static String INTENT_STOP_TRACKING = "me.guillaumin.android.osmtracker.intent.STOP_TRACKING";

	/**
	 * Key for extra data "waypoint name" in Intent
	 */
	public final static String INTENT_KEY_NAME = "name";

	/**
	 * Key for extra data "link" in Intent
	 */
	public final static String INTENT_KEY_LINK = "link";

}