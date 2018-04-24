package soko.ekibun.bangumi.presenter

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import kotlinx.android.synthetic.main.content_download.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.fragment.DownloadFragment
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.model.data.VideoCache
import soko.ekibun.bangumi.view.adapter.DownloadListAdapter

class DownloadListPresenter(context: Context, fragment: DownloadFragment){
    private val downloadListAdapter: DownloadListAdapter by lazy{ DownloadListAdapter(context) }
    private val videoCacheModel: VideoCacheModel by lazy{ App.getVideoCacheModel(context) }

    init{
        downloadListAdapter.onItemClickListener = { cacheInfo: VideoCache, _: View, _: Int ->
            DetailActivity.startActivity(context, cacheInfo.bangumi, true)
        }
        fragment.download_list.adapter = downloadListAdapter
        fragment.download_list.layoutManager = LinearLayoutManager(context)

        fragment.download_swipe.setOnRefreshListener {
            loadDownloadList()
            fragment.download_swipe.isRefreshing = false
        }
    }

    fun loadDownloadList() {
        downloadListAdapter.clearData()
        downloadListAdapter.addData(videoCacheModel.getVideoCacheList().values.toList())
    }
}