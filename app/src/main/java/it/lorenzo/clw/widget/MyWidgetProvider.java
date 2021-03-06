package it.lorenzo.clw.widget;

/**
 * Created by lorenzo on 17/02/15.
 */

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.io.PrintWriter;
import java.io.StringWriter;

import it.lorenzo.clw.R;
import it.lorenzo.clw.chooser.FileSelect;
import it.lorenzo.clw.core.Core;

import static android.app.PendingIntent.getBroadcast;
import static it.lorenzo.clw.R.id.button1;


public class MyWidgetProvider extends AppWidgetProvider {

	public static String getStackTrace(final Throwable throwable) {
		if (throwable == null)
			return "";
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		createAlarm(context);
	}

	public void createAlarm(Context context) {
		SharedPreferences sharedPref = context.getSharedPreferences(
				context.getString(R.string.preference),
				Context.MODE_PRIVATE);
		Boolean update = sharedPref.getBoolean("alarm_key", false);
		int intervall = sharedPref.getInt("intervall_key", 30);
		if (update) {
			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, MyWidgetProvider.class);
			intent.setAction("it.lorenzo.clw.intent.action.ALARM");
			PendingIntent pi = getBroadcast(context, 0, intent, 0);
			am.setRepeating(AlarmManager.RTC,
					System.currentTimeMillis() + 1000 * intervall, 1000 * intervall, pi);
		}
	}

	public void removeAlarm(Context context) {
		Intent intent = new Intent(context, MyWidgetProvider.class);
		intent.setAction("it.lorenzo.clw.intent.action.ALARM");
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent sender =
				getBroadcast(context, 0, intent, 0);

		alarmManager.cancel(sender);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		removeAlarm(context);
		SharedPreferences sharedPref = context.getSharedPreferences(
				context.getString(R.string.preference), Context.MODE_PRIVATE);
		for (int i : appWidgetIds)
			sharedPref.edit().remove("" + i).apply();
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		context.getSharedPreferences(context.getString(R.string.preference),
				Context.MODE_PRIVATE).edit().clear().apply();

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		String act = "" + intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(act)
				|| act.equals("it.lorenzo.clw.intent.action.SETTINGS_CHANGED")) {
			removeAlarm(context);
			createAlarm(context);
		} else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(act)
				|| act.equals("it.lorenzo.clw.intent.action.ALARM")) {

			AppWidgetManager man = AppWidgetManager.getInstance(context);
			int[] ids = man.getAppWidgetIds(
					new ComponentName(context, MyWidgetProvider.class));
			onUpdate(context, AppWidgetManager.getInstance(context), ids);
		}else
		{
			int appWidgetIds[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));
			if (appWidgetIds != null && appWidgetIds.length > 0) {
				this.onUpdate(context,
						AppWidgetManager.getInstance(context), appWidgetIds);
			}
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
						 int[] appWidgetIds) {
		for (int n : appWidgetIds) {
			// get path for config file
			SharedPreferences sharedPref = context.getSharedPreferences(
					context.getString(R.string.preference),
					Context.MODE_PRIVATE);
			String path = sharedPref.getString("" + n, "");
			Boolean notification = sharedPref.getBoolean("use_notification_key", false);
			if (path.equals("")) {
				// path is not set
				RemoteViews remoteViews = new RemoteViews(
						context.getPackageName(), R.layout.clickme);
				Intent intent = new Intent(context, FileSelect.class);
				intent.putExtra("appWidgetId", n);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				PendingIntent pendIntent = PendingIntent.getActivity(context,
						0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				remoteViews.setOnClickPendingIntent(button1, pendIntent);
				remoteViews.setTextViewText(button1, "No configuration file set");
				appWidgetManager.updateAppWidget(n, remoteViews);
			} else {
				// path is set
				RemoteViews remoteViews;
				try {
					remoteViews = new RemoteViews(
							context.getPackageName(), R.layout.widgetlayout);

					remoteViews.setImageViewBitmap(R.id.widget_image,
							new Core().getImageToSet(context.getApplicationContext(), path));
					Intent intent = new Intent();
					intent.setAction("it.lorenzo.clw.intent.action.CHANGE_PICTURE");
					int ids[] = new int[1];
					ids[0] = n;
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

					remoteViews.setOnClickPendingIntent(R.id.widget_image,
							getBroadcast(context, 0, intent,
									PendingIntent.FLAG_UPDATE_CURRENT));

					appWidgetManager.updateAppWidget(n, remoteViews);
				} catch (Exception e) {
					remoteViews = new RemoteViews(
							context.getPackageName(), R.layout.clickme);

					String text = e.getMessage() + "\n" +
							(e.getCause() != null ? e.getCause().getMessage() : "" + "\n") +
							getStackTrace(e);

					if (notification)
						sendNotification(context, text);

					remoteViews.setTextViewText(button1, text);
					Intent intent = new Intent();
					intent.setAction("it.lorenzo.clw.intent.action.CHANGE_PICTURE");
					int ids[] = new int[1];
					ids[0] = n;
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
					remoteViews.setOnClickPendingIntent(button1,
							getBroadcast(context, 0, intent,
									PendingIntent.FLAG_UPDATE_CURRENT));

					appWidgetManager.updateAppWidget(n, remoteViews);
				}
			}
		}
	}

	private void sendNotification(Context context, String text) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.notification)
				.setContentTitle("CLW ERROR").setContentText(text);
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(5, mBuilder.build());
	}
}
