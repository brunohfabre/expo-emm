package expo.modules.emm

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.net.Uri
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.ImageDecoder
import android.graphics.BitmapFactory
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import java.util.SortedMap
import java.util.TreeMap
import android.app.WallpaperManager
import android.provider.Settings
import android.app.AppOpsManager
import android.os.Process
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.app.Activity
import java.util.Calendar
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.provider.CallLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import android.util.Log

private fun drawableToBitmap(drawable: Drawable): Bitmap {
  if (drawable is BitmapDrawable) {
    return drawable.bitmap
  } else {
    val width = max(drawable.intrinsicWidth.toDouble(), 1.0).toInt()
    val height = max(drawable.intrinsicHeight.toDouble(), 1.0).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap
  }
}

private fun saveIconToFile(iconBitmap: Bitmap, fileName: String, context: Context): String? {
  val cacheDir = File(context.cacheDir, "icons")
  if (!cacheDir.exists() && !cacheDir.mkdirs()) {
    Log.e("AppUtils", "Failed to create directory for icons")
    return null
  }
  val iconFile = File(cacheDir, "$fileName.png")
  try {
    FileOutputStream(iconFile).use { fos ->
      iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      fos.flush()
      return iconFile.absolutePath
    }
  } catch (e: IOException) {
    Log.e("AppUtils", "Error saving icon to file", e)
    return null
  }
}

class ExpoEmmModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoEmm")

    Function("openedByDpc") {
      try {
        val activity = appContext.activityProvider?.currentActivity

        if(activity != null) {
          activity.intent.getSerializableExtra("com.google.android.apps.work.clouddpc.EXTRA_LAUNCHED_AS_SETUP_ACTION")
        } else {
          "not-permitted"
        }
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getEnrollmentSpecificId") {
      val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

      try {
        devicePolicyManager.getEnrollmentSpecificId()
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getImei") { index: Int ->
      try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        telephonyManager.getImei(index)
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getSerialNumber") {
      try {
        Build.getSerial()
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getDeviceId") {
      try {
        val uri = Uri.parse("content://com.google.android.gsf.gservices")

        val query = context.contentResolver.query(uri, null, null, arrayOf<String>("android_id"), null)

        if (query == null) {
          return@Function "not-found"
        }

        if (!query.moveToFirst() || query.columnCount < 2) {
          return@Function "not-found"
        }

        val toHexString = java.lang.Long.toHexString(query.getString(1).toLong())

        query.close()

        return@Function toHexString
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getInstalledPackages") { withIcon: Boolean ->
      try {
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        val allApps = packageManager.queryIntentActivities(intent, 0);

        val applications = ArrayList<Map<String, String?>>()

        for(item in allApps) {
          val packageInfo = packageManager.getPackageInfo(item.activityInfo.packageName, 0)

          var icon: String? = ""

          if(withIcon) {
            val drawable: Drawable = packageManager.getApplicationIcon(item.activityInfo.packageName)

            if(drawable is BitmapDrawable) {
              val outputStream = ByteArrayOutputStream()
              drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

              icon = "data:image/jpeg;base64,${Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)}"
            } else if (drawable is AdaptiveIconDrawable) {
              val backgroundDr = drawable.background
              val foregroundDr = drawable.foreground

              val drr = arrayOfNulls<Drawable>(2)
              drr[0] = backgroundDr
              drr[1] = foregroundDr

              val layerDrawable = LayerDrawable(drr)

              val width = layerDrawable.intrinsicWidth
              val height = layerDrawable.intrinsicHeight

              val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

              val canvas = Canvas(bitmap)

              layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
              layerDrawable.draw(canvas)

              val outputStream = ByteArrayOutputStream()
              bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

              icon = "data:image/jpeg;base64,${Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)}"
            }

            if(icon == "") {
              val iconBitmap = drawableToBitmap(item.loadIcon(packageManager))

              icon = saveIconToFile(iconBitmap, item.activityInfo.packageName, context)
            }
          }

          applications.add(
            mutableMapOf(
              "label" to item.loadLabel(packageManager).toString(), 
              "packageName" to item.activityInfo.packageName,
              "versionName" to packageInfo.versionName,
              "versionCode" to packageInfo.getLongVersionCode().toString(),
              "icon" to icon,
            )
          )
        }

        applications
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("launchApplication") { packageName: String ->
      try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
          context.startActivity(launchIntent);
        } else {
          "not-permitted"
        }
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("goToHome") {
      try {
        val startMain = Intent(Intent.ACTION_MAIN)

        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(startMain)
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getCurrentApp") {
      try {
        var currentApp = ""

        val usageStatsManager = context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager

        val time = System.currentTimeMillis()

        val appList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 1000,
            time
        )

        if (appList.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (!mySortedMap.isEmpty()) {
                currentApp = mySortedMap[mySortedMap.lastKey()]!!.packageName
            }
        }

        currentApp
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("setWallpaper") { uri: String ->
      try {
        val context: Context = context
        val wallpaperManager = WallpaperManager.getInstance(context)
        val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
        val bitmap = BitmapFactory.decodeStream(inputStream)

        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("verifyOverlayPermission") {
      try {
        if(!Settings.canDrawOverlays(context)) {
          false
        } else {
          true
        }
      } catch (e: Exception) {
        "not-permitted"
      }
    }
    Function("verifyPackageUsageStatsPermission") {
      try {
        val appOps = context.getSystemService(Service.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
        val granted = mode == AppOpsManager.MODE_ALLOWED

        granted
      } catch (e: Exception) {
        "not-permitted"
      }
    }
    Function("verifyUnknownSourcesPermission") {
      try {
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.packageManager.canRequestPackageInstalls();
        } else {
          Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
        }

        allowed
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("requestOverlayPermission") {
      try {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        val uri = Uri.fromParts("package", context.packageName, null)

        intent.setData(uri)

        context.startActivity(intent)
      } catch (e: Exception) {
        "not-permitted"
      }
    }
    Function("requestPackageUsageStatsPermission") {
      try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)

        intent.setData(uri)

        context.startActivity(intent)
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getNetworkInfo") {
      try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val subscriptionManager = context.getSystemService(Service.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        val sis = subscriptionManager.getActiveSubscriptionInfoList()

        val mobile = mutableListOf<Map<String, String?>>()

        if(sis !== null) {
          sis.forEach { item ->
            mobile.add(
              mutableMapOf(
                "iccid" to item.iccId.toString(),
                "slot" to item.simSlotIndex.toString(),
                "mcc" to item.getMccString(),
                "mnc" to item.getMncString(),
              )
            )
          }
        }
            
        mapOf(
          "mobile" to mobile,
          "wifi" to ""
        )
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("sendActivityResultOk") {
      try {
        val activity = appContext.activityProvider?.currentActivity

        if(activity != null) {
          val intent = Intent()
          
          activity.setResult(Activity.RESULT_OK, intent)
          
          activity.finish()
        } else {
          "not-permitted"
        }
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getAppUsages") { packages: Array<String> ->
      try {
        val usageStatsManager = context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager

        val startOfDay = Calendar.getInstance()
        startOfDay.set(Calendar.DAY_OF_MONTH, 0)

        val date = Calendar.getInstance()

        val usageStatsMap: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(0, date.timeInMillis)

        val usages = ArrayList<Map<String, String>>()

        for(item in packages) {
          usages.add(
            mutableMapOf(
              "packageName" to item, 
              "time" to usageStatsMap[item]?.totalTimeInForeground.toString()
            )
          )
        }

        usages
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getNetworkStats") { packages: Array<String> ->
      try {
        val networkStatsManager = context.getSystemService(Service.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val usages = ArrayList<Map<String, String>>()

        for(item in packages) {
          val info = context.packageManager.getApplicationInfo(item, 0)
          val uid = info.uid

          val packageWifi = networkStatsManager.queryDetailsForUid(
            NetworkCapabilities.TRANSPORT_WIFI,
            null,
            0,
            Calendar.getInstance().timeInMillis,
            uid
          )

          val bucketWifi = NetworkStats.Bucket()

          var receivedWifi = 0.0
          var sentWifi = 0.0

          while (packageWifi.hasNextBucket()) {
            packageWifi.getNextBucket(bucketWifi)

            receivedWifi += bucketWifi.rxBytes
            sentWifi += bucketWifi.txBytes
          }

          val totalWifi = ((sentWifi + receivedWifi) / 1024 / 1024)

          val packageMobile = networkStatsManager.queryDetailsForUid(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            null,
            0,
            Calendar.getInstance().timeInMillis,
            uid
          )

          val bucketMobile = NetworkStats.Bucket()

          var receivedMobile = 0.0
          var sentMobile = 0.0

          while (packageMobile.hasNextBucket()) {
            packageMobile.getNextBucket(bucketMobile)

            receivedMobile += bucketMobile.rxBytes
            sentMobile += bucketMobile.txBytes
          }

          val totalMobile = ((sentMobile + receivedMobile) / 1024 / 1024)

          usages.add(
            mutableMapOf(
              "packageName" to item, 
              "wifi" to totalWifi.toString(),
              "mobile" to totalMobile.toString()
            )
          )
        }

        usages
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getCallLog") {
      try {
        val cursor = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, android.provider.CallLog.Calls.DATE + " DESC")

        val calls = ArrayList<Map<String, String>>()

        if (cursor != null) {
          while (cursor.moveToNext()) {
            val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val number = cursor.getString(numberIndex)

            val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)

            val type = when(cursor.getString(typeIndex).toInt()) {
              CallLog.Calls.INCOMING_TYPE -> "incoming"
              CallLog.Calls.OUTGOING_TYPE -> "outgoing"
              CallLog.Calls.MISSED_TYPE -> "missed"
              CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
              CallLog.Calls.REJECTED_TYPE -> "rejected"
              CallLog.Calls.BLOCKED_TYPE -> "blocked"
              CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "answered-externally"
              else -> "unknown"
            }

            val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
            val date = cursor.getString(dateIndex)

            val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val duration = cursor.getString(durationIndex)

            calls.add(
              mutableMapOf(
                "number" to number,
                "type" to type,
                "date" to date,
                "duration" to duration,
              )
            )
          }
        }
      
        calls
      } catch (e: Exception) {
        "not-permitted"
      }
    }

    Function("getIntentParam") {
      try {
        val activity = appContext.activityProvider?.currentActivity

        if(activity != null) {
          activity.intent.getStringExtra("param").toString()
        } else {
          "not-permitted"
        }
      } catch (e: Exception) {
        "not-permitted"
      }
    }
  }

  private val context
  get() = requireNotNull(appContext.reactContext)
}
