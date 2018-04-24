package soko.ekibun.bangumi.view.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.*

abstract class BaseAdapter<T>(private val layoutRes: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var valueList: MutableList<T> = ArrayList()
    var onItemClickListener= {_: T, _: View, _: Int->}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BaseViewHolder(parent.context, parent, layoutRes)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindData(holder as BaseViewHolder, valueList[position], position, onItemClickListener)
    }

    fun addData(valueList: List<T>) {
        this.valueList.addAll(valueList)
        notifyItemChanged(this.valueList.size - valueList.size)
        notifyItemRangeInserted(this.valueList.size - valueList.size + 1, valueList.size - 1)
    }

    open fun addData(value: T): Int {
        this.valueList.add(value)
        notifyItemInserted(itemCount - 1)
        return valueList.size - 1
    }

    fun setData(value: T, position: Int): Int {
        this.valueList[position] = value
        notifyItemChanged(position)
        notifyItemChanged(itemCount - 1)
        return valueList.size - 1
    }

    fun clearData() {
        this.valueList = ArrayList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return valueList.size
    }

    abstract fun bindData(holder: BaseViewHolder, itemValue: T, position: Int, listener: ((itemValue: T, view: View, flag: Int)->Unit))
    class BaseViewHolder(context: Context, root: ViewGroup, layoutRes: Int) : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(layoutRes, root, false))
}
