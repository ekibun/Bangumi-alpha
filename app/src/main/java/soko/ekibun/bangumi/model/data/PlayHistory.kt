package soko.ekibun.bangumi.model.data

data class PlayHistory(
        val lastView: VideoInfo,
        val lastViewTime: Int,
        val viewList: Set<String>
)