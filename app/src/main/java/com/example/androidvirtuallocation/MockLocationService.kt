package com.example.androidvirtuallocation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    companion object {
        private const val TAG = "MockLocationService"
        const val CHANNEL_ID = "VirtualLocationChannel"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var locationManager: LocationManager
    private var mockThread: Thread? = null
    private var isRunning = false

    private var mockLat = 39.9042
    private var mockLon = 116.4074
    private var mockAlt = 50.0

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mockLat = intent?.getDoubleExtra("latitude", 39.9042) ?: 39.9042
        mockLon = intent?.getDoubleExtra("longitude", 116.4074) ?: 116.4074
        mockAlt = intent?.getDoubleExtra("altitude", 50.0) ?: 50.0

        startForeground(NOTIFICATION_ID, buildNotification())
        startMocking()
        return START_STICKY
    }

    private fun startMocking() {
        try {
            // 移除已有的测试 provider（避免重复添加报错）
            removeTestProviderSafe(LocationManager.GPS_PROVIDER)
            removeTestProviderSafe(LocationManager.NETWORK_PROVIDER)

            // 添加 GPS 模拟提供者
            addTestProvider(LocationManager.GPS_PROVIDER)
            // 添加 NETWORK 模拟提供者（双保险）
            addTestProvider(LocationManager.NETWORK_PROVIDER)

            isRunning = true

            mockThread = Thread {
                while (isRunning) {
                    pushLocation(LocationManager.GPS_PROVIDER)
                    pushLocation(LocationManager.NETWORK_PROVIDER)
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }.also { it.start() }

            Log.d(TAG, "Mock location started: $mockLat, $mockLon")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mock location", e)
        }
    }

    private fun addTestProvider(provider: String) {
        try {
            locationManager.addTestProvider(
                provider,
                false,  // requiresNetwork
                false,  // requiresSatellite
                false,  // requiresCell
                false,  // hasMonetaryCost
                true,   // supportsAltitude
                true,   // supportsSpeed
                true,   // supportsBearing
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(provider, true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not add provider: $provider", e)
        }
    }

    private fun pushLocation(provider: String) {
        try {
            val location = Location(provider).apply {
                latitude = mockLat
                longitude = mockLon
                altitude = mockAlt
                accuracy = 1.0f
                bearing = 0f
                speed = 0f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = System.nanoTime()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isMock = true
                }
            }
            locationManager.setTestProviderLocation(provider, location)
        } catch (e: Exception) {
            // provider 可能还未就绪，忽略
        }
    }

    private fun removeTestProviderSafe(provider: String) {
        try {
            locationManager.removeTestProvider(provider)
        } catch (_: Exception) { }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "虚拟定位服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持虚拟定位在后台运行"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛰 虚拟定位运行中")
            .setContentText("位置：%.4f, %.4f".format(mockLat, mockLon))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mockThread?.interrupt()
        removeTestProviderSafe(LocationManager.GPS_PROVIDER)
        removeTestProviderSafe(LocationManager.NETWORK_PROVIDER)
        Log.d(TAG, "Mock location stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
