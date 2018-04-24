package soko.ekibun.bangumi.model.data;

data class VideoCache (
        val bangumi: Bangumi,
        val videoList: Map<String, VideoCacheBean>
){
    data class VideoCacheBean (
            val video: VideoInfo,
            val url: String
    )
}
