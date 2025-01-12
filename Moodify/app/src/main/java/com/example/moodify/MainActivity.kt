package com.example.moodify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.moodify.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tfliteHelper: TFLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding kullanımı
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TensorFlow Lite Helper sınıfını başlatma
        tfliteHelper = TFLiteHelper(this)
        tfliteHelper.loadModel("emotion_model.tflite")

        // Kamera iznini kontrol etme
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            startCamera()
        }

        // Duygu tespiti için buton tıklama
        binding.captureButton.setOnClickListener {
            captureMood()
        }
    }

    private fun requestCameraPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                // Kullanıcıya açık bir şekilde izin gerektiğini söyleyin
                Toast.makeText(this, "Kamera kullanımı için izin gereklidir.", Toast.LENGTH_LONG).show()
                // Ayarlar sayfasına yönlendirme yapabilirsiniz
                openAppSettings()
            }
        }
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Önceki kamera bağlantılarını temizle
                cameraProvider.unbindAll()

                val preview = androidx.camera.core.Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                preview.setSurfaceProvider(binding.previewView.surfaceProvider)
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("CameraX", "Kamera başlatma hatası", exc)
                Toast.makeText(this, "Kamera başlatılamadı: ${exc.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureMood() {
        val bitmap: Bitmap? = binding.previewView.bitmap
        if (bitmap == null) {
            Toast.makeText(this, "Kamera görüntüsü alınamadı!", Toast.LENGTH_SHORT).show()
            return
        }

        // Bitmap'i model girişine uygun boyuta ölçekleme
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // 4 boyutlu input tensor hazırlama
        val inputData = Array(1) {
            Array(224) {
                Array(224) {
                    FloatArray(3)
                }
            }
        }

        // Piksel verilerini normalleştirip tensora aktar
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                inputData[0][y][x][0] = ((pixel shr 16 and 0xFF) / 255.0f)  // Red
                inputData[0][y][x][1] = ((pixel shr 8 and 0xFF) / 255.0f)   // Green
                inputData[0][y][x][2] = ((pixel and 0xFF) / 255.0f)         // Blue
            }
        }

        // Modeli çalıştır
        val result = tfliteHelper.predict(inputData)
        val mood = interpretMood(result)
        Toast.makeText(this, "Ruh Hali: $mood", Toast.LENGTH_SHORT).show()
    }
    private fun interpretMood(output: FloatArray): String {
        val emotions = arrayOf("Sinirli 😡","Nötr 😐", "Mutlu 😊", "Korkmuş 😱","Tiksinmiş 🤢",  "Üzgün 😢",  "Şaşırmış 😲")
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        return emotions[maxIndex]
    }
}
