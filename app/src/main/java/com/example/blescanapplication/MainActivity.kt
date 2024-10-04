package com.example.blescanapplication

import GattCallback
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.blescanapplication.ui.theme.BLEScanApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var gattCallback: GattCallback
    private var accelerationData: String by mutableStateOf("時間, x, y, z")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 必要なパーミッションのリスト
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        // パーミッションをチェック
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 1000)
        }

        // BLEデバイスのスキャンを開始
        gattCallback = GattCallback()
        gattCallback.setDataListener { data ->
            accelerationData = data // 受信したデータを保持
        }
        val getble = GetBLE(this, gattCallback)
        getble.startScan()

        // UIを設定
        enableEdgeToEdge()
        setContent {
            BLEScanApplicationTheme {
                MainContent(gattCallback)
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

//fun MainContent(gattCallback: GattCallback, data: String) {
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        // 加速度データの表示
//        Text(text = data, modifier = Modifier.padding(16.dp))
//
//        Box(
//            modifier = Modifier.fillMaxSize(0.9f),
//            contentAlignment = Alignment.BottomCenter
//        ) {
//            GreetingButton(gattCallback)
//        }
//    }
//}

@Composable
fun GreetingButton(gattCallback: GattCallback) {
    // ボタンの状態を保持する
    val isOn = remember { mutableStateOf(false) }

    Button(
        onClick = {
            // ボタンがクリックされたときに状態を切り替え
            isOn.value = !isOn.value

            // 書き込みたい値を決定
            val valueToWrite = if (isOn.value) byteArrayOf(0x00) else byteArrayOf(0x01)
            // BluetoothGattに値を書き込む
            gattCallback.writeValueToCharacteristic(valueToWrite)
        },
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 12.dp
        )
    ) {
        Text(text = if (isOn.value) "ON" else "OFF")
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Favorite",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    }
}

@Composable
fun MainContent(gattCallback: GattCallback) {
    // 受信データを保持する状態
    val elapsedTime = remember { mutableStateOf("0") }
    val aX = remember { mutableStateOf("0.0") }
    val aY = remember { mutableStateOf("0.0") }
    val aZ = remember { mutableStateOf("0.0") }

    // データリスナーを設定
    gattCallback.setDataListener { dataString ->
        // データをカンマで分割
        val dataParts = dataString.split(",")
        if (dataParts.size >= 4) {
            elapsedTime.value = dataParts[0]
            aX.value = dataParts[1]
            aY.value = dataParts[2]
            aZ.value = dataParts[3]
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 受信したデータを表示
        Text(text = "Time: ${elapsedTime.value}", modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
        Text(text = "X: ${aX.value}", modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        Text(text = "Y: ${aY.value}", modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
        Text(text = "Z: ${aZ.value}", modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))

        Box(
            modifier = Modifier.fillMaxSize(0.9f),
            contentAlignment = Alignment.BottomCenter
        ) {
            GreetingButton(gattCallback)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BLEScanApplicationTheme {
        GreetingButton(GattCallback()) // 仮の引数としてGattCallbackを渡す
    }
}


