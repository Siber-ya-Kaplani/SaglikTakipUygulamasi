package tr.com.muzafferyenilmez.saglikliyasamtakip

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Kullanıcı giriş yapmış, MainActivity'e yönlendir
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Kullanıcı giriş yapmamış, LoginActivity'e yönlendir
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish() // SplashActivity'i kapat
    }
}
