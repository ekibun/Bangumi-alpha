package xiaolin.api.bean

data class ChaseListBean(
        val msg: String,
        val total: Int,
        val code: Int,
        val rows: List<ChaseBean>
){
    data class ChaseBean(
            val platform: Int,
            val num: String,
            val chaseName: String,
            val permission: Int
    )
}