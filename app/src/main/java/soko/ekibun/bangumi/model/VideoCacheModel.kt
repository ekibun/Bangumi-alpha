package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import com.google.android.exoplayer2.offline.Downloader
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.offline.ProgressiveDownloader
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.VideoCache
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil
import java.util.HashMap

class VideoCacheModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    private val videoCache = "videoCache"
    fun getVideoCacheList(): Map<String, VideoCache>{
        return JsonUtil.toEntity(sp.getString(videoCache, JsonUtil.toJson(HashMap<String, VideoCache>())),
                object : TypeToken<Map<String, VideoCache>>() {}.type)
    }

    fun getBangumiVideoCacheList(bangumi: Bangumi): VideoCache?{
        return  getVideoCacheList()[bangumi.parseString()]
    }

    fun getCache(video: VideoInfo): VideoCache.VideoCacheBean? {
        return getCache(video, video.bangumi.parseString())
    }

    fun getCache(video: VideoInfo, bangumi: String): VideoCache.VideoCacheBean? {
        return getVideoCacheList()[bangumi]?.videoList?.get(video.id)
    }

    fun addVideoCache(video: VideoInfo, url: String){
        val editor = sp.edit()
        val set = HashMap<String, VideoCache>()
        set += getVideoCacheList()
        set[video.bangumi.parseString()] = VideoCache(video.bangumi, (set[video.bangumi.parseString()]?.videoList?: HashMap()).plus(
                Pair(video.id, VideoCache.VideoCacheBean(video, url))
        ))
        editor.putString(videoCache, JsonUtil.toJson(set))
        editor.apply()
    }

    fun removeVideoCache(video: VideoInfo){
        val editor = sp.edit()
        val set = HashMap<String, VideoCache>()
        set += getVideoCacheList()
        set[video.bangumi.parseString()] = VideoCache(video.bangumi, (set[video.bangumi.parseString()]?.videoList?: HashMap()).minus(video.id))
        set[video.bangumi.parseString()]?.let{
            if(it.videoList.isEmpty()) set.remove(video.bangumi.parseString())
        }
        editor.putString(videoCache, JsonUtil.toJson(set))
        editor.apply()
    }

    fun getDownloader(video: VideoInfo): Downloader?{
        getCache(video)?.url?.let{ return getDownloader(it) }
        return null
    }

    val factory by lazy{ DefaultHttpDataSourceFactory("exoplayer") }
    private val cache by lazy{ SimpleCache(LoaderUtil.getDiskCacheDir(context, "video"), NoOpCacheEvictor()) }
    fun getCacheDataSourceFactory(url: String): CacheDataSourceFactory{
        factory.defaultRequestProperties.set("referer", url)
        return CacheDataSourceFactory(cache, factory)
    }
    fun getDownloader(url: String): Downloader {
        val dataSourceFactory =getCacheDataSourceFactory(url)
        val helper = DownloaderConstructorHelper(cache, dataSourceFactory)
        val downloader = if (url.contains("m3u8")) {
            HlsDownloader(Uri.parse(url), helper)
        } else {
            ProgressiveDownloader(url, url, helper)
        }
        downloader.init()
        return downloader
    }

    companion object {
        fun isFinished(downloadPercentage: Float): Boolean{
            return Math.abs(downloadPercentage - 100f) < 0.001f
        }
    }
}