package com.aracekran.video

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

/** Telefondaki ekran: adres gönderme, site listesi, ayarlar ve kurulum yardımı. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var bookmarkList: LinearLayout
    private lateinit var btnPermission: Button

    private val stateListener = AppState.StateListener { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        bookmarkList = findViewById(R.id.bookmarkList)
        btnPermission = findViewById(R.id.btnPermission)

        val urlInput = findViewById<EditText>(R.id.urlInput)
        val open = {
            val url = Prefs.normalizeUrl(urlInput.text.toString())
            if (url.isNotEmpty()) sendToCar(url)
        }
        findViewById<Button>(R.id.btnOpen).setOnClickListener { open() }
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { open(); true } else false
        }

        btnPermission.setOnClickListener {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }

        val bmName = findViewById<EditText>(R.id.bmName)
        val bmUrl = findViewById<EditText>(R.id.bmUrl)
        findViewById<Button>(R.id.btnAddBookmark).setOnClickListener {
            val url = Prefs.normalizeUrl(bmUrl.text.toString())
            val name = bmName.text.toString().trim().ifEmpty { url }
            if (url.isEmpty()) return@setOnClickListener
            val list = Prefs.bookmarks(this)
            list.add(Bookmark(name, url))
            Prefs.saveBookmarks(this, list)
            bmName.text.clear()
            bmUrl.text.clear()
            renderBookmarks()
        }

        val zoomGroup = findViewById<RadioGroup>(R.id.zoomGroup)
        zoomGroup.check(
            when (Prefs.textZoom(this)) {
                100 -> R.id.zoom100
                150 -> R.id.zoom150
                else -> R.id.zoom125
            }
        )
        zoomGroup.setOnCheckedChangeListener { _, id ->
            Prefs.setTextZoom(
                this, when (id) {
                    R.id.zoom100 -> 100
                    R.id.zoom150 -> 150
                    else -> 125
                }
            )
            AppState.notifySettingsChanged()
        }

        val desktop = findViewById<CheckBox>(R.id.desktopMode)
        desktop.isChecked = Prefs.desktopMode(this)
        desktop.setOnCheckedChangeListener { _, checked ->
            Prefs.setDesktopMode(this, checked)
            AppState.notifySettingsChanged()
        }

        findViewById<Button>(R.id.btnOpenAA).setOnClickListener { openAndroidAutoSettings() }

        renderBookmarks()
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        AppState.addStateListener(stateListener)
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        AppState.removeStateListener(stateListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
        AppState.notifySettingsChanged() // araç bağlıysa GPS hemen devreye girsin
    }

    /** YouTube vb. uygulamalarda "Paylaş → Araç Ekran Video" ile gelen linki araca gönder. */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val link = Regex("https?://\\S+").find(text)?.value ?: return
        sendToCar(link)
    }

    private fun sendToCar(url: String) {
        AppState.openUrl(url)
        val msg = if (AppState.carConnected) "Araç ekranına gönderildi"
        else "Araç bağlı değil. Bağlandığında bu adres açılacak."
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun hasLocation(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun updateStatus() {
        val car = if (AppState.carConnected) "Araç ekranı: BAĞLI" else "Araç ekranı: bağlı değil"
        val drive = when (AppState.driveState) {
            DriveState.PARKED -> "Araç duruyor: görüntü AÇIK"
            DriveState.MOVING -> "Araç hareket halinde: görüntü KAPALI"
            DriveState.UNKNOWN -> "Araç hızı bilinmiyor: görüntü KAPALI"
        }
        val perm = if (hasLocation()) "Konum izni: verildi" else "Konum izni: VERİLMEDİ"
        status.text = if (AppState.carConnected) "$car\n$drive\n$perm" else "$car\n$perm"
        btnPermission.visibility = if (hasLocation()) View.GONE else View.VISIBLE
    }

    private fun renderBookmarks() {
        bookmarkList.removeAllViews()
        val list = Prefs.bookmarks(this)
        list.forEachIndexed { index, bm ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val openBtn = Button(this).apply {
                text = bm.name
                isAllCaps = false
                setOnClickListener { sendToCar(bm.url) }
            }
            val delBtn = Button(this).apply {
                text = "Sil"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("\"${bm.name}\" silinsin mi?")
                        .setPositiveButton("Sil") { _, _ ->
                            val l = Prefs.bookmarks(this@MainActivity)
                            if (index < l.size) l.removeAt(index)
                            Prefs.saveBookmarks(this@MainActivity, l)
                            renderBookmarks()
                        }
                        .setNegativeButton("Vazgeç", null)
                        .show()
                }
            }
            row.addView(openBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(delBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            bookmarkList.addView(row)
        }
    }

    private fun openAndroidAutoSettings() {
        val attempts = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.google.android.projection.gearhead",
                    "com.google.android.apps.auto.carservice.settings.SettingsActivity"
                )
            ),
            Intent("com.google.android.projection.gearhead.SETTINGS"),
            packageManager.getLaunchIntentForPackage("com.google.android.projection.gearhead")
        )
        for (i in attempts) {
            if (i == null) continue
            try {
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
        Toast.makeText(this, "Ayarlar → Bağlı cihazlar → Android Auto yolunu izleyin", Toast.LENGTH_LONG).show()
    }
}
