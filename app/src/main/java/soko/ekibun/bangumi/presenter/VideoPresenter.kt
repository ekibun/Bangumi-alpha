package soko.ekibun.bangumi.presenter

import android.content.pm.ActivityInfo
import android.view.View
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.video_layout.*
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.model.VideoModel
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.view.VideoController
import soko.ekibun.bangumi.view.controller.Controller
import java.util.*

class VideoPresenter(private val context: DetailActivity){
    val danmakuPresenter: DanmakuPresenter by lazy{
        DanmakuPresenter(context.danmaku_view){
            loadDanmaku = true
        }
    }

    val controller: VideoController by lazy{
        VideoController(context.controller_frame, { action: Controller.Action, param: Any ->
            when (action) {
                Controller.Action.PLAY_PAUSE -> doPlayPause(!videoModel.player.playWhenReady)
                Controller.Action.FULLSCREEN ->
                    context.requestedOrientation = if(context.systemUIPresenter.isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                Controller.Action.NEXT -> {
                    context.viewpager.loadAv(nextAv)
                }
                Controller.Action.DANMAKU -> {
                    danmakuPresenter.view.visibility = if(danmakuPresenter.view.visibility == View.VISIBLE)
                        View.INVISIBLE else View.VISIBLE
                    controller.updateDanmaku(danmakuPresenter.view.visibility == View.VISIBLE)
                }
                Controller.Action.SEEKTO -> {
                    videoModel.player.seekTo(param as Long)
                    this.controller.updateProgress(videoModel.player.currentPosition)
                }
                Controller.Action.SHOW -> {
                    updatePauseResume()
                    updateProgress()
                    context.mask_view.visibility = View.VISIBLE
                    context.toolbar.visibility = View.VISIBLE
                }
                Controller.Action.HIDE -> {
                    context.mask_view.visibility = View.INVISIBLE
                    context.toolbar.visibility = View.INVISIBLE
                }
                Controller.Action.TITLE -> {
                    doPlayPause(false)
                    context.app_bar.setExpanded(false)
                    context.systemUIPresenter.appbarCollapsible(true)
                }
            }
        }, { context.systemUIPresenter.isLandscape })
    }
    val videoModel: VideoModel by lazy{
        VideoModel(context){ action: VideoModel.Action, param: Any ->
            when(action){
                VideoModel.Action.READY -> {
                    if(!controller.ctrVisibility){
                        controller.ctrVisibility = true
                        context.tv_logcat.visibility = View.INVISIBLE
                        controller.doShowHide(false)
                    }
                    if(this.videoModel.player.playWhenReady)
                        doPlayPause(true)
                    if(!controller.isShow){
                        context.mask_view.visibility = View.INVISIBLE
                        context.toolbar.visibility = View.INVISIBLE
                    }
                    controller.updateLoading(false)
                }
                VideoModel.Action.BUFFERING -> controller.updateLoading(true)
                VideoModel.Action.ENDED -> doPlayPause(false)
                VideoModel.Action.VIDEO_SIZE_CHANGE -> {
                    val array = param as Array<*>
                    val width = array[0] as Int
                    val height = array[1] as Int
                    val pixelWidthHeightRatio = array[3] as Float
                    context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * width * pixelWidthHeightRatio/ height)) / context.video_surface.measuredWidth
                    context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * height * pixelWidthHeightRatio/ width)) / context.video_surface.measuredHeight
                }
            }
        }
    }

    private var loadVideo = false
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadDanmaku = false
        set(v) {
            field = v
            parseLogcat()
        }
    private fun parseLogcat(){
        context.tv_logcat.post{
            context.tv_logcat.text = "解析视频地址… " + (if(loadVideo) "【完成】" else "") + "\n" +
                    "全舰弹幕装填… "+ (if(danmakuPresenter.finished) "【完成】" else "") +
                    if(loadVideo) "\n开始视频缓冲…" else ""
        }
    }

    private var nextAv: VideoInfo? = null
    fun loadVideo(avBean: VideoInfo){
        loadVideo = false
        loadDanmaku = false
        nextAv = context.viewpager.getNext(avBean)
        controller.updateNext(nextAv != null)
        videoModel.player.playWhenReady = false
        controller.updateLoading(true)
        context.video_surface_container.visibility = View.VISIBLE
        context.video_surface.visibility = View.VISIBLE
        context.controller_frame.visibility = View.VISIBLE
        controller.ctrVisibility = false
        context.tv_logcat.visibility = View.VISIBLE
        controller.doShowHide(true)
        controller.setTitle(avBean.parseTitle(context))
        danmakuPresenter.loadDanmaku(avBean)
        playLoopTask?.cancel()
        videoModel.playVideo(avBean, context.video_surface){
            loadVideo = true
        }
    }

    private var playLoopTask: TimerTask? = null
    fun doPlayPause(play: Boolean){
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if(play){
            playLoopTask = object: TimerTask(){ override fun run() {
                updateProgress()
                danmakuPresenter.add(videoModel.player.currentPosition)
            } }
            controller.timer.schedule(playLoopTask, 0, 1000)
            context.video_surface.keepScreenOn = true
            if(!controller.isShow)context.toolbar.visibility = View.INVISIBLE
            danmakuPresenter.view.resume()
        }else{
            context.video_surface.keepScreenOn = false
            danmakuPresenter.view.pause()
        }
        context.systemUIPresenter.appbarCollapsible(!play)
    }

    private fun updateProgress(){
        controller.duration = videoModel.player.duration.toInt() /10
        controller.buffedPosition = videoModel.player.bufferedPosition.toInt() /10
        controller.updateProgress(videoModel.player.currentPosition)
    }

    private fun updatePauseResume() {
        controller.updatePauseResume(videoModel.player.playWhenReady)
    }
}