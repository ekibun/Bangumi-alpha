package xiaolin.api

import android.support.annotation.StringRes
import android.util.Log
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil
import xiaolin.api.bean.*

object Api{
    private const val hostLink = "http://www.xxxlin.com/bangumi/api"
    private const val publicKeyLink = "$hostLink/user/publickey"
    private const val loginAuthsLink = "$hostLink/user/loginbyauths"
    private const val logoutLink = "$hostLink/user/logout"
    private const val chaseListLink = "$hostLink/chase/list"
    private const val chaseAddLink = "$hostLink/chase/add"
    private const val chaseDeleteLink = "$hostLink/chase/delete"

    private var publicKey: PublicKeyBean? = null
    private val loader by lazy{ LoaderUtil() }
    private fun getPublicKey(callback: (PublicKeyBean?, LoadStatus)->Unit){
        if(publicKey != null)
            callback(publicKey!!, LoadStatus.SUCCESS)
        else
            loader.post(Api.publicKeyLink){ s: String, loadStatus: Parser.LoadStatus ->
                if(loadStatus == Parser.LoadStatus.SUCCESS) try{
                    Log.v("Api", "publicKey:$s")
                    publicKey = JsonUtil.toEntity(s, PublicKeyBean::class.java)
                    callback(publicKey!!, LoadStatus.SUCCESS)
                }catch (e: Exception){
                    callback(null, createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                    e.printStackTrace() }
                else{
                    callback(null, createErrMessage(LoadStatus.NETWORK, s))
                }
            }
    }

    fun loginAuthsQQ(token: String, openId: String, callback: (LoginAuthsBean?, Api.LoadStatus)->Unit){
        getPublicKey { publicKeyBean: PublicKeyBean?, loadStatus: LoadStatus ->
            if (loadStatus == LoadStatus.SUCCESS) try {
                loader.post(Api.loginAuthsLink, publicKeyBean!!.encryptQQ(token, openId), getHeader()) { s: String, status: Parser.LoadStatus ->
                    if (status == Parser.LoadStatus.SUCCESS) try {
                        Log.v("Api", "loginAuths:$s")
                        callback(JsonUtil.toEntity(s, LoginAuthsBean::class.java), LoadStatus.SUCCESS)
                    } catch (e: Exception) {
                        callback(null, createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                        e.printStackTrace()
                    }
                    else {
                        callback(null, createErrMessage(LoadStatus.NETWORK, s))
                    }
                }
            } catch (e: Exception) {
                callback(null, createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace()
            }
            else {
                callback(null, loadStatus)
            }
        }
    }

    enum class LoadStatus(@StringRes val err: Int, var message: String =""){
        SUCCESS(0), NOT_LOGIN(R.string.err_not_login), ERROR(R.string.err_load), NETWORK(R.string.err_network)
    }

    fun createErrMessage(status: LoadStatus, message: String): LoadStatus{
        status.message = message
        return status
    }

    private fun getHeader(token: String? = null): Map<String,String>{
        val map = HashMap<String,String>()
        token?.let{ map["authorization"] = it}
        map["Content-Type"]="application/json"
        return map
    }

    fun logout(token: String, callback: (LoadStatus)->Unit){
        loader.post(Api.logoutLink, "", getHeader(token)){ s: String, loadStatus: Parser.LoadStatus ->
            if(loadStatus == Parser.LoadStatus.SUCCESS) try{
                Log.v("Api", "logout:$s")
                callback(if(JsonUtil.toJsonObject(s).get("code").asInt == 0)LoadStatus.SUCCESS else createErrMessage(LoadStatus.ERROR, s))
            }catch (e: Exception){
                callback(createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace() }
            else{
                callback(createErrMessage(LoadStatus.NETWORK, s))
            }
        }
    }

    fun getChaseList(token: String, callback: (ChaseListBean?, LoadStatus)->Unit){
        loader.post(Api.chaseListLink, "", getHeader(token)){ s: String, loadStatus: Parser.LoadStatus ->
            if(loadStatus == Parser.LoadStatus.SUCCESS) try{
                Log.v("Api", "getChaseList:$s")
                callback(JsonUtil.toEntity(s, ChaseListBean::class.java), LoadStatus.SUCCESS)
            }catch (e: Exception){
                callback(null, createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace() }
            else{
                callback(null, createErrMessage(LoadStatus.NETWORK, s))
            }
        }
    }

    fun addChase(token: String, chaseBean: ChaseListBean.ChaseBean, callback: (LoadStatus)->Unit){
        loader.post(Api.chaseAddLink, JsonUtil.toJson(chaseBean), getHeader(token)){ s: String, loadStatus: Parser.LoadStatus ->
            if(loadStatus == Parser.LoadStatus.SUCCESS) try{
                Log.v("Api", "addChase:$s")
                callback(if(JsonUtil.toJsonObject(s).get("code").asInt == 0)LoadStatus.SUCCESS else LoadStatus.ERROR)
            }catch (e: Exception){
                callback(createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace() }
            else{
                callback(createErrMessage(LoadStatus.NETWORK, s))
            }
        }
    }

    fun deleteChase(token: String, chaseBean: ChaseListBean.ChaseBean, callback: (LoadStatus)->Unit){
        loader.post(Api.chaseDeleteLink, JsonUtil.toJson(chaseBean), getHeader(token)){ s: String, loadStatus: Parser.LoadStatus ->
            if(loadStatus == Parser.LoadStatus.SUCCESS) try{
                Log.v("Api", "deleteChase:$s")
                callback(if(JsonUtil.toJsonObject(s).get("code").asInt == 0)LoadStatus.SUCCESS else LoadStatus.ERROR)
            }catch (e: Exception){
                callback(createErrMessage(LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace() }
            else{
                callback(createErrMessage(LoadStatus.NETWORK, s))
            }
        }
    }
}