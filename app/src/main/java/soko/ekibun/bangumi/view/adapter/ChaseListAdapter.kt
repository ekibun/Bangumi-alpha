package soko.ekibun.bangumi.view.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import kotlinx.android.synthetic.main.item_chase.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.PlayHistoryModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.util.ResourceUtil


class ChaseListAdapter(context: Context) : BaseAdapter<Bangumi>(R.layout.item_chase) {
    private val imageLoader by lazy{ App.getImageLoader(context) }
    private val lastViewModel = PlayHistoryModel(context)

    override fun getItemId(position: Int): Long {
        return valueList[position].parseString().hashCode().toLong()
    }

    override fun addData(value: Bangumi): Int {
        var index = indexOf(value)
        if (index == -1) {
            this.valueList.add(value)
            valueList.sort()
            index = indexOf(value)
        } else {
            this.valueList[index] = value
        }
        notifyDataSetChanged()
        return index
    }

    private fun indexOf(value: Bangumi): Int {
        return valueList.indices.firstOrNull { valueList[it].id == value.id } ?: -1
    }

    override fun bindData(holder: BaseViewHolder, itemValue: Bangumi, position: Int, listener: (itemValue: Bangumi, view: View, position: Int) -> Unit) {
        holder.itemView.item_layout.setOnClickListener {
            listener(itemValue, holder.itemView, position)
        }
        holder.itemView.item_title.text = itemValue.title
        holder.itemView.item_site.text = itemValue.siteId.label
        holder.itemView.item_phrase.text = itemValue.formatPhrase(holder.itemView.context)
        val lastView = lastViewModel.getPlayHistory(itemValue)
        holder.itemView.item_desc.text = parseDesc(itemValue.updateTime, lastView?.lastView?.order)
        val poster = itemValue.imgUrl
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

        holder.itemView.item_phrase.setTextColor(ResourceUtil.resolveColorAttr(holder.itemView.context,
                if ((lastView?.lastView?.order?:0) < itemValue.newOrder) R.attr.colorPrimary else android.R.attr.textColorSecondary))
    }

    private fun parseDesc(updateTime: String, lastView: Int?): String {
        return (if(updateTime.isEmpty())"" else (updateTime + "\n"))+if (lastView == null) "尚未观看" else "看到第 $lastView 话"
    }
}