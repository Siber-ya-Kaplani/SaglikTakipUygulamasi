package tr.com.muzafferyenilmez.saglikliyasamtakip

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var stepsText: TextView
    private lateinit var btnAddWater: ImageView
    private lateinit var waterImages: List<ImageView>

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var previousTotalSteps = 0f

    private var waterCount = 0
    private val totalGlasses = 10 // 10 bardak x 200ml = 2 litre

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("health_prefs", Context.MODE_PRIVATE)

        // İzin kontrolü (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
        }

        val serviceIntent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }


        stepsText = findViewById(R.id.stepsText)
        btnAddWater = findViewById(R.id.btnAddWater)

        waterImages = listOf(
            findViewById(R.id.water1),
            findViewById(R.id.water2),
            findViewById(R.id.water3),
            findViewById(R.id.water4),
            findViewById(R.id.water5),
            findViewById(R.id.water6),
            findViewById(R.id.water7),
            findViewById(R.id.water8),
            findViewById(R.id.water9),
            findViewById(R.id.water10)
        )

        // Daha önce kaydedilen verileri al
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f)
        waterCount = sharedPreferences.getInt("waterCount", 0)

        // Su bardaklarını güncelle
        updateWaterUI()

        // Sensör başlat
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "Adım sensörü bulunamadı", Toast.LENGTH_SHORT).show()
        }

        btnAddWater.setOnClickListener {
            if (waterCount < totalGlasses) {
                waterImages[waterCount].setImageResource(R.drawable.ic_water_filled)
                waterCount++
                saveWaterCount()
            } else {
                Toast.makeText(this, "Günlük su hedefinize ulaştınız!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        // Adım sayısını göster (kaydedilmiş olabilir)
        val savedSteps = sharedPreferences.getFloat("savedSteps", 0f)
        stepsText.text = "${savedSteps.toInt()} Adım"
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val stepsSinceBoot = event.values[0]
            val currentSteps = stepsSinceBoot - previousTotalSteps

            stepsText.text = "${currentSteps.toInt()} Adım"
            saveSteps(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Kullanılmıyor
    }

    private fun saveSteps(steps: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("savedSteps", steps)
        editor.apply()
    }

    private fun saveWaterCount() {
        val editor = sharedPreferences.edit()
        editor.putInt("waterCount", waterCount)
        editor.apply()
    }

    private fun updateWaterUI() {
        for (i in 0 until totalGlasses) {
            if (i < waterCount) {
                waterImages[i].setImageResource(R.drawable.ic_water_filled)
            } else {
                waterImages[i].setImageResource(R.drawable.ic_water_empty)
            }
        }
    }
}
