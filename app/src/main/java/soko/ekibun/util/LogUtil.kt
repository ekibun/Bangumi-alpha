package soko.ekibun.util

import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View

object LogUtil{
    /*
    fun showSnackbar(view: View, @StringRes info: Int, duration: Int = Snackbar.LENGTH_SHORT){
        Snackbar.make(view, info, duration).show()
    }*/

    fun showSnackbar(view: View, @StringRes info: Int, message: String, duration: Int = Snackbar.LENGTH_SHORT){
        Snackbar.make(view, view.context.getString(info) + "\n" + message, duration).show()
    }
}