package tr.com.muzafferyenilmez.saglikliyasamtakip

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var previousTotalSteps = 0f
    private var currentSteps = 0f

    private lateinit var sharedPreferences: SharedPreferences

    private val CHANNEL_ID = "step_counter_channel"
    private val NOTIF_ID = 1

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f)

        // Android 10 ve üzeri için izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                stopSelf()
                return
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            // Sensör yoksa servis durdurulsun
            stopSelf()
            return
        }

        createNotificationChannel()

        // Foreground servisi başlat
        startForeground(NOTIF_ID, buildNotification())

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Servis sistem tarafından kapatılırsa yeniden başlatılması için
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        // Güncellenen adım sayısını kaydet
        sharedPreferences.edit()
            .putFloat("previousTotalSteps", previousTotalSteps + currentSteps)
            .apply()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            currentSteps = event.values[0] - previousTotalSteps

            // Bildirimi güncelle
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIF_ID, buildNotification())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Kullanılmıyor
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adım Sayacı",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Adım sayacı servisi çalışıyor"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sağlıklı Yaşam Takip")
            .setContentText("Adımlarınız: ${currentSteps.toInt()}")
            .setSmallIcon(R.drawable.ic_walk) // Burada kendi ikonunu koymalısın
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
