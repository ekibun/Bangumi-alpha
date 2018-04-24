package soko.ekibun.bangumi.parser

import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.LoaderUtil

interface Parser {
    val siteId: SiteId

    fun search(parseModel: ParseModel, key: String = "", callback: (List<Bangumi>?, String, LoadStatus)->Unit)

    fun getInfo(parseModel: ParseModel, bangumi: Bangumi, callback: (Bangumi?, LoadStatus, LoaderUtil.LoadType) -> Unit)

    fun getVideoList(parseModel: ParseModel, bangumi: Bangumi, page: Int, size: Int, callback: (List<VideoInfo>?, Int, LoadStatus) -> Unit)

    fun getVideo(parseModel: ParseModel, avBean: VideoInfo, callback: (String, LoadStatus) -> Unit)

    //fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, callback: (Map<Int, List<Danmaku>>?, LoadStatus) -> Unit)

    fun getDanmakuKey(parseModel: ParseModel, avBean: VideoInfo, callback: (String?, LoadStatus) -> Unit)

    fun getDanmaku(parseModel: ParseModel, avBean: VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?, VideoInfo, Int, LoadStatus) -> Unit)

    enum class LoadStatus{
        SUCCESS, FAILURE, ERROR, COMPLETE
    }

    enum class SiteId(val label: String, val id: Int) {
        ALL("", -1),
        IQIYI("爱奇艺", 0),
        YOUKU("优酷", 1),
        TXVIDEO("腾讯视频", 2),
        PPTV("PPTV", 3),
        DILIDILI("Dilidili", 4)
    }
}