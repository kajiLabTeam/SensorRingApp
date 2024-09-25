import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.os.Handler
import java.util.UUID

class GattCallback : BluetoothGattCallback() {

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 2000L // 2秒の遅延
    private val handler = Handler()

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("GattCallback", "GATTサーバーに接続されました。")
                reconnectAttempts = 0 // 成功したのでリセット
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d("GattCallback", "GATTサーバーから切断されました。")
                attemptReconnect(gatt)
            }
        } else {
            Log.e("GattCallback", "接続状態の変更エラー: $status")
            if (status == 133) { // GATT_CONNECTION_TIMEOUT
                Log.e("GattCallback", "接続タイムアウト、再接続を試みます...")
                attemptReconnect(gatt)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptReconnect(gatt: BluetoothGatt?) {
        if (reconnectAttempts < maxReconnectAttempts) {
            Log.d("GattCallback", "再接続を $reconnectDelay ms 後に試みます...")
            handler.postDelayed({
                gatt?.close()
                gatt?.connect() // 再接続を試みる
                reconnectAttempts++
            }, reconnectDelay)
        } else {
            Log.e("GattCallback", "最大再接続試行回数に達しました。")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("GattCallback", "サービスが発見されました。")
            val service: BluetoothGattService? =
                gatt?.getService(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200021"))
            val controlCharacteristic: BluetoothGattCharacteristic? =
                service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200022"))

            if (controlCharacteristic != null) {
                // キャラクタリスティックが読み取り可能か確認
                if (controlCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    gatt.readCharacteristic(controlCharacteristic)
                    Log.d("GattCallback", "キャラクタリスティックの読み取りを試みています。")
                } else {
                    Log.e("GattCallback", "キャラクタリスティックは読み取りプロパティを持っていません。")
                }
            } else {
                Log.e("GattCallback", "キャラクタリスティックが見つかりません。")
            }
        } else {
            Log.e("GattCallback", "サービス発見エラー: $status")
        }
    }

    private var readAttempts = 0
    private val maxReadAttempts = 5 // 最大読み取り試行回数

    @SuppressLint("MissingPermission")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            characteristic?.let {
                val value = it.value
                if (value != null && value.isNotEmpty()) {
                    val dataString = value.toString(Charsets.UTF_8)
                    Log.d("GattCallback", "キャラクタリスティック読み取り: ${it.uuid} - 値: $dataString")
                    readAttempts = 0 // 成功した場合、リトライカウントをリセット
                } else {
                    Log.e("GattCallback", "キャラクタリスティック読み取り: ${it.uuid} - 値がnullまたは空です。")
                    attemptReadCharacteristic(gatt, characteristic) // 再試行
                }
            }
        } else {
            Log.e("GattCallback", "キャラクタリスティック読み取りエラー: $status")
            attemptReadCharacteristic(gatt, characteristic) // 再試行
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptReadCharacteristic(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (readAttempts < maxReadAttempts) {
            Log.d("GattCallback", "再度キャラクタリスティックの読み取りを試みています... (試行: ${readAttempts + 1})")
            handler.postDelayed({
                characteristic?.let {
                    val success = gatt?.readCharacteristic(it) ?: false
                    if (success) {
                        Log.d("GattCallback", "キャラクタリスティックの読み取り要求が送信されました。")
                    } else {
                        Log.e("GattCallback", "キャラクタリスティックの読み取り要求を送信できませんでした。")
                    }
                }
                readAttempts++
            }, 3000) // 3秒後に再試行
        } else {
            Log.e("GattCallback", "最大読み取り試行回数に達しました。")
        }
    }

    @SuppressLint("MissingPermission")
    fun writeValueToCharacteristic(gatt: BluetoothGatt?, value: ByteArray) {
        val service: BluetoothGattService? =
            gatt?.getService(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200021"))
        val controlCharacteristic: BluetoothGattCharacteristic? =
            service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200022"))

        controlCharacteristic?.let {
            it.value = value
            val success = gatt.writeCharacteristic(it)
            if (success) {
                Log.d("GattCallback", "キャラクタリスティックに値が書き込まれました: ${value.toHexString()}")
            } else {
                Log.e("GattCallback", "キャラクタリスティックへの値の書き込みに失敗しました。")
            }
        } ?: Log.e("GattCallback", "コントロールキャラクタリスティックが見つかりません。")
    }

    // ヘルパー関数: ByteArrayを16進数文字列に変換
    private fun ByteArray.toHexString(): String {
        return joinToString("") { String.format("%02x", it) }
    }
}

