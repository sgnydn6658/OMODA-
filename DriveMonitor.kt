package com.aracekran.video.car

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat
import com.aracekran.video.DriveState

/**
 * Aracın durup durmadığını izler. İki kaynak kullanır:
 *  1) Android Auto'nun araçtan aldığı hız bilgisi (varsa)
 *  2) Telefonun GPS hızı (yedek)
 *
 * Kural (güvenli taraf): Hiçbir kaynaktan taze hız gelmiyorsa ya da herhangi bir
 * kaynak hareket gösteriyorsa görüntü KAPALI. Görüntü ancak tüm taze kaynaklar
 * en az 3 saniye boyunca "duruyor" dediğinde açılır.
 */
class DriveMonitor(
    private val carContext: CarContext,
    private val onChange: (DriveState) -> Unit
) {
    companion object {
        private const val TAG = "DriveMonitor"
        /** Bunun üstü kesin hareket: ~7 km/s */
        private const val MOVING_MS = 2.0f
        /** Bunun altı durma: ~3,6 km/s (GPS durağanken de biraz oynar) */
        private const val STILL_MS = 1.0f
        /** Veri bu süreden eskiyse yok sayılır */
        private const val FRESH_MS = 5_000L
        /** Açmadan önce bu kadar süre durmuş olmalı */
        private const val STILL_HOLD_MS = 3_000L
    }

    private val handler = Handler(Looper.getMainLooper())

    private var carSpeed: Float? = null
    private var carSpeedTime = 0L
    private var gpsSpeed: Float? = null
    private var gpsSpeedTime = 0L
    private var stillSince = 0L

    var state: DriveState = DriveState.UNKNOWN
        private set

    private var running = false
    private var carInfoRegistered = false
    private var gpsRegistered = false

    private val speedListener = OnCarDataAvailableListener<Speed> { speed ->
        val v: CarValue<Float> = speed.rawSpeedMetersPerSecond
        val value = v.value
        if (v.status == CarValue.STATUS_SUCCESS && value != null) {
            carSpeed = kotlin.math.abs(value)
            carSpeedTime = SystemClock.elapsedRealtime()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) {
                gpsSpeed = location.speed
                gpsSpeedTime = SystemClock.elapsedRealtime()
            }
        }
        // Eski Android sürümlerinde bu metotlar zorunlu
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val tick = object : Runnable {
        override fun run() {
            evaluate()
            if (running) handler.postDelayed(this, 1_000L)
        }
    }

    fun start() {
        if (running) return
        running = true
        registerCarSpeed()
        registerGps()
        handler.post(tick)
    }

    /** Konum izni sonradan verilirse GPS'i tekrar dene. */
    fun retryGps() {
        if (running && !gpsRegistered) registerGps()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        if (carInfoRegistered) {
            try {
                carContext.getCarService(CarHardwareManager::class.java)
                    .carInfo.removeSpeedListener(speedListener)
            } catch (e: Exception) {
                Log.w(TAG, "removeSpeedListener", e)
            }
            carInfoRegistered = false
        }
        if (gpsRegistered) {
            try {
                val lm = carContext.getSystemService(LocationManager::class.java)
                lm?.removeUpdates(locationListener)
            } catch (e: Exception) {
                Log.w(TAG, "removeUpdates", e)
            }
            gpsRegistered = false
        }
    }

    private fun registerCarSpeed() {
        try {
            val hw = carContext.getCarService(CarHardwareManager::class.java)
            hw.carInfo.addSpeedListener(ContextCompat.getMainExecutor(carContext), speedListener)
            carInfoRegistered = true
        } catch (e: Exception) {
            // Araç/Android Auto hız vermiyorsa GPS ile devam edilir.
            Log.w(TAG, "Araç hızı okunamıyor, GPS kullanılacak", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGps() {
        val granted = ContextCompat.checkSelfPermission(
            carContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        try {
            val lm = carContext.getSystemService(LocationManager::class.java) ?: return
            if (lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1_000L, 0f, locationListener, Looper.getMainLooper()
                )
                gpsRegistered = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPS başlatılamadı", e)
        }
    }

    private fun evaluate() {
        val now = SystemClock.elapsedRealtime()
        val fresh = buildList {
            carSpeed?.let { if (now - carSpeedTime < FRESH_MS) add(it) }
            gpsSpeed?.let { if (now - gpsSpeedTime < FRESH_MS) add(it) }
        }

        val newState = when {
            fresh.isEmpty() -> {
                stillSince = 0L
                DriveState.UNKNOWN
            }
            fresh.any { it > MOVING_MS } -> {
                stillSince = 0L
                DriveState.MOVING
            }
            fresh.all { it < STILL_MS } -> {
                if (stillSince == 0L) stillSince = now
                if (now - stillSince >= STILL_HOLD_MS) DriveState.PARKED
                else if (state == DriveState.PARKED) DriveState.PARKED
                else DriveState.MOVING
            }
            // Arada kalan hız (yavaş kayma): park halindeyse hareket say, değilse olduğu gibi kalsın.
            else -> {
                stillSince = 0L
                if (state == DriveState.PARKED) DriveState.MOVING else state
            }
        }

        if (newState != state) {
            state = newState
            onChange(newState)
        }
    }
}
