package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.util.Log
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.util.JsonUtil
import xiaolin.api.Api
import xiaolin.api.bean.ChaseListBean


class ChaseModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    val userModel by lazy { UserModel(context) }

    fun getChaseList(callback: (Set<Bangumi>, Api.LoadStatus)->Unit){
        val user = userModel.getUser()
        if(user != null) {
            Api.getChaseList(user.token){ chaseListBean: ChaseListBean?, loadStatus: Api.LoadStatus ->
                val set = HashSet<Bangumi>()
                chaseListBean?.rows?.forEach {
                    try{
                        set += JsonUtil.toEntity(it.chaseName, Bangumi::class.java)
                    }catch (e: Exception){ }
                }
                callback(set, loadStatus)
            }
        }
    }

    fun isChase(bangumi: Bangumi, callback: (Boolean)->Unit) {
        getChaseList{ set: Set<Bangumi>, _:  Api.LoadStatus ->
            set.forEach {
                if(it.parseString() == bangumi.parseString()) {
                    callback(true)
                    return@getChaseList
                }
            }
            callback(false)
        }
    }

    fun setChase(bangumi: Bangumi, chase: Boolean, callback: ( Api.LoadStatus)->Unit) {
        val user = userModel.getUser()
        if(user != null) {
            if(chase){
                Api.addChase(user.token, bangumi.toChaseBean()){
                    callback(it)
                }
            }else{
                Api.deleteChase(user.token, bangumi.toChaseBean()){
                    callback(it)
                }
            }
        }else
            callback( Api.LoadStatus.NOT_LOGIN)
    }
}