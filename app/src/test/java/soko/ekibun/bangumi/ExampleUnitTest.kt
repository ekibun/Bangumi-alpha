package soko.ekibun.bangumi

import org.junit.Test

import org.junit.Assert.*
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.LoaderUtil

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        //assertEquals(4, 2 + 2)
        LoaderUtil().get("https://www.google.com.hk/search?&q=${java.net.URLEncoder.encode("魔法少女网站", "utf-8")}+site%3Adilidili.wang"){ bytes: ByteArray?, s: String, loadType: LoaderUtil.LoadType ->
            if(bytes!= null){
                val html = String(bytes)
                val json = JsonUtil.toJsonObject("{data: [" + html.substringAfter("var m=[").substringBefore("];") + "]}")
                print(json.toString())
            }
        }
    }
}
