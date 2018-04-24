package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.PlayHistory
import soko.ekibun.bangumi.model.data.VideoCache
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.util.JsonUtil
import java.util.*

class PlayHistoryModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    private val lastPlayList = "lastViewList"
    private fun getLastViewList(): Map<String, PlayHistory>{
        return JsonUtil.toEntity(sp.getString(lastPlayList, JsonUtil.toJson(HashMap<String, PlayHistory>())),
                object : TypeToken<Map<String, PlayHistory>>() {}.type)
    }

    fun getPlayHistory(bangumi: Bangumi): PlayHistory?{
        return getLastViewList()[bangumi.parseString()]
    }

    fun setLastView(bangumi: Bangumi, video: VideoInfo, viewTime: Int){
        val editor = sp.edit()
        val set = HashMap<String, PlayHistory>()
        set += getLastViewList()
        set[bangumi.parseString()] = PlayHistory(video, viewTime, (getPlayHistory(bangumi)?: PlayHistory(video, viewTime, setOf(video.id))).viewList.plus(video.id))
        editor.putString(lastPlayList, JsonUtil.toJson(set))
        editor.apply()
    }
}