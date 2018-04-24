package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.util.LoaderUtil
import java.util.concurrent.Executors

class App: Application() {
    private val cacheLoaders = HashMap<String, LoaderUtil>()
    private val videoCacheModel by lazy { VideoCacheModel(this) }

    fun flushCache() {
        cacheLoaders.forEach { it.value.flushCache() }
    }

    fun getLoader(flag: Int, uniName: String):LoaderUtil{
        cacheLoaders[uniName] = cacheLoaders[uniName]?: LoaderUtil(flag, this, uniName)
        return cacheLoaders[uniName]!!
    }

    companion object {

        fun getImageLoader(context: Context): LoaderUtil{
            return getLoader(context, LoaderUtil.FLAG_USE_CACHE, "image")
        }
        fun getLoader(context: Context, flag: Int, uniName: String): LoaderUtil {
            val app = context.applicationContext as App
            return app.getLoader(flag, uniName)
        }
        fun getVideoCacheModel(context: Context): VideoCacheModel{
            val app = context.applicationContext as App
            return app.videoCacheModel
        }
        fun flushCache(context: Context) {
            val app = context.applicationContext as App
            app.flushCache()
        }
    }
}