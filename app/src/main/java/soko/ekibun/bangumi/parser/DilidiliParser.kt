package soko.ekibun.bangumi.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.LoaderUtil

object DilidiliParser: Parser{
    override val siteId: Parser.SiteId = Parser.SiteId.DILIDILI

    override fun search(parseModel: ParseModel, key: String, callback: (List<Bangumi>?, String, Parser.LoadStatus) -> Unit) {
        var ret: MutableList<Bangumi>? = ArrayList()
        var status: Parser.LoadStatus
        /*
        parseModel.loader.get("https://www.google.com.hk/search?&q=${java.net.URLEncoder.encode(key, "utf-8")}+dilidili%2Fanime")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null){
                status = Parser.LoadStatus.FAILURE
                ret = null
            }else try {
                val doc = Jsoup.parse(String(bytes))
                doc.selectFirst(".srg").select("a").forEach {
                    val id = Regex("""dilidili.wang/anime/([^/]*)/""").find(it.attr("href")?:"")?.groupValues?.get(1)?:""
                    if(id.isEmpty())
                        return@forEach
                    getInfo(parseModel, Bangumi(id, siteId)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                        bangumi?.let{ ret!! +=  it }
                    }
                }
                status = Parser.LoadStatus.SUCCESS
            }catch(e: Exception) {
                e.printStackTrace()
                ret = null
                status = Parser.LoadStatus.ERROR
            }
            callback(ret, key, status)
        }*/
        parseModel.loader.get("http://zhannei.baidu.com/cse/search?s=4514337681231489739&q=${java.net.URLEncoder.encode(key, "utf-8")}&stp=1&site=www.dilidili.wang")
        { bytes: ByteArray?, _, _ ->
            if (bytes == null){
                status = Parser.LoadStatus.FAILURE
                ret = null
            }else try {
                val doc = Jsoup.parse(String(bytes))
                val lists = doc.select(".result")
                lists.filter { it.attr("href")?.contains("dilidili.wang/anime/") == true }
                        .forEach {
                            try {
                                //val title = it.selectFirst(".result-title").text()
                                val id = Regex("""dilidili.wang/anime/([^/]*)/""").find(it.attr("href"))?.groupValues?.get(1)?:""
                                if(id.isEmpty())
                                    return@forEach
                                getInfo(parseModel, Bangumi(id, siteId)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                                    bangumi?.let{ ret!! +=  it }
                                }
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
        parseModel.loader.get("http://m.dilidili.wang/anime/${bangumi.id}/"){ bytes: ByteArray?, _: String, type: LoaderUtil.LoadType ->
            if (bytes == null) {
                status = Parser.LoadStatus.FAILURE
            } else try {
                val d = Jsoup.parse(String(bytes))
                val detail = d.selectFirst(".details-hd")
                val imgUrl = detail.selectFirst("img").attr("src")
                val title = detail.selectFirst("h1").text()
                val count = d.selectFirst(".episodeWrap").select("a").filter { !(Regex("""dilidili.wang/watch[0-9]?/([^/]*)/""").find(it.attr("href"))?.groupValues?.get(1)?:"").isEmpty() }.size
                var totalCount = count
                var region = ""
                val category = ArrayList<String>()
                detail.select("p").forEach {
                    when {
                        it.text().contains("更新至") -> totalCount = 0
                        it.text().contains("地区：") -> region = it.text().replace("地区：", "")
                        it.text().contains("标签：") -> category.addAll(it.text().replace("标签：", "").split("|"))
                    }
                }
                info = Bangumi(
                        bangumi.id,
                        siteId,
                        title,
                        0f,
                        imgUrl,
                        "http://m.dilidili.wang/anime/${bangumi.id}/",
                        count,
                        totalCount,
                        region,
                        category,
                        "",
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
        parseModel.loader.get("http://m.dilidili.wang/anime/${bangumi.id}/") { bytes: ByteArray?, _, _ ->
            if (bytes == null) {
                ret = null
                callback(ret, page, Parser.LoadStatus.FAILURE)
            } else try {
                val d = Jsoup.parse(String(bytes))
                val list = d.selectFirst(".episodeWrap").select("a")
                for(i in (page-1)*size until Math.min(page*size, list.size) ){
                    val url = list[i].attr("href")
                    val id = Regex("""dilidili.wang/watch[0-9]?/([^/]*)/""").find(url)?.groupValues?.get(1)?:""
                    if(id.isEmpty())
                        continue
                    ret!! += VideoInfo(
                            id,
                            bangumi,
                            i + 1,
                            list[i].text(),
                            url,
                            0)
                }
                callback(ret, page, Parser.LoadStatus.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()
                ret = null
                callback(ret, page, Parser.LoadStatus.ERROR)
            }
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
        val map = HashMap<String, String>()
        map["referer"]=avBean.url
        parseModel.webView.loadUrl(avBean.url, map)
    }

    override fun getDanmakuKey(parseModel: ParseModel, avBean: VideoInfo, callback: (String?, Parser.LoadStatus) -> Unit) {
        //TODO("not implemented")
    }

    override fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, Parser.LoadStatus) -> Unit) {
        //TODO("not implemented")
    }

}