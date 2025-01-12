package com.example.moodify

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(private val context: Context) {

    private lateinit var interpreter: Interpreter

    fun loadModel(fileName: String) {
        val modelFile = loadModelFile(context, fileName)
        interpreter = Interpreter(modelFile)
    }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputData: Array<Array<Array<FloatArray>>>): FloatArray {
        // 14x7 boyutunda çıktı için hazırlık
        val outputData = Array(14) { FloatArray(7) }

        // Modeli çalıştır
        interpreter.run(inputData, outputData)

        // Son satırı (ortalama veya en yüksek olasılık) al
        return outputData[outputData.size - 1]
    }
}