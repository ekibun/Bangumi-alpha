package soko.ekibun.bangumi.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil

object TxVideoParser: Parser{
    override val siteId: Parser.SiteId = Parser.SiteId.TXVIDEO

    override fun search(parseModel: ParseModel, key: String, callback: (List<Bangumi>?, String, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<Bangumi>? = ArrayList()
        var status: Parser.LoadStatus
        parseModel.loader.get("http://m.v.qq.com/search.html?keyWord=${java.net.URLEncoder.encode(key, "utf-8")}")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null){
                status = Parser.LoadStatus.FAILURE
                ret = null
            }else try {
                val doc = Jsoup.parse(String(bytes))
                val infoList = doc.select(".search_item")
                infoList.filter { it.selectFirst(".mask_scroe") != null && it.selectFirst(".figure_source")?.text()?.contains("腾讯") ?: false }
                        .forEach {
                            try {
                                val genre = it.selectFirst(".figure_genre").text()
                                var title = it.selectFirst(".figure_title").text()
                                val region = Regex("""\[([^]]+)] """).find(title)?.groupValues?.get(1) ?: ""
                                if (!genre.contains("动画") && !region.contains("动漫"))
                                    return@forEach
                                val categories = it.selectFirst(".figure_genre").text().split('|').map { it.trim() }
                                val url = it.selectFirst(".figure").attr("href")
                                val vid = Regex("""/([^/.]+).html""").find(url)?.groupValues?.get(1) ?: url
                                val mask = it.selectFirst(".mask_txt")?.toString() ?: ""
                                val newOrder = Regex("""([0-9]+)集""").find(mask)?.groupValues?.get(1)?.toInt() ?: 1
                                val count = if (mask.contains("更新")) 0 else newOrder
                                title = title.substringAfter(" ")
                                val bean = Bangumi(
                                        vid,
                                        siteId,
                                        title,
                                        it.selectFirst(".mask_scroe").text().toFloat(),
                                        it.selectFirst(".figure_pic").selectFirst("img").attr("src"),
                                        "https://v.qq.com/detail/5/$vid.html",
                                        newOrder,
                                        count,
                                        region,
                                        categories,
                                        "",
                                        0)
                                Log.v("search", bean.toString())
                                ret!! += bean
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                status = Parser.LoadStatus.SUCCESS

            } catch (e: Exception) {
                e.printStackTrace()
                ret = null
                status = Parser.LoadStatus.ERROR
            }
            callback(ret, key, status)
        }
    }

    override fun getInfo(parseModel: ParseModel, bangumi: Bangumi, callback: (Bangumi?, Parser.LoadStatus, LoaderUtil.LoadType) -> Unit) {
        var info: Bangumi? = null
        var status: Parser.LoadStatus
        parseModel.loader.get("https://v.qq.com/detail/5/${bangumi.id}.html") { bytes: ByteArray?, _, type ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
            } else try {
                val doc = Jsoup.parse(String(bytes))
                val container = doc.selectFirst(".container_inner")
                val score = container.selectFirst(".score_v")?.selectFirst(".score")?.text()?.toFloatOrNull()?:0f
                val title = container.selectFirst(".video_title_collect").selectFirst("a").text()
                val imgUrl = "http:" + container.selectFirst("img").attr("src")
                val categories = container.selectFirst(".tag_list")?.select("a")?.map { it.text() }?:ArrayList<String>()
                var region = ""
                var strategy = ""
                var videoCount = 0
                val episode = doc.selectFirst(".mod_row_episode")
                val tabs = episode.selectFirst(".mod_episode_tabs")
                val newOrder =
                        if(tabs == null){
                            episode.selectFirst(".mod_episode")?.select(".item")?.last()?.text()?.toIntOrNull()?:0
                        }else{
                            Regex("""-([0-9]+)""").find(tabs.selectFirst("._tabsNav").select("a").last().attr("data-range"))?.groupValues?.get(1)?.toInt()?:0
                        }
                container.select(".type_item")?.forEach {
                    val typetit = it.selectFirst(".type_tit")?.text()?:""
                    val value = it.selectFirst(".type_txt")?.text()?:""
                    when(typetit){
                        "地　区:"-> region = value
                        "总集数:"-> videoCount = value.toIntOrNull()?:0
                        "更新时间:" -> strategy = value.split("，")[0]
                    }
                }

                info = Bangumi(bangumi.id,
                        siteId,
                        title,
                        score,
                        imgUrl,
                        "https://v.qq.com/detail/5/${bangumi.id}.html",
                        newOrder,
                        videoCount,
                        region,
                        categories,
                        strategy,
                        0)
                Log.v("info", info.toString())
                status = Parser.LoadStatus.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                status = Parser.LoadStatus.ERROR
            }
            callback(info, status, type)
        }
    }

    override fun getVideoList(parseModel: ParseModel, bangumi: Bangumi, page: Int, size: Int, callback: (List<VideoInfo>?, Int, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<VideoInfo>? = ArrayList()
        var status: Parser.LoadStatus
        parseModel.loader.get("https://s.video.qq.com/get_playsource?id=${bangumi.id}&type=4&range=${(page-1)*size+1}-${page*size+1}&otype=json") { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                ret = null
                status = Parser.LoadStatus.FAILURE
            } else try {
                var json = String(bytes)
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
                val jo = JsonUtil.toJsonObject(json).getAsJsonObject("PlaylistItem")

                val infoList = jo.getAsJsonArray("videoPlayList")
                infoList.map { it.asJsonObject }
                        .forEach {
                            ret!! += VideoInfo(
                                    it.get("id").asString,
                                    bangumi,
                                    it.get("episode_number").asInt,
                                    it.get("title").asString,
                                    it.get("playUrl").asString,
                                    0)
                        }
                status = Parser.LoadStatus.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                ret = null
                status = Parser.LoadStatus.ERROR
            }
            callback(ret, page, status)
        }
    }

    override fun getVideo(parseModel: ParseModel, avBean: VideoInfo, callback: (String, Parser.LoadStatus) -> Unit) {
        parseModel.webView.onCatchVideo={
            Log.v("video", it.url.toString())
            if(it.url.toString().contains("/404.mp4")){
                callback(it.url.toString(), Parser.LoadStatus.FAILURE)
            }else{
                callback(it.url.toString(), Parser.LoadStatus.SUCCESS)
            }
            parseModel.webView.onCatchVideo={}
        }
        var url = parseModel.sharedPreferences.getString("api_tencent", "")
        if(url.isEmpty())
            url = "http://jx.myxit.cn/000o/?url="
        if(url.endsWith("="))
            url += "http://v.qq.com/x/cover/${avBean.bangumi.id}/${avBean.id}.html"

        val map = HashMap<String, String>()
        map["referer"]=url
        parseModel.webView.loadUrl(url, map)

        //"http://jiexi.071811.cc/jx2.php?url=http://v.qq.com/x/cover/${avBean.albumId}/${avBean.id}.html"
    }

    /*
    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, Parser.LoadStatus) -> Unit) {
        if (avBean.duration == 0)
            parseModel.loader.get("http://vv.video.qq.com/getinfo?vids=${avBean.id}") { bytes: ByteArray?, _, _ ->
                if (bytes == null) {
                    callback(null, Parser.LoadStatus.FAILURE)
                } else try {
                    val doc = Jsoup.parse(String(bytes))
                    avBean.duration = (doc.selectFirst("tm").text().toLong() / 1000000L).toInt()
                    getDanmaku(parseModel, avBean, callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null, Parser.LoadStatus.FAILURE)
                }
            } else {
            parseModel.loader.get("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${avBean.id}") { bytes: ByteArray?, _, _ ->
                if (bytes == null) {
                    callback(null, Parser.LoadStatus.FAILURE)
                } else try {
                    val doc = Jsoup.parse(String(bytes))
                    val key = doc.selectFirst("targetid").text()
                    Log.v("key", key.toString())
                    val totalCount = avBean.duration / 30
                    var loadCount = 0
                    for (i in 0..totalCount)
                        getDanmaku(parseModel, key, i) { map: Map<Int, List<Danmaku>>?, _: Int, _: Parser.LoadStatus ->
                            loadCount++
                            if (map != null) {
                                callback(map, if (loadCount == totalCount) Parser.LoadStatus.SUCCESS else Parser.LoadStatus.COMPLETE)
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null, Parser.LoadStatus.FAILURE)
                }
            }
        }
    }
    */

    private fun getDanmakuAt(parseModel: ParseModel, avBean: VideoInfo, key: String, page: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        var map: MutableMap<Int, MutableList<Danmaku>>? = HashMap()
        var status: Parser.LoadStatus

        parseModel.loader.get("https://mfm.video.qq.com/danmu?timestamp=${page*30}&target_id=$key")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                map = null
                status = Parser.LoadStatus.FAILURE
            }else try {
                Log.v("arr", String(bytes))
                val result = JsonUtil.toJsonObject(String(bytes)).getAsJsonArray("comments")
                result.map{ it.asJsonObject}
                        .forEach {
                            val time = it.get("timepoint").asInt
                            val color = "#" + (it.get("bb_bcolor")?.asString?.replace("0x","")?:"FFFFFF")
                            val context = it.get("content").asString
                            val list: MutableList<Danmaku> = map?.get(time) ?: ArrayList()
                            val danmaku = Danmaku(context, time, color)
                            Log.v("danmaku", danmaku.toString())
                            list += danmaku
                            map?.put(time, list)
                        }
                status = Parser.LoadStatus.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                map = null
                status = Parser.LoadStatus.ERROR
            }
            callback(map, avBean, page, status)
        }
    }

    override fun getDanmakuKey(parseModel: ParseModel, avBean: VideoInfo, callback: (String?, Parser.LoadStatus) -> Unit) {
        parseModel.loader.get("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${avBean.id}") { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                callback(null, Parser.LoadStatus.FAILURE)
            } else try {
                val doc = Jsoup.parse(String(bytes))
                val key = doc.selectFirst("targetid").text()
                callback(key, Parser.LoadStatus.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null, Parser.LoadStatus.ERROR)
            }
        }
    }

    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        val pageStart = pos / 300 * 10
        for(i in 0..19)
            getDanmakuAt(parseModel, avBean, key,pageStart + i, callback)
    }
}