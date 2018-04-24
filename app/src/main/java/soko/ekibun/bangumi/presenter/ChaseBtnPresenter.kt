package soko.ekibun.bangumi.presenter

import android.support.constraint.ConstraintLayout
import kotlinx.android.synthetic.main.btn_chase.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ChaseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.util.LogUtil
import xiaolin.api.Api

class ChaseBtnPresenter(private val bangumi: Bangumi, private val btnChase: ConstraintLayout){
    private val chaseModel: ChaseModel by lazy{ ChaseModel(btnChase.context) }
    init{
        refreshChase()
        btnChase.setOnClickListener{view ->
            chaseModel.setChase(bangumi, !isChase){
                when(it){
                    Api.LoadStatus.SUCCESS -> refreshChase()
                    else -> LogUtil.showSnackbar(view, it.err, it.message)
                }
            }
        }
    }

    //init chase button
    private var isChase = false
    private fun refreshChase(){
        chaseModel.isChase(bangumi){
            isChase = it
            btnChase.post {
                if(isChase){
                    btnChase.tv_chase.text = btnChase.context.resources.getString(R.string.has_chase_bangumi)
                    btnChase.iv_chase.setImageDrawable(btnChase.context.resources.getDrawable(R.drawable.ic_heart, btnChase.context.theme))
                }else{
                    btnChase.tv_chase.text = btnChase.context.resources.getString(R.string.chase_bangumi)
                    btnChase.iv_chase.setImageDrawable(btnChase.context.resources.getDrawable(R.drawable.ic_heart_outline, btnChase.context.theme))
                }
            }
        }
    }
}