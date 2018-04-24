package soko.ekibun.bangumi.view.adapter

import android.content.Context
import android.support.design.widget.Snackbar
import android.text.format.Formatter
import android.view.View
import kotlinx.android.synthetic.main.item_video.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.PlayHistoryModel
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.service.DownloadService
import soko.ekibun.util.ResourceUtil
import java.util.concurrent.Executors

class VideoListAdapter : BaseAdapter<VideoInfo>(R.layout.item_video) {

    private val cachedThreadPool = Executors.newFixedThreadPool(5)

    override fun bindData(holder: BaseViewHolder, itemValue: VideoInfo, position: Int, listener: ((itemValue: VideoInfo, view: View, flag: Int) -> Unit)) {
        holder.itemView.item_layout.setOnClickListener {
            listener(itemValue, holder.itemView, 0)
        }
        holder.itemView.tag = itemValue
        holder.itemView.item_title.text = parseTitle(holder.itemView.context, itemValue)
        holder.itemView.item_desc.text = itemValue.title
        holder.itemView.item_download.setOnClickListener {
            val cache = App.getVideoCacheModel(holder.itemView.context).getCache(itemValue)
            if(cache != null)
                DownloadService.download(holder.itemView.context, itemValue, cache.url)
            else
                ParseModel(holder.itemView.context).getVideo(itemValue){ url: String, loadStatus ->
                    if(loadStatus == Parser.LoadStatus.SUCCESS) {
                        DownloadService.download(holder.itemView.context, itemValue, url)
                        listener(itemValue, holder.itemView, 1)
                    }else
                        Snackbar.make(holder.itemView, R.string.load_video_failed, Snackbar.LENGTH_SHORT).show()
                }
        }
        holder.itemView.item_download.setOnLongClickListener {
            val cache = App.getVideoCacheModel(holder.itemView.context).getCache(itemValue)
            if(cache != null)
                DownloadService.remove(holder.itemView.context, itemValue)
            listener(itemValue, holder.itemView, 1)
            /*
            ParseModel(holder.itemView.context).getVideo(itemValue){ url: String, _ ->
                DownloadService.download(holder.itemView.context, itemValue.bangumi.parseString(), itemValue, url)
            }*/
            true
        }
        cachedThreadPool.execute{
            val downloader = App.getVideoCacheModel(holder.itemView.context).getDownloader(itemValue)
            holder.itemView.post {
                if(holder.itemView.tag == itemValue)
                    updateDownload(holder.itemView, downloader?.downloadPercentage?: Float.NaN, downloader?.downloadedBytes?:0L, downloader != null)
            }
        }
        updateSelect(holder.itemView)
    }

    companion object {
        fun updateSelect(itemView: View){
            val itemValue = (itemView.tag as? VideoInfo)?: return
            val playHistory = PlayHistoryModel(itemView.context).getPlayHistory(itemValue.bangumi)
            val color =ResourceUtil.resolveColorAttr(itemView.context,
                    when {
                        itemValue.select -> R.attr.colorPrimary
                        playHistory?.viewList?.contains(itemValue.id) == true ->
                            android.R.attr.textColorSecondaryNoDisable
                        else -> android.R.attr.textColorSecondary
                    })
            itemView.item_title.text = parseTitle(itemView.context, itemValue)
            itemView.item_title.setTextColor(color)
            itemView.item_desc.setTextColor(color)
        }

        fun updateDownload(itemView: View, percent: Float, bytes: Long, hasCache: Boolean, download: Boolean = false){
            if(hasCache && !VideoCacheModel.isFinished(percent)){
                itemView.item_progress.max = 10000
                itemView.item_progress.progress = (percent * 100).toInt()
                itemView.item_download_info.text = DownloadService.parseDownloadInfo(itemView.context, percent, bytes)
                itemView.item_progress.isEnabled = download
                itemView.item_progress.visibility = View.VISIBLE
            }else{
                itemView.item_download_info.text = if(hasCache) Formatter.formatFileSize(itemView.context, bytes) else ""
                itemView.item_progress.visibility = View.INVISIBLE
            }
            itemView.item_download.setImageResource(
                    if(VideoCacheModel.isFinished(percent)) R.drawable.ic_cloud_done else if(download) R.drawable.ic_pause else R.drawable.ic_download )
        }

        fun parseTitle(context: Context, itemValue: VideoInfo): String{
            return context.getString(R.string.av_title, itemValue.order)
        }
    }
}