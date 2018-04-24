package xiaolin.api.bean

data class UserBean(
        val openid: String,
        val uid: Long,
        val nick: String,
        val sex: Int,
        val createTime: Long,
        val updateTime: Long
)