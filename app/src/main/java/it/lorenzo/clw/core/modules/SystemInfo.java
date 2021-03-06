package it.lorenzo.clw.core.modules;

/**
 * Created by lorenzo on 17/02/15.
 */

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Formatter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.lorenzo.clw.core.Core;
import it.lorenzo.clw.core.modules.Utility.BitmapWithPosition;
import it.lorenzo.clw.core.modules.Utility.CommonUtility;

public class SystemInfo extends AbstractModule {

	private final static String cpu = "/sys/devices/system/cpu/cpu";
	private final static String cpuCurr = "/cpufreq/scaling_cur_freq";

	private final static String CPU_FREQ = "cpuFreq";

	private final static String BATTERY_PERCENT = "battery_percent";
	private final static String BATTERY_CHARGING = "battery_charging";
	private final static String BATTERY_BAR = "battery_bar";

	private final static String UPTIME = "uptime";

	private final static String MEM_FREE = "mem_free";
	private final static String MEM_USED = "mem_used";
	private final static String MEM_TOTAL = "mem_total";
	private final static String MEM_PERCENT = "mem_percent";
	private final static String MEM_BAR = "mem_bar";

	private final static String FS_FREE = "fs_free";
	private final static String FS_USED = "fs_used";
	private final static String FS_SIZE = "fs_size";
	private final static String FS_PERCENT = "fs_perc";
	private final static String FS_BAR = "fs_bar";

	private final static String SSID = "ssid";
	private final static String WIFI_IP = "wifi_ip";

	private long maxRam;

	public SystemInfo(Core core) {
		super(core);
		keys.put(CPU_FREQ, Result.string);
		keys.put(BATTERY_PERCENT, Result.string);
		keys.put(BATTERY_CHARGING, Result.string);
		keys.put(UPTIME, Result.string);
		keys.put(BATTERY_BAR, Result.draw);
		keys.put(MEM_FREE, Result.string);
		keys.put(MEM_USED, Result.string);
		keys.put(MEM_TOTAL, Result.string);
		keys.put(MEM_PERCENT, Result.string);
		keys.put(MEM_BAR, Result.draw);
		keys.put(FS_FREE, Result.string);
		keys.put(FS_USED, Result.string);
		keys.put(FS_SIZE, Result.string);
		keys.put(FS_PERCENT, Result.string);
		keys.put(FS_BAR, Result.draw);
		keys.put(SSID, Result.string);
		keys.put(WIFI_IP, Result.string);
		maxRam = -1;
	}

	@Override
	public String genString(String key, String[] params, Context context) {
		try {
			switch (key) {
				case CPU_FREQ:
					if (params.length == 2)
						return ""
								+ getCpuCurrentFreq(Integer.parseInt(params[0]),
								params[1]);
					else if (params.length == 1)
						return ""
								+ getCpuCurrentFreq(Integer.parseInt(params[0]), "");
					break;
				case BATTERY_PERCENT:
					return "" + getBatteryLevel(context);
				case BATTERY_CHARGING:
					return isConnected(context) ? "connected" : "not connected";
				case UPTIME:
					return getUptime();
				case MEM_PERCENT:
					return "" + getMemPercent(context);
				case MEM_FREE:
					return CommonUtility.convert(getFreeeMemory(context));
				case MEM_USED:
					return CommonUtility.convert(getTotalMemory() - getFreeeMemory(context));
				case MEM_TOTAL:
					return CommonUtility.convert(getTotalMemory());
				case FS_FREE:
					return CommonUtility.convert(getFreeSpace(params, context));
				case FS_USED:
					return CommonUtility.convert(getUsedSpace(params, context));
				case FS_SIZE:
					return CommonUtility.convert(getTotalSpace(params, context));
				case FS_PERCENT:
					return "" + getFsPercent(params, context);
				case SSID:
					return getSsid(context);
				case WIFI_IP:
					return this.getWiFiIp(context);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	@Override
	public void initialize(Context context) {
	}

	@Override
	public BitmapWithPosition genBmp(String key, String[] params, int maxWidth, Context context) {
		try {
			int start = 0;
			if (key.equals(FS_BAR))
				start = 1;

			int width = 0;
			int height = 0;
			if (params != null && params.length > start) {
				height = Integer.parseInt(params[start]);
			}
			if (params != null && params.length > 1 + start) {
				width = Integer.parseInt(params[1 + start]);
			}


			switch (key) {
				case BATTERY_BAR:
					return core.getBarDrawer().getBar(width, height,
							getBatteryLevel(context), maxWidth);
				case MEM_BAR:
					return core.getBarDrawer().getBar(width, height,
							getMemPercent(context), maxWidth);
				case FS_BAR:
					return core.getBarDrawer().getBar(width, height,
							getFsPercent(params, context), maxWidth);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void changeSetting2(String key, String[] params, Context context) {
	}

	@Override
	public void setDefaults(String key, String[] params, Context context) {
	}

	@Override
	protected void finalize(Context context) {
	}

//	-----------------------------------------------------------------------

	private int getBatteryLevel(Context context) {
		Intent batteryIntent = context
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if (level == -1 || scale == -1) {
			return (int) 50.0f;
		}
		return (int) (((float) level / (float) scale) * 100.0f);
	}

	private boolean isConnected(Context context) {
		Intent intent = context.getApplicationContext()
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
	}

	private String getUptime() {
		Long mill = SystemClock.elapsedRealtime();
		int minutes = (int) ((mill / (1000 * 60)) % 60);
		int hours = (int) ((mill / (1000 * 60 * 60)) % 24);
		int days = (int) (mill / (1000 * 60 * 60 * 24));
		String res = "";
		if (days > 0)
			res += days + "days";
		res += hours + "h";
		res += minutes + "m";
		return res;
	}

	public String getCpuCurrentFreq(int cpuNumber, String f) {
		float freq = CommonUtility.readSystemFileAsInt(cpu + cpuNumber + cpuCurr);
		switch (f) {
			case "g":
				return "" + freq / 1000000;
			case "m":
				return "" + Math.round(freq / 1000);
			default:
				return "" + Math.round(freq);
		}
	}


	// RAM --------------------------------------------------------------------

	private int getMemPercent(Context context) {
		return 100 - (int) (100 * getFreeeMemory(context) / getTotalMemory());
	}

	private long getFreeeMemory(Context context) {
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		return mi.availMem;
	}

	private long getTotalMemory() {

		if (maxRam < 0) {
			RandomAccessFile reader;
			String load;
			try {
				reader = new RandomAccessFile("/proc/meminfo", "r");
				load = reader.readLine();
				// Get the Number value from the string
				Pattern p = Pattern.compile("(\\d+)");
				Matcher m = p.matcher(load);
				String value = "";
				while (m.find()) {
					value = m.group(1);
					// System.out.println("Ram : " + value);
				}
				reader.close();
				maxRam = Long.parseLong(value) * 1024;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return maxRam;
	}


	// STORAGE -----------------------------------------------------------------

	private String getInternalStorage(Context context) {
		File storage;
		storage = context.getFilesDir();
		if (storage.exists())
			return storage.getAbsolutePath();
		else
			storage = Environment.getExternalStorageDirectory();
		return storage.getAbsolutePath();
	}

	private long getTotalSpace(String[] params, Context context) {
		String path;
		if (params != null && params.length > 0) {
			path = params[0];
		} else
			path = getInternalStorage(context);

		StatFs statFs = new StatFs(path);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
		} else {
			return (long) statFs.getBlockCount() * (long) statFs.getBlockSize();
		}
	}

	private long getFreeSpace(String[] params, Context context) {
		String path;
		if (params != null && params.length > 0) {
			path = params[0];
		} else
			path = getInternalStorage(context);
		StatFs statFs = new StatFs(path);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return statFs.getAvailableBlocksLong()
					* statFs.getBlockSizeLong();
		} else {
			return (long) statFs.getAvailableBlocks()
					* (long) statFs.getBlockSize();
		}
	}

	private long getUsedSpace(String[] params, Context context) {
		return getTotalSpace(params, context) - getFreeSpace(params, context);
	}

	private int getFsPercent(String[] params, Context context) {
		return 100 - (int) (100 * getFreeSpace(params, context) / getTotalSpace(params, context));
	}

	// WIFI --------------------------------------------------------------------

	private String getSsid(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
			if (networkInfo.isConnected()) {
				final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
				if (connectionInfo != null
						&& !TextUtils.isEmpty(connectionInfo.getSSID())) {
					String ssid;
					ssid = connectionInfo.getSSID();
					return ssid.substring(1, ssid.length() - 1);
				}
			}
		return "";
	}

	private String getWiFiIp(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		String ip;

		if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null
					&& !TextUtils.isEmpty(connectionInfo.getSSID())) {
				ip = Formatter.formatIpAddress(connectionInfo.getIpAddress());
				return ip;
			}
		}
		return "";
	}
}
