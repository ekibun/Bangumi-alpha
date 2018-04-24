package soko.ekibun.bangumi.presenter

import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Danmaku
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.view.DanmakuView

class DanmakuPresenter(val view: DanmakuView,
                       private val onFinish:()->Unit){
    private val parseModel: ParseModel by lazy{ ParseModel(view.context) }
    private val danmakus = HashMap<Int, List<Danmaku>>()

    private var videoInfo: VideoInfo? = null
    private var danmakuKey: String = ""

    val callback = { map: Map<Int, List<Danmaku>>?, video: VideoInfo, pos: Int, loadStatus: Parser.LoadStatus ->
        if(map!= null && video.id == videoInfo?.id)
            danmakus.putAll(map)
        if(loadStatus == Parser.LoadStatus.COMPLETE && video.id == videoInfo?.id){
            finished = true
            onFinish()
        }
    }

    var finished: Boolean = false
    fun loadDanmaku(avBean: VideoInfo){
        finished = false
        danmakus.clear()
        view.clear()
        lastPos = 0
        videoInfo = avBean
        danmakuKey = ""
        parseModel.getDanmakuKey(avBean){ s: String?, loadStatus: Parser.LoadStatus ->
            if(loadStatus == Parser.LoadStatus.SUCCESS && s!= null) {
                danmakuKey = s
                parseModel.getDanmaku(avBean, s, 0, callback)
            }
        }
    }

    var lastPos = 0
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos != newPos){
            lastPos = newPos
            videoInfo?.let {
                parseModel.getDanmaku(it, danmakuKey, (pos / 1000).toInt(), callback)
            }
        }
        view.add(danmakus[(pos/1000).toInt()]?: ArrayList())
    }
}