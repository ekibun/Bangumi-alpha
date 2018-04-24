package soko.ekibun.bangumi.fragment

import android.os.Bundle
import android.view.View
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.presenter.ChaseListPresenter

class ChaseFragment: DrawerFragment(R.layout.content_chase) {
    override val titleRes: Int = R.string.chase_bangumi
    var chasePresenter: ChaseListPresenter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chasePresenter = ChaseListPresenter(view.context, this)
        chasePresenter?.loadChaseList()
    }
}