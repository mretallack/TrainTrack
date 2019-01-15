package uk.org.retallack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.JsonReader;
import android.util.Log;
import android.Manifest;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;

public class StateMon extends Service  {

	static final public String RESULT = "uk.org.retallack.result";
	static final public String MESSAGE = "uk.org.retallack.newstate";
	static final public String OPENIN = "uk.org.retallack.openin";
	static final public String START = "uk.org.retallack.start";
	static final public String STOP = "uk.org.retallack.stop";
	
	static final public String UPDATETIME = "updatetime";
	private static final int MY_NOTIFICATION_ID=1;
	
	static final public int FAST_UPDATE = 3;
	static final public int SLOW_UPDATE = 100;
	
	static final public String UPDATE_PERIOD = "updateperiod";

	private Thread t; 
	
	private boolean blnOldState = false; 
	
	private boolean blnOverrideActice = false;
	
	private boolean blnMonitor = false;
	
	Intent intent;
	
	

	private service_thread serviceThread;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onCreate() {
		super.onCreate();

		Log.i(TrainTrack.logID, "StateMon onCreate Called");

	}

	@Override
	public void onDestroy() {		
        
		Log.i(TrainTrack.logID, "StateMon onDestroy Called");
		
		stopMonitoring();
		
		super.onDestroy();
	}
	

	private int MIN_TIME_BW_UPDATES = 30000; // milliseconds
	private int MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // meters
	
	
	
	// listen for override events
	private final BroadcastReceiver myReceiverStart = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			blnOverrideActice=true;
			// is this a start or stop?
			startMonitoring();
		
		}
	};	
	
	// listen for override events
	private final BroadcastReceiver myReceiverStop = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			// is this a start or stop?
			stopMonitoring();
			blnOverrideActice=false;
		}
	};			
	
	public int onStartCommand(Intent intent, int flags, int startId) {

		// ok, start the listenter
		if (locationListiner==null)
		{
	    	locationListiner = new MyLocationListener( 50.68149, -2.22254);
			
			// ok, start the location monitor
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			Criteria criteria = new Criteria();
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(false);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			
			String strProvider = null;
			
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			String providerCoarse = locationManager.getBestProvider(criteria, true);
			
			if (providerCoarse != null)
			{
				strProvider = providerCoarse;
			}
			else
			{
				// ok, no coarse location.... 
				// so try fine
				
				
				// get the fine and coarse providers
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				String providerFine = locationManager.getBestProvider(criteria, true);
				
				if (providerFine != null) 
				{
					strProvider = providerFine;
				}
			}
			
			if ((strProvider != null)&&(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED))
			{
			
				locationManager.requestLocationUpdates( strProvider, 
					MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListiner);
				
				Log.i(TrainTrack.logID, "Location update enabled with provider \""+strProvider+"\", With "+MIN_DISTANCE_CHANGE_FOR_UPDATES);
			}
			else
			{
				Log.i(TrainTrack.logID, "No location provider found");
			}
			

			// register us as a receiver for "start" command
			registerReceiver(myReceiverStart, new IntentFilter(StateMon.START));
			registerReceiver(myReceiverStop, new IntentFilter(StateMon.STOP));

		}
		return Service.START_STICKY;
	}
	
	
	private void startMonitoring()
	{
		Log.i(TrainTrack.logID, "Starting montoring");

		blnMonitor=true;
		
		if (t==null)
		{
	
			serviceThread = new service_thread();
	
			t = new Thread(serviceThread);
			t.start();
			
		}
		else
		{
			t.interrupt();
		}
		
		
	}
	
	private void stopMonitoring()
	{
		
		
		Log.i(TrainTrack.logID, "Stopping montoring");
		
		blnMonitor=false;
		
		if (t!=null)
		{
	//		serviceThread.triggerExit();
			if (t!=null)
				t.interrupt();
		}
//		t=null;
		//serviceThread=null;
		
		
	}

	class service_thread implements Runnable
	{
		private boolean blnExit;	
		private Notification myNotification;
		private NotificationManager notificationManager;
		
		service_thread()
		{
			blnExit=false;
		}
		
		void triggerExit()
		{
			blnExit=true;
		}

		@Override
		public void run() {

			notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			
			Log.i(TrainTrack.logID, "Thread run");
			
				while (blnExit==false) {
					int intSleepTime = SLOW_UPDATE;
					
					
					if (blnMonitor)
					{
						Log.i(TrainTrack.logID, "Monitor active, polling");
						
						readWebPage();
						intSleepTime = FAST_UPDATE;
					}
					else
					{
						Log.i(TrainTrack.logID, "Monitor not active, sleeping...");
					}

					try {
					
						Thread.sleep(intSleepTime * 1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			

			
			notificationManager.cancel(MY_NOTIFICATION_ID);
		}
		
		class TrainState
		{
			private int intState;
			private int intOpenTime;
			
			public TrainState(int intNewState, int intNewOpenTime)
			{
				intState = intNewState;
				intOpenTime = intNewOpenTime;
			}
			
			public int getState()
			{
				return(intState);
			}
			
			public int getOpenTime()
			{
				return(intOpenTime);
			}
		}
		
		
		private TrainState getTrainState(String strJSON)
		{
			int intTrainState=0;
			int intOpenTime=0;
			
			InputStream is = new ByteArrayInputStream( strJSON.getBytes( ) );

			try {
				
				
				JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
				
				
				try {
					reader.beginObject();
					
					while (reader.hasNext()) 
					{
						String name = reader.nextName();
						if (name.equals("state")) {
							intTrainState = reader.nextInt();
						} else if (name.equals("opentime")) {
							intOpenTime = reader.nextInt();
						} else {
							reader.skipValue();
						}
					}

					reader.endObject();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					reader.close();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return(new TrainState(intTrainState, intOpenTime));
		}

		private int getNotificationIcon() 
		{
		    boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
		    return useWhiteIcon ? R.drawable.down : R.drawable.down;
		}
		
		

		@SuppressWarnings("deprecation")
		private void readWebPage() {

			TrainState trainState = null;
			boolean blnNewState=false;
			
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(
					"https://www.retallack.org.uk/cgi-bin/train-track.cgi");
				//	"https://78.143.212.20/cgi-bin/train-track.cgi");
			// Get the response
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String response_str = null;
			try {
				response_str = client.execute(request, responseHandler);

				trainState = getTrainState(response_str);
				
				//response_str = response_str.replaceAll("[^A-Za-z0-9]", "");
				
				Log.i(TrainTrack.logID, "New State: "+trainState.getState()+", open: "+trainState.getOpenTime());
				
				
				// ok, send the result to the app to display		
				sendResult(trainState.getState(), trainState.getOpenTime());
				
				// if the crossing is not "up"
				// meaning - closed, closing or opening....
				if (trainState.getState() != 0)
				{
					blnNewState=true;
				}
				

			} catch (Exception e) {
				e.printStackTrace();
			}
			

			if (blnNewState != blnOldState)
			{
				Log.i(TrainTrack.logID, "State changed");
				
				if (blnNewState)
				{
					Log.i(TrainTrack.logID, "New State Active");

					Context context = getApplicationContext();
					
					// we want the activity to start on selection
					Intent notificationIntent = new Intent(context, TrainTrack.class);
					
					notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					
					PendingIntent newPendingIntent = PendingIntent.getActivity(context, 0,
				            notificationIntent, 0);
					

					
					Notification.Builder mBuilder;
					
					// also set the notification
					mBuilder = new Notification.Builder(context)
					.setContentTitle("Train Detected")
			          .setContentText("Wool Level Crossing is Down")
			          .setTicker("Train Detected")
							.setVisibility(Notification.VISIBILITY_PUBLIC)
							.setSmallIcon(R.drawable.small_down)
							.setOnlyAlertOnce(true)
							.setContentIntent(newPendingIntent);
					
					try {
						Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.train);
						
			            mBuilder.setSound(soundUri);
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
					

					myNotification = mBuilder.build();
					
					//myNotification.defaults |= Notification.DEFAULT_SOUND;
					myNotification.defaults |= Notification.DEFAULT_LIGHTS;
					myNotification.defaults |= Notification.DEFAULT_VIBRATE;
					
					//myNotification.flags |= Notification.FLAG_INSISTENT;
					//myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
					//myNotification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
					myNotification.flags |= Notification.FLAG_NO_CLEAR;
					myNotification.flags |= Notification.FLAG_ONGOING_EVENT;
					
				    notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
				    
				    
				    
				}
				else
				{
					Log.i(TrainTrack.logID, "New State InActive");
					
					notificationManager.cancel(MY_NOTIFICATION_ID);
				}
			}	
			
			blnOldState = blnNewState;
		}

	}
	
	public void sendResult(int intNewState, int intOpenIn) {

		Log.i(TrainTrack.logID, "sendResult "+ intNewState);

		Intent intent = new Intent(RESULT);

		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss",Locale.UK);
		
		String strDate = dateFormat.format(new java.util.Date());
		
		intent.putExtra(MESSAGE, intNewState);
		intent.putExtra(OPENIN, intOpenIn);
		intent.putExtra(UPDATETIME, strDate);
	
		this.sendBroadcast(intent);
	}


    
    private class MyLocationListener implements LocationListener {
    	
    	double fltLat;
    	double fltLong;
    	
    	double fltPreviousLat=0;
    	double fltPreviousLong=0;
    	long previousTime=0;
    	
    	MyLocationListener(double fltNewLat, double fltNewLong)
    	{
    		fltLat = fltNewLat;
    		fltLong = fltNewLong;
    	}
    	
        public void onLocationChanged(Location location) {
        	
        	long lngCurrentTime = System.currentTimeMillis() / 1000L;
        	
    		float[] results = new float[1];
    		Location.distanceBetween(
    				fltLat,fltLong,
    				location.getLatitude(), location.getLongitude(), results);

    		long lngDistanceToCrossing = (long)results[0];
    		
    		long lngSpeedOverDistance = 0;
    		long lngDistanceSinceLastMeasurement = 0;
    		// calculate speed?
    		if (previousTime>0)
    		{
    		
	    		float[] resultDiff = new float[1];
	    		Location.distanceBetween(fltPreviousLat, fltPreviousLong,
	            		location.getLatitude(), location.getLongitude(),
	            		resultDiff);
	    		
	    		lngDistanceSinceLastMeasurement = (long)resultDiff[0];
	    		
	    		long lngDiffTime = lngCurrentTime - previousTime;
	    		
	    		if (lngDiffTime>0)
	    			lngSpeedOverDistance = (long) (lngDistanceSinceLastMeasurement/(lngDiffTime));
	    		
	    		//Log.i(TrainTrack.logID, "Distance is: " + lngDistanceSinceLastMeasurement +", Time is "+lngDiffTime+",  speed is: "+lngSpeedOverDistance);
	    		
    		}
    		

    		Log.i(TrainTrack.logID, "Distance to crossing is: " + (lngDistanceToCrossing/1000)+ " km");
    		Log.i(TrainTrack.logID, "Speed is: "+lngSpeedOverDistance+" m/s");
    		Log.i(TrainTrack.logID, "Distance traveled is: "+lngDistanceSinceLastMeasurement+" m");
    		
    		boolean blnEnable = false;
    		
    		// ok, are we close to the crossing and traveling at speed (>1 m/s)
    		if ( (lngDistanceToCrossing < 10000) && (lngSpeedOverDistance>1) )
    			blnEnable=true;
    		
    		// are we really close to the crossing 
    		if ( lngDistanceToCrossing < 300)
    			blnEnable=true;
    					
    		if (blnOverrideActice==false)
    		{
    		
	    		if (blnEnable)
	    		{
	    			Log.i(TrainTrack.logID, "Close to crossing, starting monitor");
	    			startMonitoring();
	    		}
	    		else
	    		{
	    			Log.i(TrainTrack.logID, "Not close to crossing, stopping monitor");
	    			stopMonitoring();
	    		}
    		}
    		
    		fltPreviousLat=location.getLatitude();
    		fltPreviousLong=location.getLongitude();
    		previousTime=lngCurrentTime;
        }
        public void onStatusChanged(String s, int i, Bundle bundle) {}
        public void onProviderEnabled(String s) {}
        public void onProviderDisabled(String s) {}
    }
    
    
    private MyLocationListener locationListiner;


}
