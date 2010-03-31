package me.guillaumin.android.osmtracker.view;

import java.text.DecimalFormat;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.util.ArrayUtils;
import me.guillaumin.android.osmtracker.util.MercatorProjection;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class DisplayTrackView extends TextView {

	private static final String TAG = DisplayTrackView.class.getSimpleName();

	/**
	 * Padding (in pixels) for drawing track, to prevent touching the borders.
	 */
	private static final int PADDING = 5;

	/**
	 * Width of the scale bar, in pixels.
	 */
	private static final int SCALE_WIDTH = 50;
	
	/**
	 * Height of left & right small lines to delimit scale (pixels)
	 */
	private static final int SCALE_DELIM_HEIGHT = 10;

	/**
	 * Formatter for scale information
	 */
	private static final DecimalFormat SCALE_FORMAT = new DecimalFormat("0");

	/**
	 * Coordinates to draw (before projection)
	 */
	private double[][] coords;

	/**
	 * Array of pixels coordinates to display track
	 */
	private int[][] pixels;

	/**
	 * The projection used to convert coordinates to pixels.
	 */
	private MercatorProjection projection;

	/**
	 * Paint used for drawing track.
	 */
	private Paint trackPaint = new Paint();
	
	/**
	 * Compass bitmap
	 */
	private Bitmap compass;
	
	/**
	 * Position marker bitmap
	 */
	private Bitmap marker;
	
	/**
	 * Letter to use for meter unit (taken from resources)
	 */
	private String meterLabel;

	/**
	 * Letter to use for indicating North (taken from resources)
	 */
	private String northLabel;
	
	/**
	 * Handler for content change observation
	 */
	private Handler handler = new Handler();
	
	/**
	 * ContentObserver to be notified about any new trackpoint and
	 * redraw screen
	 */
	private class TrackPointContentObserver extends ContentObserver {

		public TrackPointContentObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			// width & height could be = 0 if the view had
			// not been attached to window & measured when onChange()
			// is fired.
			if (getWidth() > 0 && getHeight() > 0) {
				// Populate new data, and recompute projection
				populateCoords();
				projectData(getWidth(), getHeight());
				// Force view redraw
				invalidate();
			}
		}
		
	}
	
	/**
	 * Instance of TrackpointContentObserver
	 */
	private TrackPointContentObserver trackpointContentObserver;

	public DisplayTrackView(Context context) {
		super(context);

		// Set text align to centre
		getPaint().setTextAlign(Align.CENTER);
		
		// Setup track drawing paint
		trackPaint.setColor(getCurrentTextColor());
		trackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		
		// Retrieve some resources that will be used in drawing
		meterLabel = getResources().getString(R.string.various_unit_meters);
		northLabel = getResources().getString(R.string.displaytrack_north);
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
		compass = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_compass);
		
		trackpointContentObserver = new TrackPointContentObserver(handler);
		context.getContentResolver().registerContentObserver(TrackContentProvider.CONTENT_URI_TRACKPOINT, true, trackpointContentObserver);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.v(TAG, "onSizeChanged: " + w + "," + h + ". Old: " + oldw + "," + oldh);
		
		// Populate data from content provider
		populateCoords();
		// Project coordinates into 2D screen
		projectData(w, h);
	
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDetachedFromWindow() {
		// Unregister content observer
		getContext().getContentResolver().unregisterContentObserver(trackpointContentObserver);
		super.onDetachedFromWindow();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// If we have data to paint
		if (pixels != null && pixels.length > 0) {
			for (int i = 1; i < pixels.length; i++) {
				// Draw a line between each point
				canvas.drawLine(PADDING + pixels[i - 1][MercatorProjection.X], PADDING
						+ pixels[i - 1][MercatorProjection.Y], PADDING + pixels[i][MercatorProjection.X], PADDING
						+ pixels[i][MercatorProjection.Y], trackPaint);
				
				if (i+1 >= pixels.length) {
					// We're on last point. Draw current position marker
					canvas.drawBitmap(marker, pixels[i][MercatorProjection.X], pixels[i][MercatorProjection.Y], this.getPaint());
				}
			}
			// Draw scale information
			drawScale(canvas);
		}
		// Draw static resources
		drawStatic(canvas);

	}

	/**
	 * Draw scale information.
	 * 
	 * @param canvas
	 *            Canvas used to draw
	 */
	private void drawScale(Canvas canvas) {
		double scale = projection.getScale();
		Log.v(TAG, "Scale is: " + scale);

		// Draw horizontal line
		canvas.drawLine(getWidth() - PADDING - SCALE_WIDTH, PADDING+SCALE_DELIM_HEIGHT/2, getWidth() - PADDING, PADDING+SCALE_DELIM_HEIGHT/2, this.getPaint());
		
		// Draw 2 small vertical lines for the bounds
		canvas.drawLine(getWidth() - PADDING - SCALE_WIDTH, PADDING, getWidth() - PADDING - SCALE_WIDTH,
				PADDING + SCALE_DELIM_HEIGHT, this.getPaint());
		canvas.drawLine(getWidth() - PADDING, PADDING, getWidth() - PADDING, PADDING + SCALE_DELIM_HEIGHT, this.getPaint());
		
		// Draw scale
		canvas.drawText(SCALE_FORMAT.format(100*1000*scale*SCALE_WIDTH) + meterLabel, getWidth() - PADDING - SCALE_WIDTH / 2,
				PADDING + SCALE_DELIM_HEIGHT + getPaint().getTextSize(), this.getPaint());
	}

	/**
	 * Draw various static gfx (Compass ...)
	 * 
	 * @param canvas
	 *            Canvas used to draw
	 */
	private void drawStatic(Canvas canvas) {
		canvas.drawBitmap(compass, PADDING, getHeight() - PADDING - compass.getHeight(), null);
		canvas.drawText(northLabel, PADDING + compass.getWidth() / 2, getHeight() - PADDING - compass.getHeight() - 5,
				this.getPaint());
	}
	
	/**
	 * Populate coordinates from a cursor to current track Database
	 */
	public void populateCoords() {		
		Cursor c = getContext().getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACKPOINT, null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");
		coords = new double[c.getCount()][2];
		int i=0;
		
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			coords[i][MercatorProjection.LONGITUDE] = c.getDouble(c.getColumnIndex(Schema.COL_LONGITUDE));
			coords[i][MercatorProjection.LATITUDE] = c.getDouble(c.getColumnIndex(Schema.COL_LATITUDE));
			i++;
		}
		c.close();
		
		Log.v(TAG, "Extracted " + coords.length + " points from DB.");
	}
	
	/**
	 * Project current coordinates into a 2D screen
	 * @param width Width of the display screen
	 * @param height Height of the display screen
	 */
	public void projectData(int width, int height) {
		// If we got coordinates, start projecting.
		if (coords != null && coords.length > 0) {
			projection = new MercatorProjection(ArrayUtils.findMin(coords, MercatorProjection.LATITUDE), ArrayUtils.findMin(coords,
					MercatorProjection.LONGITUDE), ArrayUtils.findMax(coords, MercatorProjection.LATITUDE), ArrayUtils.findMax(coords,
					MercatorProjection.LONGITUDE), width - PADDING * 2, height - PADDING * 2);

			// Project each coordinate into pixels.
			pixels = new int[coords.length][2];
			for (int i = 0; i < coords.length; i++) {
				pixels[i] = projection.project(coords[i][MercatorProjection.LONGITUDE],
						coords[i][MercatorProjection.LATITUDE]);
			}
		}
	}

}
