package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.util.Log
import com.tencent.connect.UserInfo
import com.tencent.connect.common.Constants
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.data.User
import soko.ekibun.util.JsonUtil
import java.lang.Exception
import xiaolin.api.Api
import xiaolin.api.bean.LoginAuthsBean

class UserModel(private val context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    private val tencent: Tencent by lazy{
        val tencent = Tencent.createInstance("1106777609", context.applicationContext)
        if(!openId.isEmpty())
            tencent.openId = openId
        tencent
    }

    private var openId get() = sp.getString("tencent_openid", "")
        set(v) {
            val editor = sp.edit()
            editor.putString("tencent_openid", v)
            editor.apply()
        }

    private fun saveOpenId(openId: String) {
        val editor = sp.edit()
        editor.putString("tencent_openid", openId)
        editor.apply()
    }

    private fun saveUser(user: User?) {
        val editor = sp.edit()
        if(user == null){
            editor.remove("user")
        }else{
            editor.putString("user", JsonUtil.toJson(user))
        }
        editor.apply()
    }

    private fun getLoginListener(context: Activity): UiListener{
        return UiListener(context){
            val loginInfo = JsonUtil.toJsonObject(it.toString())
            Log.v("complete", loginInfo.toString())
            if(loginInfo.get("ret").asInt == 0) try{
                val token = loginInfo.get(Constants.PARAM_ACCESS_TOKEN).asString
                val expires = loginInfo.get(Constants.PARAM_EXPIRES_IN).asString
                val openId = loginInfo.get(Constants.PARAM_OPEN_ID).asString
                saveOpenId(openId)
                tencent.openId = openId
                tencent.setAccessToken(token, expires)
                Api.loginAuthsQQ(token, openId){ loginAuthsInfo: LoginAuthsBean?, loadStatus: Api.LoadStatus ->
                    when {
                        loginAuthsInfo?.code == 0 -> UserInfo(context, tencent.qqToken).getUserInfo(UiListener(context) {
                            val userInfo = JsonUtil.toJsonObject(it.toString())
                            Log.v("complete", userInfo.toString())
                            if(userInfo.get("ret").asInt == 0) try{
                                val nickName = userInfo.get("nickname").asString
                                val figureUrl = userInfo.get("figureurl_2").asString
                                saveUser(User(loginAuthsInfo.token, nickName, figureUrl))
                                loginCallback(Api.LoadStatus.SUCCESS)
                            }catch (e: Exception){
                                loginCallback(Api.createErrMessage(Api.LoadStatus.ERROR, e.localizedMessage))
                                e.printStackTrace() }
                        })
                        loadStatus == Api.LoadStatus.SUCCESS -> loginCallback(Api.createErrMessage(Api.LoadStatus.ERROR, loadStatus.toString()))
                        else -> loginCallback(loadStatus)
                    }
                }
            }catch (e: Exception){
                loginCallback(Api.createErrMessage(Api.LoadStatus.ERROR, e.localizedMessage))
                e.printStackTrace() }
            else{
                loginCallback(Api.createErrMessage(Api.LoadStatus.ERROR, it.toString()))
            }
        }
    }

    private var loginCallback = {_:Api.LoadStatus->}

    fun login(activity: Activity, callback: (Api.LoadStatus)->Unit){
        loginCallback = callback
        tencent.login(activity, "get_simple_userinfo", getLoginListener(activity))
    }

    fun getUser(): User?{
        try{
            return JsonUtil.toEntity(sp.getString("user", ""), User::class.java)
        }catch (e: Exception){ e.printStackTrace() }
        return null
    }

    fun logout(callback: (Api.LoadStatus)->Unit) {
        getUser()?.let { Api.logout(it.token){
            if(it == Api.LoadStatus.SUCCESS) saveUser(null)
            callback(it)
        } }
    }

    fun processActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?){
        when(requestCode){
            Constants.REQUEST_LOGIN->
                Tencent.handleResultData(data, getLoginListener(activity))
        }
    }

    class UiListener(private val context: Activity, private val onCompleteListener:(Any?)->Unit): IUiListener {
        override fun onComplete(p0: Any?) {
            onCompleteListener(p0)
        }

        override fun onCancel() {
            Snackbar.make(context.window.decorView, context.getString(R.string.user_cancel), Snackbar.LENGTH_SHORT).show()
        }

        override fun onError(e: UiError) {
            Snackbar.make(context.window.decorView, e.errorDetail, Snackbar.LENGTH_SHORT).show()
        }

    }
}