package com.aracekran.video.car

import android.annotation.SuppressLint
import android.app.Presentation
import android.graphics.Color
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.aracekran.video.AppState
import com.aracekran.video.DriveState
import com.aracekran.video.Prefs
import com.aracekran.video.R

/**
 * Android Auto'nun verdiği çizim yüzeyine (Surface) bir sanal ekran bağlar,
 * o sanal ekranda tam ekran bir WebView gösterir. Araç ekranındaki dokunmalar
 * (tıklama, kaydırma) WebView'a iletilir.
 *
 * Her şey telefonda çalışır; araca sadece hazır görüntü gider (Android Auto'nun
 * kendi yansıtma bağlantısı üzerinden).
 */
class WebSurfaceRenderer(private val carContext: CarContext) : SurfaceCallback {

    companion object {
        private const val TAG = "WebSurfaceRenderer"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var root: FrameLayout? = null
    private var webView: WebView? = null
    private var overlay: TextView? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var visibleArea: Rect? = null

    private var driveState: DriveState = DriveState.UNKNOWN

    private val urlListener = AppState.UrlListener { url -> webView?.loadUrl(url) }
    private val settingsListener = AppState.SettingsListener { applySettings() }

    init {
        AppState.addUrlListener(urlListener)
        AppState.addSettingsListener(settingsListener)
    }

    // ---------------------------------------------------------------- Yüzey

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        destroyDisplay()

        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return

        try {
            val dm = carContext.getSystemService(DisplayManager::class.java)
            val vd = dm.createVirtualDisplay(
                "AracEkranVideo",
                surfaceWidth, surfaceHeight, surfaceContainer.dpi,
                surface,
                0 // özel (private) sanal ekran: sadece bu uygulama kullanır
            )
            virtualDisplay = vd

            val p = Presentation(
                carContext.applicationContext,
                vd.display,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
            )
            buildViews(p)
            p.show()
            presentation = p
        } catch (e: Exception) {
            Log.e(TAG, "Araç ekranı oluşturulamadı", e)
            destroyDisplay()
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = Rect(visibleArea)
        applyPadding()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        // Görünür alan yeterli; ayrıca bir işlem gerekmiyor.
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        destroyDisplay()
    }

    // ---------------------------------------------------------------- Dokunma

    override fun onClick(x: Float, y: Float) {
        if (driveState != DriveState.PARKED) return
        val target = presentation?.window?.decorView ?: return
        val down = SystemClock.uptimeMillis()
        val evDown = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)
        val evUp = MotionEvent.obtain(down, down + 50, MotionEvent.ACTION_UP, x, y, 0)
        evDown.source = InputDevice.SOURCE_TOUCHSCREEN
        evUp.source = InputDevice.SOURCE_TOUCHSCREEN
        target.dispatchTouchEvent(evDown)
        target.dispatchTouchEvent(evUp)
        evDown.recycle()
        evUp.recycle()
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        if (driveState != DriveState.PARKED || customView != null) return
        js("window.scrollBy(${distanceX.toInt()}, ${distanceY.toInt()});")
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        if (driveState != DriveState.PARKED || customView != null) return
        // Hızla savurmayı yumuşak bir kaydırmaya çevir (yön ters: parmak yukarı = sayfa aşağı)
        val dx = (-velocityX * 0.25f).toInt()
        val dy = (-velocityY * 0.25f).toInt()
        js("window.scrollBy({left:$dx, top:$dy, behavior:'smooth'});")
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if (driveState != DriveState.PARKED) return
        val wv = webView ?: return
        if (scaleFactor > 1.02f) wv.zoomIn() else if (scaleFactor < 0.98f) wv.zoomOut()
    }

    // ---------------------------------------------------------------- Komutlar (araç ekranı düğmeleri)

    fun goBack() {
        if (customView != null) {
            customCallback?.onCustomViewHidden()
            hideCustomView()
            return
        }
        val wv = webView ?: return
        if (wv.canGoBack()) wv.goBack()
    }

    fun goHome() {
        webView?.loadUrl(Prefs.homeUrl(carContext))
    }

    fun reload() {
        webView?.reload()
    }

    /** direction: -1 yukarı, +1 aşağı */
    fun scrollPage(direction: Int) {
        if (driveState != DriveState.PARKED || customView != null) return
        js("window.scrollBy({top: ${direction} * window.innerHeight * 0.7, behavior:'smooth'});")
    }

    fun applyDriveState(state: DriveState) {
        driveState = state
        updateOverlay()
    }

    fun release() {
        AppState.removeUrlListener(urlListener)
        AppState.removeSettingsListener(settingsListener)
        destroyDisplay()
    }

    // ---------------------------------------------------------------- İç işler

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildViews(p: Presentation) {
        val ctx = p.context
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Color.BLACK) }

        val wv = WebView(ctx).apply {
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    // Sadece web adreslerini aç; "intent://", "vnd.youtube://" gibi
                    // uygulama açtırma bağlantılarını yok say (araç ekranında başka uygulama açılamaz).
                    return scheme != "http" && scheme != "https"
                }

                override fun onPageFinished(view: WebView, url: String) {
                    AppState.rememberUrl(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    showCustomView(view, callback)
                }

                override fun onHideCustomView() {
                    hideCustomView()
                }
            }
        }

        val lock = TextView(ctx).apply {
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setPadding(48, 48, 48, 48)
            isClickable = true // alttaki sayfaya dokunma geçmesin
        }

        frame.addView(wv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.addView(lock, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        p.setContentView(frame)

        root = frame
        webView = wv
        overlay = lock

        applySettings()
        applyPadding()
        updateOverlay()

        val start = AppState.lastUrl ?: Prefs.homeUrl(carContext)
        wv.loadUrl(start)
    }

    private fun applySettings() {
        val wv = webView ?: return
        wv.settings.textZoom = Prefs.textZoom(carContext)
        val desktop = Prefs.desktopMode(carContext)
        val wantedUa = if (desktop) DESKTOP_UA else null
        if (wv.settings.userAgentString != (wantedUa ?: WebSettings.getDefaultUserAgent(carContext))) {
            wv.settings.userAgentString = wantedUa
            if (wv.url != null) wv.reload()
        }
    }

    /** Android Auto'nun düğmeleri sayfanın üstüne binmesin diye kenar boşluğu bırak. */
    private fun applyPadding() {
        val frame = root ?: return
        val area = visibleArea
        if (area == null || area.isEmpty || surfaceWidth == 0) {
            frame.setPadding(0, 0, 0, 0)
            return
        }
        frame.setPadding(
            area.left.coerceAtLeast(0),
            area.top.coerceAtLeast(0),
            (surfaceWidth - area.right).coerceAtLeast(0),
            (surfaceHeight - area.bottom).coerceAtLeast(0)
        )
    }

    private fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        val frame = root ?: return
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customCallback = callback
        view.setBackgroundColor(Color.BLACK)
        frame.addView(view, frame.childCount - 1, // kilit katmanının altına ekle
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        webView?.visibility = View.INVISIBLE
    }

    private fun hideCustomView() {
        val frame = root
        customView?.let { frame?.removeView(it) }
        customView = null
        customCallback = null
        webView?.visibility = View.VISIBLE
    }

    private fun updateOverlay() {
        val lock = overlay ?: return
        when (driveState) {
            DriveState.PARKED -> lock.visibility = View.GONE
            DriveState.MOVING -> {
                lock.setText(R.string.lock_moving)
                lock.visibility = View.VISIBLE
            }
            DriveState.UNKNOWN -> {
                lock.setText(R.string.lock_unknown)
                lock.visibility = View.VISIBLE
            }
        }
    }

    private fun js(script: String) {
        webView?.evaluateJavascript(script, null)
    }

    private fun destroyDisplay() {
        customCallback?.onCustomViewHidden()
        customView = null
        customCallback = null
        webView?.let { wv ->
            wv.url?.let { if (Uri.parse(it).scheme?.startsWith("http") == true) AppState.rememberUrl(it) }
            wv.stopLoading()
            wv.destroy()
        }
        webView = null
        overlay = null
        root = null
        try {
            presentation?.dismiss()
        } catch (e: Exception) {
            Log.w(TAG, "dismiss", e)
        }
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
