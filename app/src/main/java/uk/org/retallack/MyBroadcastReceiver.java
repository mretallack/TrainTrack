package uk.org.retallack;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
	
	private final String SERVICE_STATE = "serviceState";

	
	/** 
	 * On startup, start a cal event
	 */
    public void onReceive(Context context, Intent intent) {
    
    	if (intent.getAction()=="android.intent.action.BOOT_COMPLETED")
    	{
	    	// start the service
			// ok, we are close
			Log.i(TrainTrack.logID, "Starting service");
			// so start the service 
	    	Intent startServiceIntent = new Intent(context, StateMon.class);
	    	context.startService(startServiceIntent);
    	}
    	
    }
    

    
    public void addStopTime(Context context, int intRequestCode, int intHour, int intMinute, int intSecond)
    {
    	SetAlarm(context, intRequestCode, intHour,intMinute,0, StopServiceBroadcastReceiver.class);
    }
    
    public void addStartTime(Context context, int intRequestCode, int intHour, int intMinute, int intSecond)
    {
    	SetAlarm(context, intRequestCode, intHour,intMinute,0, StartServiceBroadcastReceiver.class);
    }
    
    
    public void SetAlarm(Context context, int intRequestCode, int intHour, int intMinute, int intSecond, Class<?> cls) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        
        
        Calendar cal = new GregorianCalendar();
        
        cal.add(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR));
        cal.set(Calendar.HOUR_OF_DAY, intHour);
        cal.set(Calendar.MINUTE, intMinute);
        cal.set(Calendar.SECOND, intSecond);
        cal.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND));
        cal.set(Calendar.DATE, calendar.get(Calendar.DATE));
        cal.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
        
        
        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, cls);
        intent.putExtra(SERVICE_STATE, true);
        PendingIntent pi = PendingIntent.getBroadcast(context, intRequestCode, intent, 0);
        // After after 30 seconds

        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }


    
    
}
