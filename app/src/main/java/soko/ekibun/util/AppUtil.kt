package soko.ekibun.util

import android.content.Context
import android.graphics.Point
import android.util.Size
import android.view.WindowManager

object AppUtil{
    fun getAppVersion(context: Context): Int {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return info.versionCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1
    }

    fun getScreenSize(context: Context): Size{
        val p = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(p)
        return Size(Math.min(p.x, p.y), Math.max(p.x, p.y))
    }
}