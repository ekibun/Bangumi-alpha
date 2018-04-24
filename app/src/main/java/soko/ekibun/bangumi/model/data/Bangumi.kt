package soko.ekibun.bangumi.model.data

import android.content.Context
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.util.JsonUtil
import xiaolin.api.bean.ChaseListBean
import java.util.*

data class Bangumi(
        val id:String,
        val siteId: Parser.SiteId,
        val title: String = "",
        val score:Float = 0f,
        val imgUrl:String = "",
        val url:String = "",
        val newOrder: Int = 0,
        val count: Int = 0,
        val region: String = "",
        @Transient val category: List<String> = ArrayList(),
        val updateTime: String = "",
        val latestTime: Long = 0,
        val permission: Int = 0): Comparable<Bangumi>{
    override fun compareTo(other: Bangumi): Int {
        return when {
            other.parseString() > parseString() -> 1
            other.parseString() == parseString() -> 0
            else -> -1
        }
    }

    fun formatPhrase(context: Context): String {
        return if (newOrder >= count && count != 0 && (updateTime.isEmpty() || Date().time - latestTime> 604800000) ) {
            context.getString(R.string.phrase_full, newOrder)
        } else {
            context.getString(R.string.phrase_updating, newOrder)
        }
    }

    fun formatCatagory(): String {
        var re=""
        for(i in category.indices)
            re += category[i] + " "
        return re
    }

    fun parseString(): String{
        return "$siteId,$id"
    }

    fun toChaseBean(): ChaseListBean.ChaseBean{
        return ChaseListBean.ChaseBean(
                siteId.id,
                id,
                JsonUtil.toJson(this),
                permission
        )
    }
}