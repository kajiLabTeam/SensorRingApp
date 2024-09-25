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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.blescanapplication.ui.theme.BLEScanApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var gattCallback:GattCallback
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
            // 必要なパーミッションがない場合、リクエスト
            ActivityCompat.requestPermissions(this, permissions, 1000)
        }

        // BLEデバイスのスキャンを開始
        gattCallback = GattCallback()

        val getble = GetBLE(this,gattCallback)
        getble.startScan()

        // UIを設定
        enableEdgeToEdge()
        setContent {
            BLEScanApplicationTheme {
                MainContent()
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

            } else {
                // パーミッションが拒否された場合の処理
                // ここでユーザーにパーミッションの重要性を説明することができます
            }
        }
    }
}

@Composable
fun MainContent() {
    // ボックスを使ってコンテンツ全体を配置
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // ボタンを下部中央に配置
    ) {
        Box(
            modifier = Modifier.fillMaxSize(0.9f),
            contentAlignment = Alignment.BottomCenter // ボタンを下部中央に配置
        ) {
            GreetingButton()
        }
    }
}


@Composable
fun GreetingButton() {
    // ボタンの状態を保持する
    val isOn = remember { mutableStateOf(false) }

    Button(
        onClick = {
            // ボタンがクリックされたときに状態を切り替え
            isOn.value = !isOn.value
        },
        // Paddingを設定する
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 12.dp
        )
    ) {
        // 状態に応じて表示するテキストを変更
        Text(text = if (isOn.value) "OFF" else "ON")


        // 必要に応じてアイコンを追加
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Favorite",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BLEScanApplicationTheme {
        GreetingButton()
    }
}
