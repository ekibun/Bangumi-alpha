package soko.ekibun.util

import android.content.Context
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.util.LruCache
import libcore.io.DiskLruCache
import soko.ekibun.bangumi.parser.Parser
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Executors

class LoaderUtil(
        private val flag: Int = 0,
        context: Context? = null,
        uniName: String = ""){
    //flag
    private val useCache: Boolean get() = (flag and FLAG_USE_CACHE) != 0
    private val alwaysRequest: Boolean get() = (flag and FLAG_ALWAYS_REQUEST) != 0
    //cache
    private val cachedThreadPool = Executors.newFixedThreadPool(CACHE_THREAD_POOL_SIZE)
    private val memoryCache: LruCache<String, ByteArray> by lazy{
        val maxMemory = Runtime.getRuntime().maxMemory()
        val cacheSize = Math.max(Math.min(maxMemory * MEMORY_CACHE_PERCENT / 100, Int.MAX_VALUE.toLong()).toInt(), 1)
        Log.v("cacheSize", cacheSize.toString())
        LruCache<String, ByteArray>(cacheSize)
    }
    private val diskLruCache: DiskLruCache by lazy{
        val cacheDir = getDiskCacheDir(context!!, uniName)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        DiskLruCache.open(cacheDir, AppUtil.getAppVersion(context), 1, DISK_CACHE_MAX_SIZE)
    }

    enum class LoadType{
        MEMORY, DISK, INTERNET
    }

    fun post(url: String, body: String = "", header: Map<String, String> = HashMap(), callback: (String, Parser.LoadStatus) -> Unit) {
        Log.v("post", "$url $body")
        if(Thread.currentThread() == Looper.getMainLooper().thread){
            cachedThreadPool.execute {
                post(url, body, header, callback)
            }
        }else{
            var urlConnection: HttpURLConnection? = null
            try {
                urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.doInput = true
                urlConnection.doOutput = true
                urlConnection.connectTimeout = 5000
                urlConnection.readTimeout = 10000
                header.forEach {
                    urlConnection.setRequestProperty(it.key, it.value)
                }
                if (!body.trim().isEmpty()) {
                    urlConnection.outputStream.write(body.toByteArray())
                    urlConnection.outputStream.flush()
                }

                val ous = ByteArrayOutputStream()
                if (urlConnection.responseCode == 200) {
                    urlConnection.inputStream.copyTo(ous)
                    callback(ous.toString(), Parser.LoadStatus.SUCCESS)
                }else{
                    urlConnection.errorStream.copyTo(ous)
                    callback(ous.toString(), Parser.LoadStatus.FAILURE)
                }
            }catch(e: Exception){
                e.printStackTrace()
                callback(e.localizedMessage, Parser.LoadStatus.FAILURE)
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    fun get(url: String, header: Map<String, String> = HashMap(), callback: (ByteArray?, String, LoadType) -> Unit) {
        var obj: ByteArray? = null
        //form memory
        if (useCache)
            obj = getFromMemoryCache(url)
        obj?.let { callback(it, url, LoadType.MEMORY) }
        if (obj != null && !alwaysRequest) return

        if(Thread.currentThread() == Looper.getMainLooper().thread){
            cachedThreadPool.execute {
                get(url, header, callback)
            }
        }else{
            //form disk
            val key = hashKeyForDisk(url)
            if (useCache) {
                obj = getFromDiskCache(key)
                obj?.let {
                    addToMemoryCache(url, it)
                    callback(obj, url, LoadType.DISK) } }
            if (obj != null && !alwaysRequest) return
            //from net
            val ous = ByteArrayOutputStream()
            if (requestUrl(url, header) {
                        if (useCache) it.inputStream.copyTo(ous)
                        else
                            obj = decodeInputStream(it.inputStream)
                    } && useCache) {
                val editor = diskLruCache.edit(key)
                if (editor != null) {
                    val outputStream = editor.newOutputStream(0)
                    ous.writeTo(outputStream)
                    editor.commit()
                }
                obj = getFromDiskCache(key)
                obj?.let { addToMemoryCache(url, it) }
            }
            callback(obj, url, LoadType.INTERNET)
        }
    }

    fun flushCache() {
        if (useCache) { diskLruCache.flush() }
    }

    private fun requestUrl(urlString: String, header: Map<String, String>, process: (HttpURLConnection) -> Unit): Boolean {
        var urlConnection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 10000
            header.forEach {
                urlConnection.setRequestProperty(it.key, it.value)
            }
            process(urlConnection)
            true
        }catch(e: Exception){
            false
        } finally {
            urlConnection?.disconnect()
        }
    }

    //LruCache
    private fun addToMemoryCache(key: String, obj: ByteArray) {
        if (getFromMemoryCache(key) == null) memoryCache.put(key, obj)
    }
    private fun getFromMemoryCache(key: String): ByteArray? {
        return memoryCache.get(key)
    }

    //DiskLruCache
    private fun getFromDiskCache(key: String): ByteArray? {
        val snapShot = diskLruCache.get(key)?.getInputStream(0) ?: return null
        return decodeInputStream(snapShot)
    }
    private fun decodeInputStream(inputStream: InputStream): ByteArray? {
        val ous = ByteArrayOutputStream()
        inputStream.copyTo(ous)
        return ous.toByteArray()
    }

    //DecodeKey
    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (aByte in bytes) {
            val hex = Integer.toHexString(0xFF and aByte.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }
    private fun hashKeyForDisk(key: String): String {
        return try {
            val mDigest = MessageDigest.getInstance("MD5")
            mDigest.update(key.toByteArray())
            bytesToHexString(mDigest.digest())
        } catch (e: NoSuchAlgorithmException) {
            key.hashCode().toString()
        }
    }

    companion object{
        //flag
        const val FLAG_USE_CACHE = 0x1
        const val FLAG_ALWAYS_REQUEST = 0x2

        //cache
        const val CACHE_THREAD_POOL_SIZE = 100
        const val DISK_CACHE_MAX_SIZE: Long = 10 * 1024 * 1024
        const val MEMORY_CACHE_PERCENT = 20
        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            val cachePath: String = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.externalCacheDir.path
            } else {
                context.cacheDir.path
            }
            return File(cachePath + File.separator + uniqueName)
        }
    }
}