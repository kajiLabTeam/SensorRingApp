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
                gatt?.close() // 既存の接続を閉じる
                gatt?.connect() // 再接続を試みる (ここは必要に応じて実装)
                reconnectAttempts++
            }, reconnectDelay)
        } else {
            Log.e("GattCallback", "最大再接続試行回数に達しました。")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val service: BluetoothGattService? = gatt?.getService(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200021"))
            val characteristic: BluetoothGattCharacteristic? = service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200022"))
            if (characteristic != null && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                gatt.readCharacteristic(characteristic)
            }
        }
    }


    private var readAttempts = 0
    private val maxReadAttempts = 5 // 最大読み取り試行回数

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission")
    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // 読み取ったデータを取得
            val data = characteristic?.value

            // データをログに出力
            if (data != null) {
                val dataString = data.toString(Charsets.UTF_8) // バイト配列を文字列に変換
                Log.d("BLEData", "読み取ったデータ: $dataString")
            } else {
                Log.d("BLEData", "データが null です")
            }
        } else {
            Log.e("BLEData", "キャラクタリスティックの読み取りに失敗しました。ステータス: $status")
            attemptReadCharacteristic(gatt,characteristic)
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // バイト配列を16進数の文字列に変換
            val hexString = value.joinToString(" ") { String.format("%02X", it) }
            Log.d("newBLEData", "読み取ったデータ: $hexString")
        } else {
            Log.e("newBLEData", "キャラクタリスティックの読み取りに失敗しました。ステータス: $status")
            attemptReadCharacteristic(gatt, characteristic)
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

    fun logCharacteristicProperties(characteristic: BluetoothGattCharacteristic) {
        val properties = characteristic.properties

        Log.d("GattCallback", "キャラクタリスティックのプロパティ:")
        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) {
            Log.d("GattCallback", "BROADCAST")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            Log.d("GattCallback", "READ")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            Log.d("GattCallback", "WRITE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            Log.d("GattCallback", "WRITE_NO_RESPONSE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            Log.d("GattCallback", "NOTIFY")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            Log.d("GattCallback", "INDICATE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) {
            Log.d("GattCallback", "SIGNED_WRITE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) {
            Log.d("GattCallback", "EXTENDED_PROPS")
        }
    }

}

