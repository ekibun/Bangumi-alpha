package soko.ekibun.bangumi.parser

import android.util.Log
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil


object PptvParser: Parser {
    override val siteId: Parser.SiteId = Parser.SiteId.PPTV

    private val header: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
        map["Cookie"] = "PUID=616193bbc9804a7dde16-12845cf9388b; __crt=1474886744633; ppi=302c3638; Hm_lvt_7adaa440f53512a144c13de93f4c22db=1475285458,1475556666,1475752293,1475913662"
        map
    }

    override fun search(parseModel: ParseModel, key: String, callback: (List<Bangumi>?, String, Parser.LoadStatus)->Unit){
        var ret: MutableList<Bangumi>? = ArrayList()
        var status: Parser.LoadStatus
        parseModel.loader.get("http://search.pptv.com/result?search_query=${java.net.URLEncoder.encode(key, "utf-8")}&result_type=3", header)
        { bytes: ByteArray?, _, _ ->
            if (bytes == null){
                status = Parser.LoadStatus.FAILURE
                ret = null
            }else{
                try {
                    val doc = Jsoup.parse(String(bytes))
                    val lists = doc.select(".scon")
                    lists.forEach {
                        try {
                            val bpic = it.selectFirst(".bpic").selectFirst("img")
                            val type = it.selectFirst(".pinfo").select(".w100")
                            val category = ArrayList<String>()
                            var region = ""
                            type.forEach {
                                if(it.text().contains("地区"))
                                    region = it.selectFirst("a").text()
                                else
                                    category.add(it.selectFirst("a").text())
                            }
                            val dl = it.selectFirst(".tit").selectFirst("a")

                            val renew = it.selectFirst(".msk-txt")?.text() ?: ""
                            val newOrder = Regex("""([0-9]+)集""").find(renew)?.groupValues?.get(1)?.toInt() ?: 1
                            val strategy = ""///TODO(it.selectFirst(".s_collect")?.selectFirst("a")?.text() ?: "").split(' ')[0]
                            val count = if (strategy.isEmpty()) newOrder else 0
                            val score = Regex("""([0-9.]+)分""").find(it.selectFirst(".tit").selectFirst("span").text())?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

                            val bean = Bangumi(
                                    bpic.attr("alt"),
                                    siteId,
                                    dl.attr("title"),
                                    score,
                                    bpic.attr("src"),
                                    dl.attr("href"),
                                    newOrder,
                                    count,
                                    region,
                                    category,
                                    strategy,
                                    0L)
                            Log.v("search", bean.toString())
                            ret!! += bean

                        } catch (e: Exception) {
                            //e.printStackTrace()
                        }
                    }
                    status = Parser.LoadStatus.SUCCESS
                }catch(e: Exception) {
                    e.printStackTrace()
                    ret = null
                    status = Parser.LoadStatus.ERROR
                }
            }
            callback(ret, key, status)
        }
    }

    override fun getInfo(parseModel: ParseModel, bangumi: Bangumi, callback: (Bangumi?, Parser.LoadStatus, LoaderUtil.LoadType) -> Unit) {
        var info: Bangumi? = null
        var status: Parser.LoadStatus
        parseModel.loader.get("http://apis.web.pptv.com/show/star?cid=${bangumi.id}", header) { bytes: ByteArray?, _, type ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
            } else try {
                val jsonArray = JsonUtil.toJsonObject(String(bytes)).get("info").asJsonArray
                var jo: JsonObject? = null
                jsonArray.forEach {
                    if(it.asJsonObject.get("id").asString == bangumi.id)
                        jo = it.asJsonObject
                }
                if(jo == null)
                    throw Exception("wrong pptv id: ${bangumi.id}")
                parseModel.loader.get("http://apis.web.pptv.com/show/videoList?format=jsonp&pid=${bangumi.id}", header) { bytes2: ByteArray?, _, _ ->
                    if (bytes2 == null) {
                        status = Parser.LoadStatus.FAILURE
                    } else try {
                        val src = JsonUtil.toJsonObject(String(bytes2))
                        val data = src.get("data").asJsonObject
                        val count = data.get("total").asString.toInt()
                        val total = if(data.get("update").asInt == 0) count else 0
                        info = Bangumi(bangumi.id,
                                siteId,
                                jo!!.get("title").asString,
                                0f,
                                jo!!.get("coverPic").asString,
                                jo!!.get("href").asString,
                                count,
                                total,
                                "",
                                listOf(),
                                data.get("fixupdate").asString,
                                0L)
                        Log.v("info", info.toString())
                        status = Parser.LoadStatus.SUCCESS
                    } catch (e: Exception) {
                        e.printStackTrace()
                        status = Parser.LoadStatus.ERROR
                        callback(info, status, type)
                    }
                    callback(info, status, type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                status = Parser.LoadStatus.ERROR
                callback(info, status, type)
            }
        }
    }

    override fun getVideoList(parseModel: ParseModel, bangumi: Bangumi, page: Int, size: Int, callback: (List<VideoInfo>?, Int, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<VideoInfo>? = ArrayList()
        parseModel.loader.get("http://apis.web.pptv.com/show/videoList?format=jsonp&pid=${bangumi.id}", header) { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                ret = null
                callback(ret, page, Parser.LoadStatus.FAILURE)
            } else try {
                val src = JsonUtil.toJsonObject(String(bytes))
                val list = src.get("data").asJsonObject.get("list").asJsonArray
                for(i in (page-1)*size until Math.min(page*size, list.size()) ){
                    val v = list[i].asJsonObject
                    ret!! += VideoInfo(
                            v.get("id").asString,
                            bangumi,
                            i + 1,
                            v.get("title").asString,
                            v.get("url").asString,
                            0)
                }
                callback(ret, page, Parser.LoadStatus.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()
                ret = null
                callback(ret, page, Parser.LoadStatus.FAILURE)
            }
        }
    }

    override fun getVideo(parseModel: ParseModel, avBean: VideoInfo, callback: (String, Parser.LoadStatus) -> Unit) {
        parseModel.webView.onCatchVideo={
            Log.v("video", it.url.toString())
            if(it.url.toString().contains("mdparse.duapp.com/404.mp4")){
                callback(it.url.toString(), Parser.LoadStatus.FAILURE)
            }else{
                callback(it.url.toString(), Parser.LoadStatus.SUCCESS)
            }
            parseModel.webView.onCatchVideo={}
        }
        var url = parseModel.sharedPreferences.getString("api_pptv", "")
        if(url.isEmpty())
            url = "https://jx.maoyun.tv/?id="
        if(url.endsWith("="))
            url += avBean.url.split("?")[0]

        val map = HashMap<String, String>()
        map["referer"]=url
        parseModel.webView.loadUrl(url, map)
    }

    /*
    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, Parser.LoadStatus) -> Unit) {
        getDanmaku(parseModel, avBean.id, 0){ map: Map<Int, List<Danmaku>>?, _: Int, _ ->
            if (map != null) {
                callback(map, if (map.isEmpty()) Parser.LoadStatus.SUCCESS else Parser.LoadStatus.COMPLETE)
            }
        }
    }*/

    private fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, page: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        var map: MutableMap<Int, MutableList<Danmaku>>? = HashMap()
        var status: Parser.LoadStatus

        parseModel.loader.get("http://apicdn.danmu.pptv.com/danmu/v4/pplive/ref/vod_${avBean.id}/danmu?pos=${page* 1000}", header)
        { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                map = null
                status = Parser.LoadStatus.FAILURE
            }else try {
                val result = JsonUtil.toJsonObject(String(bytes)).getAsJsonObject("data").getAsJsonArray("infos")
                result.map{ it.asJsonObject}
                        .filter { it.get("id").asLong != 0L }
                        .forEach {
                            val time = it.get("play_point").asInt / 10
                            val fontclr = it.get("font_color")
                            val color = if(fontclr is JsonNull) "#FFFFFF" else fontclr.asString
                            val context = Jsoup.parse(it.get("content").asString).text()
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
        callback(avBean.id, Parser.LoadStatus.SUCCESS)
    }

    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        val pageStart = pos / 300 * 3
        for(i in 0..5)
            getDanmaku(parseModel, avBean, pageStart + i, callback)
    }
}