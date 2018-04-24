@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.btn_chase.*
import kotlinx.android.synthetic.main.video_layout.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.PlayHistoryModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.presenter.ChaseBtnPresenter
import soko.ekibun.bangumi.presenter.SystemUIPresenter
import soko.ekibun.bangumi.presenter.VideoPresenter
import soko.ekibun.bangumi.view.adapter.VideoListPagerAdapter
import soko.ekibun.util.AppUtil
import soko.ekibun.util.JsonUtil
import com.tencent.connect.share.QQShare
import com.tencent.tauth.Tencent



class DetailActivity : AppCompatActivity() {
    private val bangumi: Bangumi by lazy {
        Log.v("bangumi", intent.getStringExtra(intentExtraBangumi))
        JsonUtil.toEntity(intent.getStringExtra(intentExtraBangumi), Bangumi::class.java) }
    private val imageLoader by lazy{ App.getImageLoader(this) }
    val videoPresenter: VideoPresenter by lazy { VideoPresenter(this) }
    val systemUIPresenter: SystemUIPresenter by lazy{ SystemUIPresenter(this) }

    var videoInfo: VideoInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val screenSize = AppUtil.getScreenSize(this)
        val lp = video_container.layoutParams as ConstraintLayout.LayoutParams
        lp.dimensionRatio = "h,${screenSize.height}:${screenSize.width}"

        //init actionbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //init chase
        ChaseBtnPresenter(bangumi, cl_chase)

        //init data
        title_text.text = bangumi.title
        title_site.text = bangumi.siteId.label
        imageLoader.get(bangumi.imgUrl) { bytes: ByteArray?, _: String, _ ->
            if(bytes == null) return@get
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            iv_cover.post{iv_cover.setImageBitmap(bitmap)}
        }

        //init tab
        tab_layout.setupWithViewPager(viewpager)
        viewpager.adapter = VideoListPagerAdapter(intent.getBooleanExtra(intentExtraUseCache, false), bangumi, this){ avBean: VideoInfo, _ ->
            videoInfo = avBean
            PlayHistoryModel(this).setLastView(bangumi, avBean, 0)
            videoPresenter.loadVideo(avBean)
        }

        app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            toolbar.visibility = if(verticalOffset != 0 || videoPresenter.controller.isShow || video_surface_container.visibility != View.VISIBLE) View.VISIBLE else View.INVISIBLE
        }
        viewpager.post {
            viewpager.minHeight = root_layout.height - toolbar.height - tab_layout.height
        }
        registerReceiver(receiver, IntentFilter(bangumi.parseString()))

        systemUIPresenter.init()
    }

    private val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            (viewpager.adapter as? VideoListPagerAdapter)?.onReceive(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        videoInfo?.let{
            videoPresenter.doPlayPause(true)
        }
    }

    override fun onStop() {
        super.onStop()
        App.flushCache(this)
        videoInfo?.let{
            PlayHistoryModel(this).setLastView(bangumi, it,
                    videoPresenter.videoModel.player.currentPosition.toInt())
            videoPresenter.doPlayPause(false)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        onMultiWindowModeChanged((Build.VERSION.SDK_INT >=24 && isInMultiWindowMode), newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        systemUIPresenter.onWindowModeChanged(isInMultiWindowMode, newConfig)
        if(video_surface_container.visibility == View.VISIBLE)
            videoPresenter.controller.doShowHide(false)
        viewpager.minHeight = 0
        viewpager.requestLayout()
        viewpager.post {
            viewpager.minHeight = root_layout.height - toolbar.height - tab_layout.height
            viewpager.requestLayout()
        }
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(systemUIPresenter.isLandscape && videoPresenter.videoModel.player.playWhenReady && Build.VERSION.SDK_INT >= 24) {
            enterPictureInPictureMode()
        }
    }

    //back
    private fun processBack(){
        if (systemUIPresenter.isLandscape)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            finish()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_share -> share()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_detail, menu)
        return true
    }

    fun share(){
        val params = Bundle()
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT)
        params.putString(QQShare.SHARE_TO_QQ_TITLE, bangumi.title)// 标题
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, bangumi.imgUrl);
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, bangumi.url)// 内容地址
        Tencent.createInstance("1106777609", applicationContext).shareToQQ(this, params, null)
    }

    companion object{
        private const val intentExtraBangumi = "extraBangumi"
        private const val intentExtraUseCache = "extraUseCache"

        fun startActivity(context: Context, bangumi: Bangumi, useCache: Boolean = false) {
            context.startActivity(parseIntent(context, bangumi, useCache))
        }

        fun parseIntent(context: Context, bangumi: Bangumi, useCache: Boolean = false): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(intentExtraBangumi, JsonUtil.toJson(bangumi))
            intent.putExtra(intentExtraUseCache, useCache)
            return intent
        }
    }
}
