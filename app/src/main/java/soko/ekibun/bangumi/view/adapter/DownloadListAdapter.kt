package soko.ekibun.bangumi.view.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.Formatter
import android.view.View
import kotlinx.android.synthetic.main.item_chase.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.model.data.VideoCache

class DownloadListAdapter(context: Context) : BaseAdapter<VideoCache>(R.layout.item_chase) {
    private val imageLoader by lazy{ App.getImageLoader(context) }
    private val videoCacheModel by lazy{ App.getVideoCacheModel(context) }

    override fun bindData(holder: BaseViewHolder, itemValue: VideoCache, position: Int, listener: (itemValue: VideoCache, view: View, position: Int) -> Unit) {
        holder.itemView.item_layout.setOnClickListener {
            listener(itemValue, holder.itemView, position)
        }
        holder.itemView.item_title.text = itemValue.bangumi.title
        holder.itemView.item_site.text = itemValue.bangumi.siteId.label
        Thread{

            val downloaderList = itemValue.videoList.map{ videoCacheModel.getDownloader(it.value.url) }
            var downloadBytes = 0L
            downloaderList.forEach {
                downloadBytes += it.downloadedBytes
            }
            holder.itemView.post {
                holder.itemView.item_phrase.text = parseDesc(downloaderList.filter { VideoCacheModel.isFinished(it.downloadPercentage) }.size, itemValue.videoList.size)
                holder.itemView.item_desc.text = Formatter.formatFileSize(holder.itemView.context, downloadBytes)
            }
        }.start()
        val poster = itemValue.bangumi.imgUrl
        if(holder.itemView.item_cover.tag != poster) {
            holder.itemView.item_cover.tag = poster
            holder.itemView.item_cover.setImageBitmap(null)
            imageLoader.get(poster) { bytes: ByteArray?, s: String, _ ->
                if (bytes == null || s != holder.itemView.item_cover.tag) return@get
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.itemView.item_cover.post { holder.itemView.item_cover.setImageBitmap(bitmap) }
            }
        }
        val onLayout = { _: View, left: Int, _:Int, right:Int, _:Int, oldLeft:Int, _:Int, oldRight:Int, _:Int ->
            if(right-left != oldRight-oldLeft)
                holder.itemView.item_title.maxWidth = holder.itemView.item_phrase.width - holder.itemView.item_site.width
        }
        holder.itemView.addOnLayoutChangeListener (onLayout)
        holder.itemView.item_site.addOnLayoutChangeListener (onLayout)
    }

    fun parseDesc(finishCount: Int, taskCount: Int): String {
        return "已完成 $finishCount 话/共 $taskCount 话"
    }
}