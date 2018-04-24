package soko.ekibun.bangumi.fragment

import android.os.Bundle
import android.view.View
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.presenter.DownloadListPresenter

class DownloadFragment: DrawerFragment(R.layout.content_download) {
    override val titleRes: Int = R.string.download_cache

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DownloadListPresenter(view.context, this).loadDownloadList()
    }
}