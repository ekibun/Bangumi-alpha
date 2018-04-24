package soko.ekibun.bangumi.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil

object YoukuParser: Parser{
    override val siteId: Parser.SiteId = Parser.SiteId.YOUKU

    private val header: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
        map
    }

    override fun search(parseModel: ParseModel, key: String, callback: (List<Bangumi>?, String, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<Bangumi>? = ArrayList()
        var status: Parser.LoadStatus
        parseModel.loader.get("http://www.soku.com/search_video/q_${java.net.URLEncoder.encode(key, "utf-8")}", header)
        { bytes: ByteArray?, _, _ ->
            if (bytes == null){
                status = Parser.LoadStatus.FAILURE
                ret = null
            }else try {
                val doc = Jsoup.parse(String(bytes))
                val lists = doc.select(".s_dir")
                lists.filter { "14" == it.selectFirst(".pos_area")?.attr("siteid") }
                        .forEach {
                            try {
                                val type = it.selectFirst(".base_type").text()
                                if (!type.contains("动漫"))
                                    return@forEach
                                val a = it.selectFirst(".base_name").selectFirst("a")

                                val renew = it.selectFirst(".s_collect")?.text() ?: ""
                                val newOrder = Regex("""([0-9]+)集""").find(renew)?.groupValues?.get(1)?.toInt() ?: 1
                                val strategy = (it.selectFirst(".s_collect")?.selectFirst("a")?.text()
                                        ?: "").split(' ')[0]
                                val count = if (strategy.isEmpty()) newOrder else 0
                                val region = it.selectFirst(".s_area").text().substringAfter("：")
                                val vid = a.attr("_iku_showid")

                                val bean = Bangumi(
                                        vid,
                                        siteId,
                                        a.attr("_log_title"),
                                        it.selectFirst(".s_overlay").text().toFloatOrNull() ?: 0f,
                                        "http:" + it.selectFirst(".s_target").selectFirst("img").attr("src"),
                                        "http://list.youku.com/show/id_z$vid",
                                        newOrder,
                                        count,
                                        region,//it.selectFirst(".s_area").text(),
                                        listOf(type),
                                        strategy,
                                        0L)
                                Log.v("search", bean.toString())
                                ret!! += bean

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                status = Parser.LoadStatus.SUCCESS
            }catch(e: Exception) {
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
        parseModel.loader.get("http://list.youku.com/show/id_z${bangumi.id}", header) { bytes: ByteArray?, _, type ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
            } else try {
                val doc = Jsoup.parse(String(bytes))
                //Log.v("doc", doc.toString())
                var area = ""
                val category = ArrayList<String>()
                for(list in doc.selectFirst(".p-base").selectFirst("ul").select("li")){
                    if(list.text().startsWith("地区"))
                        area = list.text()
                    else if(list.text().startsWith("类型"))
                        for(a in list.select("a"))
                            category + a.text()
                }
                val renew = doc.selectFirst(".p-renew")?.text()?:""
                val newOrder = Regex("""([0-9]+)集""").find(renew)?.groupValues?.get(1)?.toInt()?:1
                val strategy = (Regex("""（(.*)）""").find(renew)?.groupValues?.get(1)?:"").split(' ')[0]
                val count = if(strategy.isEmpty()) newOrder else 0

                info = Bangumi(bangumi.id,
                        siteId,
                        doc.selectFirst(".p-thumb").selectFirst("a").attr("title"),
                        doc.selectFirst(".star-num").text().toFloatOrNull()?:0f,
                        doc.selectFirst(".p-thumb").selectFirst("img").attr("src"),
                        "http://list.youku.com/show/id_z${bangumi.id}",
                        newOrder,
                        count,
                        area,
                        category,
                        strategy,
                        0L)
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
        for(i in ((page-1)*size/10)..(page*size/10 - 1)){
            parseModel.loader.get("http://list.youku.com/show/episode?id=${bangumi.id}&stage=reload_${i*10+1}&callback=jQuery") { bytes: ByteArray?, _, _ ->
                if (bytes == null) {
                    ret = null
                    callback(ret, page, Parser.LoadStatus.FAILURE)
                } else try {
                    val src = String(bytes)
                    val element = JsonUtil.toJsonObject(src.substring(src.indexOf("{"), src.lastIndexOf("}") + 1))
                    val li = Jsoup.parse(element.get("html").asString)
                    li.select(".c555").forEach {
                        val vid = Regex("""id_([^.=]+)""").find(it.attr("href"))?.groupValues?.get(1)?:"http:" + it.attr("href")
                        ret!! += VideoInfo(vid,
                                bangumi,
                                it.parent().text().substringBefore(it.text()).toInt(),
                                it.text(),
                                "http:" + it.attr("href"),
                                0)
                    }
                    ret?.sortBy { it.order }
                    callback(ret, page, Parser.LoadStatus.SUCCESS)
                } catch (e: Exception) {
                    //e.printStackTrace()
                }
            }
        }
    }

    override fun getVideo(parseModel: ParseModel, avBean: VideoInfo, callback: (String, Parser.LoadStatus) -> Unit) {

        var url = parseModel.sharedPreferences.getString("api_youku", "")
        if(url.isEmpty()){
            val utid = "3nOsESeJhAUCAbZyXYm8UflX"
            val ckey = java.net.URLEncoder.encode("DIl58SLFxFNndSV1GFNnMQVYkx1PP5tKe1siZu/86PR1u/Wh1Ptd+WOZsHHWxysSfAOhNJpdVWsdVJNsfJ8Sxd8WKVvNfAS8aS8fAOzYARzPyPc3JvtnPHjTdKfESTdnuTW6ZPvk2pNDh4uFzotgdMEFkzQ5wZVXl2Pf1/Y6hLK0OnCNxBj3+nb0v72gZ6b0td+WOZsHHWxysSo/0y9D2K42SaB8Y/+aD2K42SaB8Y/+ahU+WOZsHcrxysooUeND", "utf-8")
            parseModel.loader.get("""https://ups.youku.com/ups/get.json?vid=${avBean.id}&ccode=0515&client_ip=192.168.1.1&utid=$utid&client_ts=${System.currentTimeMillis() / 1000}&ckey=$ckey""")
            { bytes: ByteArray?, _, _ ->
                if(bytes==null){
                    callback("https://mdparse.duapp.com/404.mp4", Parser.LoadStatus.FAILURE)
                }else try {
                    val data = JsonUtil.toJsonObject(String(bytes)).get("data").asJsonObject
                    val pay = data.has("pay")
                    //Log.v("data", data.get("pay").toString())
                    Log.v("pay", pay.toString())
                    var videoUrl = ""
                    val stream = data.getAsJsonArray("stream")
                    if(pay){
                        stream.map{it.asJsonObject}
                                .forEach {
                                    if(it.getAsJsonObject("stream_ext")?.get("one_seg_flag")?.asInt == 1){
                                        videoUrl = it.getAsJsonArray("segs")[0].asJsonObject.get("cdn_url").asString
                                        Log.v("video", videoUrl)
                                        return@forEach
                                    }
                                }
                    }
                    if(videoUrl.isEmpty())
                        videoUrl = stream.get(0).asJsonObject.get("m3u8_url").asString
                    Log.v("video", videoUrl)
                    callback(videoUrl, Parser.LoadStatus.SUCCESS)
                }catch(e: Exception){
                    e.printStackTrace()
                    callback("https://mdparse.duapp.com/404.mp4", Parser.LoadStatus.FAILURE)
                }
            }
        }else{
            parseModel.webView.onCatchVideo={
                Log.v("video", it.url.toString())
                if(it.url.toString().contains("/404.mp4")){
                    callback(it.url.toString(), Parser.LoadStatus.FAILURE)
                }else{
                    callback(it.url.toString(), Parser.LoadStatus.SUCCESS)
                }
                parseModel.webView.onCatchVideo={}
            }
            val map = HashMap<String, String>()
            if(url.endsWith("="))
                url += avBean.url
            map["referer"]=url
            parseModel.webView.loadUrl(url, map)
        }
    }

    /*
    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, Parser.LoadStatus) -> Unit) {
        parseModel.loader.get(avBean.url, header) { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                callback(null, Parser.LoadStatus.FAILURE)
            } else try {
                val html = String(bytes)
                Log.v("html", html)
                val iid = Regex(""" videoId:"([^"]+)"""").find(html)!!.groupValues[1]
                val seconds = Regex(""" seconds:"([^"]+)"""").find(html)!!.groupValues[1].toFloat().toInt()
                var loadCount = 0
                for (i in 0..seconds / 60) {
                    getDanmaku(parseModel, iid, i) { map: Map<Int, List<Danmaku>>?, _: Int, _: Parser.LoadStatus ->
                        loadCount++
                        if (map != null) {
                            callback(map, if (loadCount == seconds) Parser.LoadStatus.SUCCESS else Parser.LoadStatus.COMPLETE)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/

    private fun getDanmakuAt(parseModel: ParseModel, avBean: VideoInfo, iid: String, page: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        var map: MutableMap<Int, MutableList<Danmaku>>? = HashMap()
        var status: Parser.LoadStatus

        parseModel.loader.get("http://service.danmu.youku.com/list?jsoncallback=&mat=$page&mcount=1&ct=1001&iid=$iid")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                map = null
                status = Parser.LoadStatus.FAILURE
            }else try {
                val result = JsonUtil.toJsonObject(String(bytes)).getAsJsonArray("result")
                result.map{ it.asJsonObject}
                        .forEach {
                            val time = it.get("playat").asInt / 1000 //Integer.valueOf(info.selectFirst("showTime").text())
                            val property = it.get("propertis").asString

                            val color = "#" + if(property.contains("color")) String.format("%06x", JsonUtil.toJsonObject(property).get("color").asInt).toUpperCase() else "FFFFFF"//info.selectFirst("color").text().toUpperCase()
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
        parseModel.loader.get(avBean.url, header) { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                callback(null, Parser.LoadStatus.FAILURE)
            } else try {
                val iid = Regex(""" videoId:"([^"]+)"""").find(String(bytes))!!.groupValues[1]
                callback(iid, Parser.LoadStatus.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null, Parser.LoadStatus.ERROR)
            }
        }
    }

    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        val pageStart = pos / 300 * 5
        for(i in 0..9)
            getDanmakuAt(parseModel, avBean, key,pageStart + i, callback)
    }

}