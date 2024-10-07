package com.example.blescanapplication

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CSVExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val accelFileName = "${dateFormat.format(Date())}_acc.csv"
    private val gyroFileName = "${dateFormat.format(Date())}_gyro.csv"
    private var accelCsvFile: File? = null
    private var gyroCsvFile: File? = null
    private var filesCreated = false

    init {
        createFiles()
    }

    // 新しいCSVファイルを外部ストレージに作成
    fun createFiles() {
        if (!filesCreated) { // ファイルがまだ作成されていない場合
            val externalStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            accelCsvFile = File(externalStorageDir, accelFileName)
            gyroCsvFile = File(externalStorageDir, gyroFileName)

            try {
                if (!accelCsvFile!!.exists()) {
                    accelCsvFile!!.createNewFile()
                    val writer = FileWriter(accelCsvFile)
                    writer.append("Time,Accel_X,Accel_Y,Accel_Z\n")
                    writer.flush()
                    writer.close()
                }

                if (!gyroCsvFile!!.exists()) {
                    gyroCsvFile!!.createNewFile()
                    val writer = FileWriter(gyroCsvFile)
                    writer.append("Time,Gyro_X,Gyro_Y,Gyro_Z\n")
                    writer.flush()
                    writer.close()
                }

                filesCreated = true // ファイルが作成されたことを記録
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // データを加速度CSVファイルに追加
    fun appendAccelData(time: String, accelX: String, accelY: String, accelZ: String) {
        try {
            val writer = FileWriter(accelCsvFile, true)
            writer.append("$time,$accelX,$accelY,$accelZ\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // データをジャイロCSVファイルに追加
    fun appendGyroData(time: String, gyroX: String, gyroY: String, gyroZ: String) {
        try {
            val writer = FileWriter(gyroCsvFile, true)
            writer.append("$time,$gyroX,$gyroY,$gyroZ\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // ファイルをクローズするメソッド
    fun closeFiles() {
        // 必要に応じてリソースを解放する処理を追加
        accelCsvFile = null
        gyroCsvFile = null
    }
}
