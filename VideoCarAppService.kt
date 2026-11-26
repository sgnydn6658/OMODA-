package com.aracekran.video.car

import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aracekran.video.AppState
import com.aracekran.video.DriveState

/**
 * Android Auto bu servise bağlanır. Servis telefonda çalışır; araç tarafına
 * hiçbir şey kurulmaz.
 */
class VideoCarAppService : CarAppService() {

    // Uygulama Play Store dışından kurulacağı için tüm Android Auto sürümlerine izin veriyoruz.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onCreateSession(): Session = BrowserSession()
}

class BrowserSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        val renderer = WebSurfaceRenderer(carContext)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(renderer)

        val monitor = DriveMonitor(carContext) { state ->
            renderer.applyDriveState(state)
            AppState.updateDriveState(state)
        }
        renderer.applyDriveState(DriveState.UNKNOWN)
        monitor.start()
        AppState.updateCarConnected(true)

        // Telefonda konum izni sonradan verilirse GPS'i hemen başlat
        val settingsListener = AppState.SettingsListener { monitor.retryGps() }
        AppState.addSettingsListener(settingsListener)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                monitor.retryGps()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                AppState.removeSettingsListener(settingsListener)
                monitor.stop()
                renderer.release()
                AppState.updateDriveState(DriveState.UNKNOWN)
                AppState.updateCarConnected(false)
            }
        })

        return BrowserScreen(carContext, renderer)
    }
}
