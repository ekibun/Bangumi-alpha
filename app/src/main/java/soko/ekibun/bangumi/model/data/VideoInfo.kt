package soko.ekibun.bangumi.model.data

import android.content.Context
import soko.ekibun.bangumi.R

data class VideoInfo(
        val id:String,
        @Transient var bangumi: Bangumi,
        val order: Int,
        val title: String,
        val url:String,
        var duration: Int,
        @Transient var select: Boolean = false
) {
    fun parseTitle(context: Context): String {
        return context.getString(R.string.av_title, order) + " - " + title
    }
}