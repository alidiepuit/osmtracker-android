package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.OpenStreetMapViewPathOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewSimpleLocationOverlay;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Display current track over an OSM map.
 * Based on osmdroid code http://osmdroid.googlecode.com/
 * 
 * @author Viesturs Zarins
 *
 */
public class DisplayTrackMap extends Activity implements OpenStreetMapConstants{

	private static final String TAG = DisplayTrackMap.class.getSimpleName();
	
	/**
	 * Default zoom level
	 */
	private static final int DEFAULT_ZOOM  = 16;

	/**
	 * Main OSM view
	 */
	private OpenStreetMapView osmView;
	
	/**
	 * Controller to interact with view
	 */
	private OpenStreetMapViewController osmViewController;
	
	/**
	 * OSM view overlay that displays current location
	 */
	private OpenStreetMapViewSimpleLocationOverlay myLocationOverlay;
	
	/**
	 * OSM view overlay that displays current path
	 */
	private OpenStreetMapViewPathOverlay pathOverlay;

	/**
	 * Observes changes on trackpoints
	 */
	private ContentObserver trackpointContentObserver;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaytrackmap);
        
        // Initialize OSM view
        osmView = (OpenStreetMapView) findViewById(R.id.displaytrackmap_osmView);
        osmViewController = osmView.getController();
		osmViewController.setZoom(DEFAULT_ZOOM);
        createOverlays();

        // Create content observer for trackpoints
        trackpointContentObserver = new ContentObserver(new Handler()) {
    		@Override
    		public void onChange(boolean selfChange) {		
    			pathChanged();		
    		}
    	};
        
        // Register listeners for zoom buttons
        findViewById(R.id.displaytrackmap_imgZoomIn).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomIn();
			}
        });
        findViewById(R.id.displaytrackmap_imgZoomOut).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomOut();
			}
        });
   }
    
    
	@Override
	protected void onResume() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
		
		// Register content observer for any trackpoint changes
		getContentResolver().registerContentObserver(TrackContentProvider.CONTENT_URI_TRACKPOINT, true, trackpointContentObserver);
		
        // Reload path
        pathChanged();
        
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		
		// Unregister content observer
		getContentResolver().unregisterContentObserver(trackpointContentObserver);
		
		// Clear the points list.
		pathOverlay.clearPath();
		
		super.onPause();
	}	

	/**
	 * Creates overlays over the OSM view
	 */
	private void createOverlays() {
        pathOverlay = new OpenStreetMapViewPathOverlay(Color.BLUE);
        osmView.getOverlays().add(pathOverlay);
        
        myLocationOverlay = new OpenStreetMapViewSimpleLocationOverlay(
        		BitmapFactory.decodeResource(getResources(),R.drawable.marker), 
        		8,8);
        osmView.getOverlays().add(myLocationOverlay);
	}
	
	/**
	 * On track path changed, update the two overlays and repaint view.
	 */
	private void pathChanged() {
		if (isFinishing()) {
			return;
		}
		
		// Update only the new points
		Cursor c = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACKPOINT, null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");
		int existingPoints = pathOverlay.getNumberOfPoints();
		
		// Process only if we have data, and new data only
		if (c.getCount() > 0 &&  c.getCount() > existingPoints) {		
			c.moveToPosition(existingPoints);
			double lastLat = 0;
			double lastLon = 0;
		
			// Add each new point to the track
			while(!c.isAfterLast()) {			
				lastLat = c.getDouble(c.getColumnIndex(Schema.COL_LATITUDE));
				lastLon = c.getDouble(c.getColumnIndex(Schema.COL_LONGITUDE));
				pathOverlay.addPoint((int)(lastLat * 1e6), (int)(lastLon * 1e6));
				c.moveToNext();
			}		
		
			// Last point is current position.
			GeoPoint currentPosition = new GeoPoint(lastLat, lastLon); 
			myLocationOverlay.setLocation(currentPosition);		
			osmViewController.setCenter(currentPosition);
		
			// Repaint
			osmView.invalidate();
		}
		c.close();
	}
}