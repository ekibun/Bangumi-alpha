package soko.ekibun.bangumi.view.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import kotlinx.android.synthetic.main.item_search.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.data.Bangumi

class SearchListAdapter(context: Context) : BaseAdapter<Bangumi>(R.layout.item_search) {
    private val imageLoader by lazy{ App.getImageLoader(context) }

    override fun bindData(holder: BaseViewHolder, itemValue: Bangumi, position: Int, listener: (Bangumi, View, Int) -> Unit) {
        holder.itemView.item_layout.setOnClickListener {
            listener(itemValue, holder.itemView, position)
        }
        holder.itemView.item_title.text = itemValue.title
        holder.itemView.item_site.text = itemValue.siteId.label
        holder.itemView.item_region.text = itemValue.region
        holder.itemView.item_category.text = itemValue.formatCatagory()
        holder.itemView.item_score.text = itemValue.score.toString()
        val poster = itemValue.imgUrl
        holder.itemView.item_cover.tag = poster
        holder.itemView.item_cover.setImageBitmap(null)

        val onLayout = { _: View, left: Int, _:Int, right:Int, _:Int, oldLeft:Int, _:Int, oldRight:Int, _:Int ->
            if(right-left != oldRight-oldLeft)
                holder.itemView.item_title.maxWidth = holder.itemView.item_region.width - holder.itemView.item_site.width
        }
        holder.itemView.addOnLayoutChangeListener (onLayout)
        holder.itemView.item_site.addOnLayoutChangeListener (onLayout)

        imageLoader.get(poster) { bytes: ByteArray?, s: String, _ ->
            if (bytes == null || s != holder.itemView.item_cover.tag) return@get
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (s == holder.itemView.item_cover.tag as String) {
                holder.itemView.item_cover.post{ holder.itemView.item_cover.setImageBitmap(bitmap) }
            }
        }
    }
}