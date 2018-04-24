package xiaolin.api.bean

data class LoginAuthsBean(
        val msg: String,
        val code: Int,
        val row: UserBean,
        val token: String
)