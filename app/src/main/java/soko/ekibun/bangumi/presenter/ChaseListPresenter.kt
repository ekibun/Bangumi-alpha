package soko.ekibun.bangumi.presenter

import soko.ekibun.bangumi.fragment.ChaseFragment
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import kotlinx.android.synthetic.main.content_chase.*
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.model.ChaseModel
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.view.adapter.ChaseListAdapter
import soko.ekibun.util.LoaderUtil
import soko.ekibun.util.LogUtil
import xiaolin.api.Api

class ChaseListPresenter(context: Context, private val fragment: ChaseFragment){
    private val chaseListAdapter: ChaseListAdapter by lazy{ ChaseListAdapter(context) }
    private val chaseModel: ChaseModel by lazy{ ChaseModel(context) }
    private val parseModel: ParseModel by lazy{ ParseModel(context) }

    init{
        chaseListAdapter.onItemClickListener = { bangumi: Bangumi, _: View, _: Int ->
            DetailActivity.startActivity(context, bangumi)
        }
        fragment.chase_list.adapter = chaseListAdapter
        fragment.chase_list.layoutManager = LinearLayoutManager(context)

        fragment.chase_swipe.setOnRefreshListener {
            loadChaseList()
        }
    }

    fun loadChaseList() {
        chaseListAdapter.clearData()

        chaseModel.getChaseList{ set: Set<Bangumi>, status: Api.LoadStatus ->
            when(status){
                Api.LoadStatus.SUCCESS ->{
                    var count = set.size
                    if(count == 0)
                        fragment.chase_swipe?.post { fragment.chase_swipe?.isRefreshing = false }
                    set.forEach {
                        fragment.chase_list.post {
                            chaseListAdapter.addData(it)
                            fragment.chase_swipe?.isRefreshing = true

                            parseModel.getInfo(it) { bangumiBean: Bangumi?, loadStatus: Parser.LoadStatus, loadType: LoaderUtil.LoadType ->
                                try{
                                    if (loadType == LoaderUtil.LoadType.INTERNET) {
                                        count--
                                        if (count == 0)
                                            fragment.chase_swipe?.post { fragment.chase_swipe?.isRefreshing = false } }
                                    if (loadStatus == Parser.LoadStatus.SUCCESS && bangumiBean != null) {
                                        fragment.chase_list?.post { chaseListAdapter.addData(bangumiBean) } }
                                }catch (e: Exception){
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                else -> {
                    fragment.chase_swipe?.post { fragment.chase_swipe?.isRefreshing = false }
                    fragment.chase_swipe?.let{ LogUtil.showSnackbar(it, status.err, status.message) }
                }
            }
        }
        /*
        val set = chaseModel.getChaseList()
        var count = set.size
        fragment.chase_swipe?.isRefreshing = false
        for (bangumi in set) {
            fragment.chase_swipe?.isRefreshing = true
            parseModel.getInfo(bangumi) { bangumiBean: Bangumi?, loadStatus: Parser.LoadStatus, loadType: LoaderUtil.LoadType ->
                try{
                    if (loadType == LoaderUtil.LoadType.INTERNET) {
                        count--
                        if (count == 0)
                            fragment.chase_swipe?.post { fragment.chase_swipe?.isRefreshing = false } }
                    if (loadStatus == Parser.LoadStatus.SUCCESS && bangumiBean != null) {
                        fragment.chase_list?.post { chaseListAdapter.addData(bangumiBean) } }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }*/
    }
}