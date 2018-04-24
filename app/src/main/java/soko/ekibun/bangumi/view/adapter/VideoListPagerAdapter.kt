package soko.ekibun.bangumi.view.adapter

import android.content.Intent
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_detail.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.activity.DetailActivity
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.VideoCache
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LogUtil

class VideoListPagerAdapter(var showCache: Boolean, internal var bangumi: Bangumi, private val context: DetailActivity, listener: (itemValue: VideoInfo, view: View?)->Unit) : PagerAdapter() {
    val viewList = ArrayList<RecyclerView>()
    private val adapters = ArrayList<VideoListAdapter>()

    private val parseModel: ParseModel by lazy{ ParseModel(context) }

    private val pageSize = 100

    fun onReceive(intent: Intent?) {
        if(intent != null) try{
            val video = JsonUtil.toEntity(intent.getStringExtra("video"), VideoInfo::class.java)
            val percent = intent.getFloatExtra("percent", Float.NaN)
            val bytes = intent.getLongExtra("bytes", 0L)
            updateView(video){
                VideoListAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra("cancel", true), !intent.hasExtra("cancel"))
                false }
            if(showCache && !intent.getBooleanExtra("cancel", true)){
                loadCacheList()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private var selectedItem: VideoInfo? = null
    private val onClickListener = { itemValue: VideoInfo, view: View?, flag:Int ->
        if(flag == 0){
            listener(itemValue, view)

            selectedItem?.let{
                setSelect(it, false)
                updateView(it){
                    VideoListAdapter.updateSelect(it)
                    false }
            }
            setSelect(itemValue, true)
            updateView(itemValue){
                VideoListAdapter.updateSelect(it)
                false }

            selectedItem = itemValue
        }else if(showCache){
            view?.post { loadCacheList() }
        }
        Unit
    }

    private fun updateView(avBean: VideoInfo, callback: (View)->Boolean){
        viewList.forEach {
            for(i in 0 until it.childCount){
                val v = it.getChildAt(i)
                if((v?.tag as? VideoInfo)?.id == avBean.id)
                    if(callback(v)) return
            }
        }
    }

    private fun setSelect(avBean: VideoInfo, select: Boolean){
        adapters.getOrNull(avBean.order/pageSize + if(!showCache) 0 else 1)?.valueList?.let {
            it.forEach {
                if(it.id == avBean.id)
                    it.select = select
            }
        }
        if(showCache){
            adapters[0].valueList.forEach {
                if (it.id == avBean.id)
                    it.select = select
            }
        }
    }

    fun getNext(avBean: VideoInfo): VideoInfo?{
        adapters.getOrNull(avBean.order/pageSize + if(!showCache) 0 else 1)?.valueList?.let {
            it.forEachIndexed { index, videoInfo ->
                if(videoInfo.id == avBean.id) return it.getOrNull(index+1)
                if(videoInfo.order == avBean.order + 1) return it.getOrNull(index)
            }
        }
        return null
    }
/*
    private fun getView(avBean: VideoInfo): View?{
        viewList.getOrNull((avBean.order -1)/pageSize + if(!showCache) 0 else 1)?.let {
            for(i in 0 until it.childCount){
                val v = it.getChildAt(i)
                if((v?.tag as? VideoInfo)?.id == avBean.id) return v
            }
        }
        return null
    }*/

    fun loadAv(avBean: VideoInfo?) {
        if (avBean != null)
            updateView(avBean){
                onClickListener(avBean, it, 0)
                true
            }
    }

    private var videoCache: VideoCache? = App.getVideoCacheModel(context).getBangumiVideoCacheList(bangumi)

    init {
        for (i in 0 until count) {
            val view = RecyclerView(context)
            val adapter = VideoListAdapter()
            adapter.onItemClickListener = onClickListener
            view.adapter = adapter
            view.layoutManager = LinearLayoutManager(context)
            view.isNestedScrollingEnabled = false
            this.adapters.add(adapter)
            this.viewList.add(view)
        }
        context.video_list_swipe.setOnRefreshListener {
            loadAvList(context.viewpager.currentItem)
        }
    }

    override fun getCount(): Int {
        return  bangumi.newOrder / pageSize + if(!showCache) 1 else 2
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val v = viewList[position]
        container.addView(v)
        if (v.tag == null) {
            v.tag = true
            loadAvList(position)
        }
        return v
    }

    private fun loadCacheList(){
        videoCache = App.getVideoCacheModel(context).getBangumiVideoCacheList(bangumi)
        adapters[0].clearData()
        if(videoCache != null){
            adapters[0].addData(videoCache!!.videoList.values.toList().map{
                val video = it.video
                video.bangumi = bangumi
                video
            }.sortedBy { it.order })
        }
    }

    private fun loadAvList(pos: Int) {
        val position = if(!showCache){
            pos
        }else{
            if(pos == 0) {
                loadCacheList()
                if(context.viewpager.currentItem == 0)
                    context.video_list_swipe.isRefreshing = false
                return
            }
            pos - 1
        }
        if(context.viewpager.currentItem == pos)
            context.video_list_swipe.isRefreshing = true
        parseModel.getVideoList(bangumi, position + 1, pageSize) { list: List<VideoInfo>?, _, loadStatus: Parser.LoadStatus ->
            Log.v("video", list.toString())
            viewList[context.viewpager.currentItem].post {
                if(context.viewpager.currentItem == pos)
                    context.video_list_swipe.isRefreshing = false
                when (loadStatus) {
                    Parser.LoadStatus.SUCCESS ->
                        if (list != null){
                            adapters[pos].clearData()
                            adapters[pos].addData(list)
                        }
                    else -> LogUtil.showSnackbar(viewList[context.viewpager.currentItem], R.string.err_load, list.toString())
                }
            }
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(pos: Int): CharSequence {
        val position = if(!showCache){
            pos
        }else{
            if(pos == 0) return "已缓存"
            pos - 1
        }
        return (position * 100 + 1).toString() + "-" + if ((position + 1) * pageSize > bangumi.newOrder) bangumi.newOrder else (position + 1) * 100
    }
}