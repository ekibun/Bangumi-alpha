package soko.ekibun.bangumi.presenter

import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import kotlinx.android.synthetic.main.activity_search.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.activity.SearchActivity
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.view.adapter.SearchListAdapter
import soko.ekibun.util.LogUtil

class SearchPresenter(private val context: SearchActivity) {
    private val searchListAdapter: SearchListAdapter by lazy{ SearchListAdapter(context) }
    private val parseModel: ParseModel by lazy{ ParseModel(context) }

    init{
        searchListAdapter.onItemClickListener = { bangumi: Bangumi, _: View, _: Int ->
            DetailActivity.startActivity(context, bangumi)
        }
        context.search_list.adapter = searchListAdapter
        context.search_list.layoutManager = LinearLayoutManager(context)

        context.search_box.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                search(s.toString())
            }
        })

        context.search_swipe.setOnRefreshListener {
            search()
        }
    }

    private var lastKey = ""
    fun search(key: String = lastKey){
        lastKey = key
        searchListAdapter.clearData()
        if(key.isEmpty()){
            context.search_swipe.isRefreshing = false
        }else{
            context.search_swipe.isRefreshing = true
            parseModel.search(key) { list: List<Bangumi>?, s: String, status: Parser.LoadStatus ->
                if (s == lastKey)
                    context.search_list.post{
                        if(list!=null) searchListAdapter.addData(list)
                        if(status != Parser.LoadStatus.SUCCESS) {
                            context.search_swipe?.isRefreshing = false
                            if (status != Parser.LoadStatus.COMPLETE){
                                context.search_list?.post{ LogUtil.showSnackbar(context.search_list, R.string.err_load, s) }
                            }
                        }
                    }
            }
        }
    }
}