package soko.ekibun.bangumi.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.text.format.Formatter
import com.google.android.exoplayer2.offline.Downloader
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.NotificationUtil
import java.util.concurrent.Executors
import android.app.NotificationManager
import soko.ekibun.bangumi.activity.SplashActivity
import java.util.*


class DownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val timer = Timer()

    private val cachedThreadPool = Executors.newCachedThreadPool()
    val taskCollection = HashMap<String, DownloadTask>()

    val manager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private fun getGroupSummary(): String{
        val groupKey = "download"
        manager.notify(0, NotificationUtil.builder(this, downloadCannelId, "下载")
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setGroup(groupKey)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, SplashActivity::class.java), 0)).build())
        return groupKey
    }

    override fun onCreate() {
        super.onCreate()
        val downloadWatcher = object: TimerTask(){
            override fun run() {
                taskCollection.forEach {
                    val video = it.value.video
                    val downloader = it.value.downloader
                    val percent = downloader.downloadPercentage
                    val bytes = downloader.downloadedBytes
                    val isFinished = VideoCacheModel.isFinished(percent)
                    if(isFinished)
                        taskCollection.remove(it.key)

                    sendBroadcast(video, percent, bytes)
                    manager.notify(video.id, 0, NotificationUtil.builder(this@DownloadService, downloadCannelId, "下载")
                            .setSmallIcon(R.drawable.ic_download)
                            .setOngoing(!isFinished)
                            .setAutoCancel(true)
                            .setGroup(this@DownloadService.getGroupSummary())
                            .setContentTitle((if(isFinished)"已完成 " else "") + "${video.bangumi.title} ${video.parseTitle(this@DownloadService)}")
                            .setContentText(if(isFinished)Formatter.formatFileSize(this@DownloadService, bytes) else parseDownloadInfo(this@DownloadService, percent, bytes))
                            .setProgress(10000, (percent * 100).toInt(), bytes == 0L)
                            .setContentIntent(PendingIntent.getActivity(this@DownloadService, video.id.hashCode(),
                                    DetailActivity.parseIntent(this@DownloadService, video.bangumi, true), PendingIntent.FLAG_UPDATE_CURRENT)).build())
                }
            }
        }
        timer.scheduleAtFixedRate(downloadWatcher, 0, 1000)
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    class DownloadTask(val video: VideoInfo, val downloader: Downloader): AsyncTask<Unit, Unit, Unit>(){

        override fun doInBackground(vararg params: Unit?) {
            while(!Thread.currentThread().isInterrupted && ! VideoCacheModel.isFinished(downloader.downloadPercentage)){
                try {
                    downloader.download{ _, _, _ -> }
                } catch (e: InterruptedException) {
                    break
                }catch(e: Exception){
                    e.printStackTrace()
                    Thread.sleep(1000)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null){
            val video = JsonUtil.toEntity(intent.getStringExtra("video"), VideoInfo::class.java)
            video.bangumi = JsonUtil.toEntity(intent.getStringExtra("bangumi"), Bangumi::class.java)

            when(intent.action){
                "download" -> {
                    val task = taskCollection[video.id]
                    if(task!= null){
                        taskCollection.remove(video.id)
                        task.cancel(true)
                        sendBroadcast(video, task.downloader.downloadPercentage, task.downloader.downloadedBytes, true)
                        manager.notify(video.id, 0, NotificationUtil.builder(this@DownloadService, downloadCannelId, "下载")
                                .setSmallIcon(R.drawable.ic_download)
                                .setOngoing(false)
                                .setAutoCancel(true)
                                .setGroup(this@DownloadService.getGroupSummary())
                                .setContentTitle("已暂停 ${video.bangumi.title} ${video.parseTitle(this@DownloadService)}")
                                .setContentText(parseDownloadInfo(this@DownloadService, task.downloader.downloadPercentage, task.downloader.downloadedBytes))
                                .setProgress(10000, (task.downloader.downloadPercentage * 100).toInt(), task.downloader.downloadedBytes == 0L)
                                .setContentIntent(PendingIntent.getActivity(this@DownloadService, video.id.hashCode(),
                                        DetailActivity.parseIntent(this@DownloadService, video.bangumi, true), PendingIntent.FLAG_UPDATE_CURRENT)).build())
                    }else{
                        val url = intent.getStringExtra("url")
                        val downloader = App.getVideoCacheModel(this).getDownloader(url)
                        val newTask = DownloadTask(video, downloader)
                        taskCollection[video.id] = newTask
                        newTask.executeOnExecutor(cachedThreadPool)
                    }
                }
                "remove" -> {
                    manager.cancel(video.id, 0)
                    if(taskCollection.containsKey(video.id)){
                        taskCollection[video.id]!!.cancel(true)
                        taskCollection.remove(video.id)
                    }
                    if(taskCollection.isEmpty())
                        manager.cancel(0)
                    val videoCacheModel = App.getVideoCacheModel(this)
                    videoCacheModel.getCache(video)?.url?.let {
                        videoCacheModel.getDownloader(it).remove() }
                    videoCacheModel.removeVideoCache(video)
                    sendBroadcast(video, Float.NaN, 0, false)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun sendBroadcast(video: VideoInfo, percent: Float, bytes: Long, hasCache: Boolean? = null){
        val broadcastIntent = Intent(video.bangumi.parseString())
        broadcastIntent.putExtra("video", JsonUtil.toJson(video))
        broadcastIntent.putExtra("percent", percent)
        broadcastIntent.putExtra("bytes", bytes)
        hasCache?.let{ broadcastIntent.putExtra("cancel", it) }
        sendBroadcast(broadcastIntent)
    }


    companion object {
        const val downloadCannelId = "download"

        fun parseDownloadInfo(context: Context, percent: Float, bytes: Long): String{
            return "${Formatter.formatFileSize(context, bytes)}/${Formatter.formatFileSize(context, (bytes * 100 / percent).toLong())}"
        }

        fun download(context: Context, video: VideoInfo, url: String){
            App.getVideoCacheModel(context).addVideoCache(video, url)
            val intent = Intent(context, DownloadService::class.java)
            intent.action = "download"
            intent.putExtra("bangumi", JsonUtil.toJson(video.bangumi))
            intent.putExtra("video", JsonUtil.toJson(video))
            intent.putExtra("url", url)
            context.startService(intent)
        }
        fun remove(context: Context, video: VideoInfo){
            val intent = Intent(context, DownloadService::class.java)
            intent.action = "remove"
            intent.putExtra("bangumi", JsonUtil.toJson(video.bangumi))
            intent.putExtra("video", JsonUtil.toJson(video))
            context.startService(intent)
        }
    }
}
