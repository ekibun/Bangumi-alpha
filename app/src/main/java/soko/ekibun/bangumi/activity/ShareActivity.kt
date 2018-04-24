package soko.ekibun.bangumi.activity

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.jsoup.Jsoup
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.data.Bangumi
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.util.LoaderUtil
import android.content.Intent
import android.util.Log
import java.util.regex.Pattern


class ShareActivity : Activity() {
    private val parseModel: ParseModel by lazy{ ParseModel(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Intent.ACTION_SEND == intent.action) {
            val type = intent.type
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    val matcher = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]").matcher(sharedText)
                    if (matcher.find()) {
                        processUrl(matcher.group())
                    } else {
                        finish()
                    }
                }
            }
        }else if(intent.data != null){
            processUrl(intent.data.toString())
        }
    }

    fun processUrl(url: String){
        when {
            url.contains("dilidili.wang") -> {
                val vid = Regex("""dilidili.wang/anime/([^/]*)/""").find(url)?.groupValues?.get(1)
                if(vid != null) parseModel.getInfo(Bangumi(vid, Parser.SiteId.DILIDILI)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                    if(bangumi!= null)
                        DetailActivity.startActivity(this, bangumi)
                    finish()
                }else
                    finish()
            }
            url.contains("iqiyi.com") -> parseModel.loader.get(url){ bytes: ByteArray?, s: String, loadType: LoaderUtil.LoadType ->
                if(bytes!= null) try{
                    val vid = Regex("""albumId: ([0-9]*),""").find(String(bytes))?.groupValues?.get(1)
                    Log.v("vid", vid)
                    if(vid != null) parseModel.getInfo(Bangumi(vid, Parser.SiteId.IQIYI)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                        if(bangumi!= null)
                            DetailActivity.startActivity(this, bangumi)
                        finish()
                    }else
                        finish()
                }catch (e: Exception){
                    e.printStackTrace()
                    finish()
                }
                else
                    finish()
            }
            url.contains("qq.com") -> {
                val vid = Regex("""qq.com/detail/5/([^/.]*)""").find(url)?.groupValues?.get(1)?:
                    Regex("""qq.com/cover/g/([^/.]*)""").find(url)?.groupValues?.get(1)
                if(vid != null) parseModel.getInfo(Bangumi(vid, Parser.SiteId.TXVIDEO)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                    if(bangumi!= null)
                        DetailActivity.startActivity(this, bangumi)
                    finish()
                }else
                    finish()
            }
            url.contains("youku.com") -> {
                val vid = Regex("""youku.com/show/id_z([^/]*)""").find(url)?.groupValues?.get(1)
                if(vid != null) parseModel.getInfo(Bangumi(vid, Parser.SiteId.YOUKU)){ bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                    if(bangumi!= null)
                        DetailActivity.startActivity(this, bangumi)
                    finish()
                }else
                    finish()
            }
            url.contains("pptv.com") -> parseModel.loader.get(url) { bytes: ByteArray?, s: String, loadType: LoaderUtil.LoadType ->
                if(bytes!= null) try {
                    val vid = Regex(""""pid":([^,]*),""").find(String(bytes))?.groupValues?.get(1)
                    if (vid != null)
                        parseModel.getInfo(Bangumi(vid, Parser.SiteId.PPTV)) { bangumi: Bangumi?, _: Parser.LoadStatus, _: LoaderUtil.LoadType ->
                            if (bangumi != null)
                                DetailActivity.startActivity(this, bangumi)
                            finish()
                        }
                    else
                        finish()
                }catch (e: Exception){
                    e.printStackTrace()
                    finish()
                }
                else
                    finish()
            }
            else -> finish()
        }
    }
}
