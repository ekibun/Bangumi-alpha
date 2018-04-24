package soko.ekibun.bangumi.presenter

import android.graphics.BitmapFactory
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.Menu
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.activity.MainActivity
import soko.ekibun.bangumi.activity.SettingsActivity
import soko.ekibun.bangumi.fragment.*
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.util.LoaderUtil
import soko.ekibun.util.LogUtil
import xiaolin.api.Api

class DrawerPresenter(private val context: MainActivity){
    private val imageLoader by lazy{ App.getImageLoader(context) }
    val userModel by lazy{ UserModel(context) }

    private val headerView by lazy{context.nav_view.getHeaderView(0)}

    init{
        context.setSupportActionBar(context.toolbar)
        val toggle = ActionBarDrawerToggle(context, context.drawer_layout, context.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        context.drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        context.nav_view.setNavigationItemSelectedListener {
            context.drawer_layout.closeDrawers()
            if(fragments.containsKey(it.itemId))
                select(it.itemId)
            else{
                when(it.itemId){
                    R.id.nav_setting -> SettingsActivity.startActivity(context)
                }
            }
            true }

        refreshUser()

        headerView.user_figure.setOnClickListener {view->
            if(userModel.getUser() == null){
                userModel.login(context){
                    if(it == Api.LoadStatus.SUCCESS)
                        refreshUser()
                    else
                        LogUtil.showSnackbar(view, it.err,it.message) }
            }else{
                userModel.logout{
                    if(it == Api.LoadStatus.SUCCESS)
                        refreshUser()
                    else
                        LogUtil.showSnackbar(view, it.err,it.message) }
            }
        }
    }

    private fun refreshUser(){
        val user = userModel.getUser()
        Log.v("refreshUser", user.toString())
        if(user == null){
            headerView.post {
                headerView.user_figure.setImageResource(R.drawable.akkarin)
                headerView.user_name.text = "点击头像登录"
                loadChaseList()
            }
        }else{
            headerView.post {
                headerView.user_name.text = user.nickName
                loadChaseList()
            }
            imageLoader.get(user.figureUrl){ bytes: ByteArray?, _: String, _: LoaderUtil.LoadType ->
                if(bytes == null) return@get
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                headerView.post {
                    headerView.user_figure.setImageBitmap(bitmap)
                }
            }
        }
    }

    fun loadChaseList() {
        (fragments[R.id.nav_chase] as? ChaseFragment)?.chasePresenter?.loadChaseList()
    }

    private val fragments: Map<Int, Fragment> = mapOf(
            R.id.nav_chase to ChaseFragment(),
            R.id.nav_download to DownloadFragment()
    )

    fun onPrepareOptionsMenu(menu: Menu?) {
        when(checkedId){
            R.id.nav_chase -> menu?.findItem(R.id.action_search)?.isVisible = true
            else -> menu?.findItem(R.id.action_search)?.isVisible = false
        }
    }

    private var checkedId = 0
    fun select(id: Int){
        checkedId = id
        context.supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragments[id]).commit()
        context.nav_view.setCheckedItem(id)
        context.invalidateOptionsMenu()
    }
}