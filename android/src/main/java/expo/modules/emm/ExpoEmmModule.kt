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

class ExpoEmmModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoEmm")

    Function("hello") {
      "Hello world! 👋"
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

    Function("getInstalledPackages") { getIcon: Boolean ->
      try {
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        val items = packageManager.queryIntentActivities(intent, 0);

        var applications = emptyArray<String>()

        for(item in items) {
          val applicationInfo = packageManager.getPackageInfo(item.activityInfo.packageName, 0)

          var icon = ""

          if(getIcon) {
            val drawable: Drawable = packageManager.getApplicationIcon(item.activityInfo.packageName)

            if(drawable is BitmapDrawable) {
              val outputStream = ByteArrayOutputStream()
              drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

              icon = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
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

              icon = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            }
          }

          val application = applicationInfo.applicationInfo.loadLabel(packageManager).toString() + ";" + applicationInfo.packageName + ";" + applicationInfo.versionName + ";" + applicationInfo.longVersionCode.toString() + ";" + applicationInfo.firstInstallTime + ";" + applicationInfo.lastUpdateTime + ";" + icon

          applications = applications + application
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

    Function("requestOverlayPermission") {
      try {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

        context.startActivity(intent);
      } catch (e: Exception) {
        "not-permitted"
      }
    }
    Function("requestPackageUsageStatsPermission") {
      try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)

        context.startActivity(intent);
      } catch (e: Exception) {
        "not-permitted"
      }
    }
  }

  private val context
  get() = requireNotNull(appContext.reactContext)
}
