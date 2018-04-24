package soko.ekibun.bangumi.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.*
import soko.ekibun.bangumi.R

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        // 加载xml资源文件
        addPreferencesFromResource(R.xml.settings)
        activity?.title = activity?.getString(R.string.settings)
        refreshSummary()
    }



    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when(preference.key) {
        }
        refreshSummary()
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key){
        }
        refreshSummary()
    }

    override fun onResume() {
        super.onResume()

        refreshSummary()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    private fun refreshSummary() {
        var textPref = findPreference("api_iqiyi") as EditTextPreference
        textPref.summary = textPref.text
        textPref = findPreference("api_youku") as EditTextPreference
        textPref.summary = textPref.text
        textPref = findPreference("api_tencent") as EditTextPreference
        textPref.summary = textPref.text
        textPref = findPreference("api_pptv") as EditTextPreference
        textPref.summary = textPref.text
    }
}
