package com.aracekran.video

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/** Aracın durumuna göre video görüntüsünün açık olup olmayacağı. */
enum class DriveState {
    /** Araç duruyor: görüntü açık. */
    PARKED,
    /** Araç hareket halinde: görüntü kapalı, ses devam eder. */
    MOVING,
    /** Hız okunamıyor: güvenlik için görüntü kapalı. */
    UNKNOWN
}

/**
 * Telefon arayüzü (MainActivity) ile araç ekranı (car paketi) arasında
 * paylaşılan durum. Hepsi aynı uygulama süreci içinde çalışır.
 */
object AppState {
    private val main = Handler(Looper.getMainLooper())

    /** Araç ekranında açık olan / açılacak son adres. */
    @Volatile
    var lastUrl: String? = null
        private set

    @Volatile
    var driveState: DriveState = DriveState.UNKNOWN
        private set

    @Volatile
    var carConnected: Boolean = false
        private set

    fun interface UrlListener { fun onOpenUrl(url: String) }
    fun interface StateListener { fun onStateChanged() }
    fun interface SettingsListener { fun onSettingsChanged() }

    private val urlListeners = CopyOnWriteArraySet<UrlListener>()
    private val stateListeners = CopyOnWriteArraySet<StateListener>()
    private val settingsListeners = CopyOnWriteArraySet<SettingsListener>()

    fun addUrlListener(l: UrlListener) = urlListeners.add(l)
    fun removeUrlListener(l: UrlListener) = urlListeners.remove(l)
    fun addStateListener(l: StateListener) = stateListeners.add(l)
    fun removeStateListener(l: StateListener) = stateListeners.remove(l)
    fun addSettingsListener(l: SettingsListener) = settingsListeners.add(l)
    fun removeSettingsListener(l: SettingsListener) = settingsListeners.remove(l)

    /** Araç ekranında bir adres aç (telefondan ya da araç ekranındaki listeden). */
    fun openUrl(url: String) {
        lastUrl = url
        main.post { urlListeners.forEach { it.onOpenUrl(url) } }
    }

    /** Araç ekranındaki tarayıcı sayfa değiştirdiğinde adresi kaydeder (yeniden açmayı tetiklemez). */
    fun rememberUrl(url: String) {
        lastUrl = url
    }

    fun updateDriveState(state: DriveState) {
        if (driveState == state) return
        driveState = state
        main.post { stateListeners.forEach { it.onStateChanged() } }
    }

    fun updateCarConnected(connected: Boolean) {
        carConnected = connected
        main.post { stateListeners.forEach { it.onStateChanged() } }
    }

    fun notifySettingsChanged() {
        main.post { settingsListeners.forEach { it.onSettingsChanged() } }
    }
}
