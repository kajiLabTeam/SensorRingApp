package com.example.blescanapplication

import GattCallback
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.blescanapplication.ui.theme.BLEScanApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var gattCallback: GattCallback
    private lateinit var csvExporter: CSVExporter
    private var accelerationData: String by mutableStateOf("時間, x, y, z")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 必要なパーミッションのリスト
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.WRITE_EXTERNAL_STORAGE // ストレージ書き込みパーミッションを追加
        )

        // パーミッションをチェック
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 1000)
        }

        // CSVExporterの初期化
        csvExporter = CSVExporter(this)

        // BLEデバイスのスキャンを開始
        gattCallback = GattCallback()
        gattCallback.setDataListener { data ->
            accelerationData = data // 受信したデータを保持
            Log.d("MainActivity", "Received data: $data") // 追加
        }
        val getble = GetBLE(this, gattCallback)
        getble.startScan()

        // UIを設定
        enableEdgeToEdge()
        setContent {
            BLEScanApplicationTheme {
                MainContent(gattCallback, context = this@MainActivity)
            }
        }
    }

    // パーミッションの確認
    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // パーミッションリクエスト結果の処理
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // パーミッションが許可された場合の処理
            } else {
                // パーミッションが拒否された場合の処理
            }
        }
    }
}

@Composable
fun GreetingButton(gattCallback: GattCallback, csvExporter: CSVExporter) {
    // ボタンの状態を保持する
    val isOn = remember { mutableStateOf(false) }

    Button(
        onClick = {
            // ボタンがクリックされたときに状態を切り替え
            isOn.value = !isOn.value


            // 書き込みたい値を決定
            val valueToWrite = if (isOn.value) {

                byteArrayOf(0x00)
            } else {
                csvExporter.closeFiles()
                byteArrayOf(0x01)
            }
            // BluetoothGattに値を書き込む
            gattCallback.writeValueToCharacteristic(valueToWrite)

            if (isOn.value) {
                // デバイスがONになるとき、CSVファイルを作成
                csvExporter.createFiles()
            } else {

            }
        },
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 12.dp
        )
    ) {
        Text(text = if (isOn.value) "ON" else "OFF", fontSize = 24.sp)
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Favorite",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    }
}

@Composable
fun MainContent(gattCallback: GattCallback, context: Context) {
    val csvExporter = CSVExporter(context)
    // 受信データを保持する状態
    val elapsedTime = remember { mutableStateOf("0") }
    val aX = remember { mutableStateOf("0.0") }
    val aY = remember { mutableStateOf("0.0") }
    val aZ = remember { mutableStateOf("0.0") }
    val gX = remember { mutableStateOf("0.0") }
    val gY = remember { mutableStateOf("0.0") }
    val gZ = remember { mutableStateOf("0.0") }

    gattCallback.setDataListener { dataString ->
        val dataParts = dataString.split(",")
        if (dataParts.size >= 7) {
            elapsedTime.value = dataParts[0]
            aX.value = dataParts[1]
            aY.value = dataParts[2]
            aZ.value = dataParts[3]
            gX.value = dataParts[4]
            gY.value = dataParts[5]
            gZ.value = dataParts[6]

            // 加速度データをCSVに保存
            csvExporter.appendAccelData(elapsedTime.value, aX.value, aY.value, aZ.value)
            // 角速度データをCSVに保存
            csvExporter.appendGyroData(elapsedTime.value, gX.value, gY.value, gZ.value)
        }



    }

    // 全体を Column でレイアウト
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // 画面の余白を設定
    ) {
        // 中央に加速度と角速度を配置
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // 残りのスペースを全て占める
            contentAlignment = Alignment.Center // 中央に配置
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 加速度データを表示
                Text(text = "加速度:", fontSize = 24.sp)

                Spacer(modifier = Modifier.size(16.dp)) // 少しスペースを追加

                Text(text = "X: ${aX.value}", fontSize = 24.sp)
                Text(text = "Y: ${aY.value}", fontSize = 24.sp)
                Text(text = "Z: ${aZ.value}", fontSize = 24.sp)

                Spacer(modifier = Modifier.size(64.dp)) // 少しスペースを追加

                // 角速度データを表示
                Text(text = "角速度:", fontSize = 24.sp)

                Spacer(modifier = Modifier.size(16.dp)) // 少しスペースを追加

                Text(text = "X: ${gX.value}", fontSize = 24.sp)
                Text(text = "Y: ${gY.value}", fontSize = 24.sp)
                Text(text = "Z: ${gZ.value}", fontSize = 24.sp)

                Spacer(modifier = Modifier.size(64.dp)) // 少しスペースを追加

                Text(text = "経過時間:", fontSize = 24.sp)

                Spacer(modifier = Modifier.size(16.dp)) // 少しスペースを追加

                Text(text = "Time: ${elapsedTime.value} ms", fontSize = 24.sp)
            }
        }

        // 下部にボタンを配置
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter // 下に配置
        ) {
            GreetingButton(gattCallback,csvExporter)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BLEScanApplicationTheme {
        val context = LocalContext.current // Contextを取得
        val csvExporter = CSVExporter(context) // CSVExporterのインスタンスを作成
        GreetingButton(GattCallback(), csvExporter)
    }
}