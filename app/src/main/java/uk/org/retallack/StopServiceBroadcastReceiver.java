package uk.org.retallack;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class StopServiceBroadcastReceiver extends BroadcastReceiver {
	
	/** 
	 * On startup, start a cal event
	 */
    public void onReceive(Context context, Intent intent) {
    
    	Log.i(TrainTrack.logID, "Stopping service");
    	
    	Intent startServiceIntent = new Intent(context, StateMon.class);
    	context.stopService(startServiceIntent);
    }
}
