package uk.org.retallack;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class TrainTrack extends Activity {
	//private Intent intent;

	public static String logID = "TrainTrack";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.train_track_layout);

	}

	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action=intent.getAction();
			
			Log.i(TrainTrack.logID, "onReceive action=\""+ action +  "\"");
			
			
			if(action.equalsIgnoreCase(StateMon.RESULT))
			{
				updateUI(intent);	
			}
			
		
		}
	};
	
	
	private void startBackService()
	{
		Intent intent = new Intent(StateMon.START);
		this.sendBroadcast(intent);
	}
	
	private void stopBackService()
	{
		Intent intent = new Intent(StateMon.STOP);
		this.sendBroadcast(intent);
	}
	
	@Override
	public void onResume() {
		super.onResume();		
		
		Log.i(TrainTrack.logID, "onResume ");
		
		ImageView image = (ImageView) findViewById(R.id.imageView1);
		TextView txtDateTime = (TextView) findViewById(R.id.txtDateTime);  
		TextView txtOpenIn = (TextView) findViewById(R.id.txtOpenIn);  
		TextView txtState  = (TextView) findViewById(R.id.txtState);  
		
		image.setVisibility(View.GONE);
		txtState.setText("");
		txtDateTime.setText("");
		txtOpenIn.setText("");
		
		
		startService(new Intent(this, StateMon.class));
		
		registerReceiver(myReceiver, new IntentFilter(StateMon.RESULT));
		
		
		startBackService();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		ImageView image = (ImageView) findViewById(R.id.imageView1);
		TextView txtDateTime = (TextView) findViewById(R.id.txtDateTime);  
		TextView txtOpenIn = (TextView) findViewById(R.id.txtOpenIn);  
		TextView txtState  = (TextView) findViewById(R.id.txtState);  
		
		timerHandler.removeCallbacks(timerRunnable);
		
		unregisterReceiver(myReceiver);
		stopBackService(); 		
		
		image.setVisibility(View.GONE);
		txtState.setText("");
		txtDateTime.setText("");
		txtOpenIn.setText("");
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		try
		{
			unregisterReceiver(myReceiver);
		}
		// capture event where we are already unregistered
		catch(java.lang.IllegalArgumentException e)
		{
			
		}
		stopBackService();
	}
	
	private int intLastOpenIn = -1;
	private long lngTimeGotLastOpen = 0;

	
    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
        	
            long millis = (intLastOpenIn*1000) - (System.currentTimeMillis() - lngTimeGotLastOpen);
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            TextView txtOpenIn = (TextView) findViewById(R.id.txtOpenIn); 
            
			txtOpenIn.setText(String.format(Locale.US, "%d:%02d", minutes, seconds));
                       
            timerHandler.postDelayed(this, 500);
        }
    };
	
	private void updateUI(Intent intent) {
		int intNewState = intent.getIntExtra(StateMon.MESSAGE, 0);
		
		int intOpenIn = intent.getIntExtra(StateMon.OPENIN, -1);
		
		String updateTime = intent.getStringExtra(StateMon.UPDATETIME);

		ImageView image = (ImageView) findViewById(R.id.imageView1);

		
		TextView txtDateTime = (TextView) findViewById(R.id.txtDateTime);  
		
		TextView txtOpenIn = (TextView) findViewById(R.id.txtOpenIn);  
		
		TextView txtState  = (TextView) findViewById(R.id.txtState);  
		
		
		txtDateTime.setText(updateTime);

	    //state_up,
	   // state_down,
	   // state_closing,
	   // state_opening,

		Log.i(TrainTrack.logID, "new state \""+ intNewState +  "\", open in: " +intOpenIn);
		
		boolean blnUpdate = false;
		
		if (intNewState==0)
		{
			image.setVisibility(View.GONE);
			Log.i(TrainTrack.logID, "Hiding");
			
			txtState.setText("Open");
		}
		else
		{
			image.setVisibility(View.VISIBLE);
			Log.i(TrainTrack.logID, "Show");
			
			if (intOpenIn>=0)
			{				
				blnUpdate=true;
			}
			
			if (intNewState==1)
			{
				txtState.setText("Closed");
			}
			else if (intNewState==2)
			{
				txtState.setText("Closing");
			}
			else if (intNewState==3)
			{
				txtState.setText("Opening");
			}
			else
			{
				txtState.setText("Unknown");
			}
		}
		
		if (blnUpdate)
		{
			// if there a new "open In" value from the server?
			if (intOpenIn != intLastOpenIn)
			{
				// and set the correct args for the update thread
				lngTimeGotLastOpen = System.currentTimeMillis();				
				intLastOpenIn = intOpenIn;
				
				timerHandler.postDelayed(timerRunnable, 0);
			}
		}
		else
		{
			// stop the timer
			timerHandler.removeCallbacks(timerRunnable);
			
			txtOpenIn.setText("");		
			intLastOpenIn=-1;
		}

	}
}
