package soko.ekibun.bangumi.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater

object IqiyiParser : Parser {
    override val siteId = Parser.SiteId.IQIYI

    override fun search(parseModel: ParseModel, key: String, callback: (List<Bangumi>?, String, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<Bangumi>? = ArrayList()
        var status: Parser.LoadStatus
        parseModel.loader.get("http://search.video.iqiyi.com/o?channel_name=动漫&if=html5&key=${java.net.URLEncoder.encode(key, "utf-8")}&pageSize=20&video_allow_3rd=0")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
                ret = null
            } else {
                try {
                    val json = String(bytes)
                    val jo = JsonUtil.toJsonObject(json)
                    val data = jo.get("data")//  .getAsJsonObject("data")
                    if (!data.isJsonObject) {
                        status = Parser.LoadStatus.COMPLETE
                        ret = null
                    } else {
                        val infoList = data.asJsonObject.getAsJsonArray("docinfos")
                        infoList.map { it.asJsonObject.getAsJsonObject("albumDocInfo") }
                                .filter { it.has("score") && "iqiyi" == it.get("siteId")?.asString }
                                .forEach {
                                    try {
                                        val categories = ArrayList<String>()
                                        var region = it.get("region")?.asString ?: ""
                                        it.get("threeCategory").asString.split(' ')
                                                .map { it.split(',') }
                                                .forEach {
                                                    if ("1" == it[2])
                                                        region = it[0]
                                                    else
                                                        categories += it[0]
                                                }
                                        val bean = Bangumi(
                                                it.get("albumId").asString,
                                                siteId,
                                                it.get("albumTitle").asString,
                                                it.get("score").asFloat,
                                                it.get("albumVImage").asString,
                                                it.get("albumLink").asString,
                                                it.get("newest_item_number")?.asInt ?: 1,
                                                it.get("itemTotalNumber")?.asInt ?: 1,
                                                region,
                                                categories,
                                                (it.get("stragyTime")?.asString ?: "").split('，')[0],
                                                it.get("latest_update_time").asLong * 1000)
                                        Log.v("search", bean.toString())
                                        ret!! += bean

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                        status = Parser.LoadStatus.SUCCESS
                    }
                } catch (e: Exception) {
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
        parseModel.loader.get("http://pub.m.iqiyi.com/jp/h5/albums/${bangumi.id}") { bytes: ByteArray?, _, type ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
            } else try {
                var json = String(bytes)
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
                val jo = JsonUtil.toJsonObject(json)

                info = Bangumi(jo.get("albumId").asString,
                        siteId,
                        jo.get("albumName").asString,
                        0f,
                        "http:" + jo.get("picUrl").asString,
                        "http:" + jo.get("albumPageUrl").asString,
                        jo.get("latestVideoOrder").asInt,
                        jo.get("videoCount").asInt,
                        jo.get("area").asString,
                        jo.get("tags")?.asString?.split(',')?: ArrayList(),
                        jo.get("updateStrategy")?.asString?.split('，')?.get(0)?:"",
                        jo.get("latestPublishTime").asLong)
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
        parseModel.loader.get("http://mixer.video.iqiyi.com/jp/mixin/videos/avlist?albumId=${bangumi.id}&page=$page&size=$size") { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                ret = null
                status = Parser.LoadStatus.FAILURE
            } else try {
                var json = String(bytes)
                if (json.startsWith("var"))
                    json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
                val jo = JsonUtil.toJsonObject(json)

                val infoList = jo.getAsJsonArray("mixinVideos")
                infoList.map { it.asJsonObject }
                        .forEach {
                            ret!! += VideoInfo(
                                    it.get("tvId").asString,
                                    bangumi,
                                    it.get("order").asInt,
                                    it.get("subtitle").asString,
                                    it.get("url").asString,
                                    it.get("duration").asInt)
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
        var url = parseModel.sharedPreferences.getString("api_iqiyi", "")
        if(url.isEmpty())
            url = "http://api.47ks.com/webcloud/?v="
        if(url.endsWith("="))
            url += avBean.url

        val map = HashMap<String, String>()
        map["referer"]=url
        parseModel.webView.loadUrl(url, map)
    }

    /*
    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, Parser.LoadStatus) -> Unit) {
        val totalCount = (avBean.duration/300) + if(avBean.duration%300>0)1 else 0
        var loadCount = 0
        for(i in 1..totalCount)
            getDanmaku(parseModel, avBean, i){ map: Map<Int, List<Danmaku>>?, _: Int, _: Parser.LoadStatus ->
                loadCount++
                if(map!=null){
                    callback(map, if(loadCount == totalCount) Parser.LoadStatus.SUCCESS else Parser.LoadStatus.COMPLETE)
                } }
    }*/

    private fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, page: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        var map: MutableMap<Int, MutableList<Danmaku>>? = HashMap()
        var status: Parser.LoadStatus
        val tvId  = avBean.id
        parseModel.loader.get("http://cmts.iqiyi.com/bullet/${tvId.substring(tvId.length - 4, tvId.length - 2)}/${tvId.substring(tvId.length - 2, tvId.length)}/${tvId}_300_$page.z") { bytes: ByteArray?, _: String, _ ->
            if (bytes == null) {
                map = null
                status = Parser.LoadStatus.FAILURE
            }else try {
                val xml = String(inflate(bytes))
                val doc = Jsoup.parse(xml)
                val infos = doc.select("bulletInfo")
                for (info in infos) {
                    val time = Integer.valueOf(info.selectFirst("showTime").text())
                    val color = "#" + info.selectFirst("color").text().toUpperCase()
                    val context = info.selectFirst("content").text()
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
        getDanmaku(parseModel, avBean, pos / 300 + 1, callback)
        getDanmaku(parseModel, avBean, pos / 300 + 2, callback)
    }

    private fun inflate(data: ByteArray): ByteArray {
        var output: ByteArray

        val inflater = Inflater()
        inflater.reset()
        inflater.setInput(data)

        val o = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!inflater.finished()) {
                val i = inflater.inflate(buf)
                o.write(buf, 0, i)
            }
            output = o.toByteArray()
        } catch (e: java.lang.Exception) {
            output = data
            e.printStackTrace()
        } finally {
            try {
                o.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        inflater.end()
        return output
    }
}