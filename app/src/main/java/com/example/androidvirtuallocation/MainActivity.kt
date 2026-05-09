package com.example.androidvirtuallocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var etAltitude: EditText
    private lateinit var btnStartMock: Button
    private lateinit var btnStopMock: Button
    private lateinit var tvStatus: TextView
    private lateinit var statusIndicator: android.view.View

    private val LOCATION_PERMISSION_CODE = 1001
    private var isMocking = false

    // 城市预设坐标
    private val CITIES = mapOf(
        "北京" to Triple(39.9042, 116.4074, 50.0),
        "上海" to Triple(31.2304, 121.4737, 10.0),
        "广州" to Triple(23.1291, 113.2644, 20.0),
        "深圳" to Triple(22.5431, 114.0579, 30.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupButtons()
        checkPermissions()
    }

    private fun initViews() {
        etLatitude = findViewById(R.id.et_latitude)
        etLongitude = findViewById(R.id.et_longitude)
        etAltitude = findViewById(R.id.et_altitude)
        btnStartMock = findViewById(R.id.btn_start_mock)
        btnStopMock = findViewById(R.id.btn_stop_mock)
        tvStatus = findViewById(R.id.tv_status)
        statusIndicator = findViewById(R.id.status_indicator)

        // 设置默认值：北京天安门
        etLatitude.setText("39.9042")
        etLongitude.setText("116.4074")
        etAltitude.setText("50.0")
    }

    private fun setupButtons() {
        // 开始按钮
        btnStartMock.setOnClickListener {
            if (checkPermissions()) {
                startMockLocation()
            }
        }

        // 停止按钮
        btnStopMock.setOnClickListener {
            stopMockLocation()
        }

        // 城市快捷按钮
        setupCityButton(R.id.btn_beijing, "北京")
        setupCityButton(R.id.btn_shanghai, "上海")
        setupCityButton(R.id.btn_guangzhou, "广州")
        setupCityButton(R.id.btn_shenzhen, "深圳")
    }

    private fun setupCityButton(buttonId: Int, cityName: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            val coords = CITIES[cityName] ?: return@setOnClickListener
            etLatitude.setText(coords.first.toString())
            etLongitude.setText(coords.second.toString())
            etAltitude.setText(coords.third.toString())
            Toast.makeText(this, "已选择：$cityName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val denied = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                denied.toTypedArray(),
                LOCATION_PERMISSION_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "✅ 权限已授予，可以使用虚拟定位", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ 需要位置权限才能使用虚拟定位", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startMockLocation() {
        val lat = etLatitude.text.toString().toDoubleOrNull()
        val lon = etLongitude.text.toString().toDoubleOrNull()
        val alt = etAltitude.text.toString().toDoubleOrNull()

        if (lat == null || lon == null) {
            Toast.makeText(this, "请输入有效的经纬度！", Toast.LENGTH_SHORT).show()
            return
        }
        if (lat < -90 || lat > 90) {
            Toast.makeText(this, "纬度范围：-90 ~ 90", Toast.LENGTH_SHORT).show()
            return
        }
        if (lon < -180 || lon > 180) {
            Toast.makeText(this, "经度范围：-180 ~ 180", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MockLocationService::class.java).apply {
            putExtra("latitude", lat)
            putExtra("longitude", lon)
            putExtra("altitude", alt ?: 0.0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isMocking = true
        updateStatus(true, lat, lon)
        Toast.makeText(this, "🛰 虚拟定位已开启！", Toast.LENGTH_SHORT).show()
    }

    private fun stopMockLocation() {
        if (!isMocking) {
            Toast.makeText(this, "虚拟定位未开启", Toast.LENGTH_SHORT).show()
            return
        }
        stopService(Intent(this, MockLocationService::class.java))
        isMocking = false
        updateStatus(false, 0.0, 0.0)
        Toast.makeText(this, "⬛ 虚拟定位已停止", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(running: Boolean, lat: Double, lon: Double) {
        if (running) {
            tvStatus.text = "虚拟定位运行中：%.4f, %.4f".format(lat, lon)
            statusIndicator.setBackgroundResource(R.drawable.circle_green)
        } else {
            tvStatus.text = "虚拟定位已关闭"
            statusIndicator.setBackgroundResource(R.drawable.circle_red)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isMocking) stopMockLocation()
    }
}
