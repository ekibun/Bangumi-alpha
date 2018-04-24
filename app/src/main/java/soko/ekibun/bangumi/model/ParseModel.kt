package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.parser.*
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.view.BackgroundWebView
import soko.ekibun.util.LoaderUtil

class ParseModel(context: Context){
    val webView: BackgroundWebView by lazy{ BackgroundWebView(context) }
    val sharedPreferences: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private val parsers = arrayOf(
            IqiyiParser,
            YoukuParser,
            TxVideoParser,
            PptvParser,
            DilidiliParser
    )

    /*
    val cacheLoader by lazy{
        App.getLoader(context, LoaderUtil.FLAG_USE_CACHE or LoaderUtil.FLAG_ALWAYS_REQUEST, "parse") }*/
    val loader by lazy{ LoaderUtil() }

    fun search(key: String = "", siteId: Parser.SiteId = Parser.SiteId.ALL, callback: (List<Bangumi>?, String, Parser.LoadStatus)->Unit){
        var count = 0
        count = enumParser(siteId){
            it.search(this, key){bangumi: List<Bangumi>?, s: String, status: Parser.LoadStatus ->
                count--
                callback(bangumi, s, if(status == Parser.LoadStatus.SUCCESS && count == 0) Parser.LoadStatus.COMPLETE else status)
            } }
    }

    fun getInfo(bangumi: Bangumi, callback: (Bangumi?, Parser.LoadStatus, LoaderUtil.LoadType) -> Unit){
        enumParser(bangumi.siteId){
            it.getInfo(this, bangumi, callback) }
    }

    fun getVideoList(bangumi: Bangumi, page: Int, size: Int, callback: (List<VideoInfo>?, Int, Parser.LoadStatus) -> Unit){
        enumParser(bangumi.siteId){
            it.getVideoList(this, bangumi, page, size, callback) }
    }

    fun getVideo(avBean: VideoInfo, callback: (String, Parser.LoadStatus) -> Unit){
        enumParser(avBean.bangumi.siteId){
            it.getVideo(this, avBean, callback) }
    }

    /*
    fun getDanmaku(avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, Parser.LoadStatus) -> Unit){
        enumParser(avBean.bangumi.siteId){
            it.getDanmaku(this, avBean, callback) }
    }*/

    fun getDanmakuKey(avBean: VideoInfo, callback: (String?, Parser.LoadStatus) -> Unit){
        enumParser(avBean.bangumi.siteId){
            it.getDanmakuKey(this, avBean, callback) }
    }

    fun getDanmaku(avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit){
        enumParser(avBean.bangumi.siteId){
            it.getDanmaku(this, avBean, key, pos, callback) }
    }

    private fun enumParser(siteId: Parser.SiteId, callback: (Parser)-> Unit): Int{
        var count = 0
        parsers.filter { siteId == Parser.SiteId.ALL || it.siteId == siteId }.forEach{
            count++
            callback(it)
        }
        return count
    }
}