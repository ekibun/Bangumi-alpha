package soko.ekibun.bangumi.view

import android.content.Context
import android.graphics.Rect
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import soko.ekibun.bangumi.model.data.VideoInfo
import soko.ekibun.bangumi.view.adapter.VideoListPagerAdapter



class VideoListPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    var minHeight = 0

    var position = 0
    init{
        addOnPageChangeListener(object: OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(pos: Int) {
                position = pos
                requestLayout()
            }

        })
    }

    private val heightList = HashMap<Int, Int>()
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        (adapter as? VideoListPagerAdapter)?.viewList?.forEachIndexed{ index: Int, view: RecyclerView ->
            view.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            heightList[index] = view.measuredHeight
        }
        val height = Math.max(minHeight, heightList[currentItem]?:0)
        /*
        var height = minHeight
        for (i in 0 until childCount) {

            val child = getChildAt(i)
            child.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) height = h
        }*/
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    }

    fun getNext(avBean: VideoInfo): VideoInfo?{
        return (adapter as VideoListPagerAdapter).getNext(avBean)
    }

    fun loadAv(avBean: VideoInfo?){
        (adapter as VideoListPagerAdapter).loadAv(avBean)
    }
}