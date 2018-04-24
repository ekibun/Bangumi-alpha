package xiaolin.api.bean

import android.util.Log
import xiaolin.Util.RSAUtil

data class PublicKeyBean(
        val msg: String,
        val code: Int,
        val publickey: String
){
    fun encryptQQ(token: String, openId: String): String{
        val body = "1\n" +
                "${System.currentTimeMillis()}\n" +
                "qq\n" +
                "$openId\n" +
                token
        Log.v("encryptQQ", body)
        return RSAUtil.encryptToBase64(publickey, body.toByteArray())
    }
}