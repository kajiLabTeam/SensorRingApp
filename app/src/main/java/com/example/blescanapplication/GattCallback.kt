import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.util.Log
import android.os.Handler
import java.util.UUID

class GattCallback : BluetoothGattCallback() {

    private var dataListener: ((String) -> Unit)? = null

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 2000L // 2秒の遅延
    private val handler = Handler()
    var gatt: BluetoothGatt? = null
    private var readAttempts = 0
    private val maxReadAttempts = 5 // 最大読み取り試行回数

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        this.gatt = gatt
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("GattCallback", "GATTサーバーに接続されました。")
                reconnectAttempts = 0 // 成功したのでリセット
                gatt?.requestMtu(40)
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
                // gatt.connect() // 再接続のためのコード（必要に応じて実装）
                reconnectAttempts++
            }, reconnectDelay)
        } else {
            Log.e("GattCallback", "最大再接続試行回数に達しました。")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val service: BluetoothGattService? =
                gatt?.getService(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200021"))
            val controlCharacteristic: BluetoothGattCharacteristic? =
                service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200022"))
            val accCharacteristic: BluetoothGattCharacteristic? =
                service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200023"))

            // 制御キャラクタリスティックを読み取る
            controlCharacteristic?.let {
                if (it.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    gatt.readCharacteristic(it)
                }
            }

            accCharacteristic?.let {
                if (it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    Log.d("GattCallback", "加速度キャラクタリスティックに通知がサポートされています。")
                    gatt.setCharacteristicNotification(it, true)

                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    if (descriptor == null) {
                        Log.e("GattCallback", "指定したデスクリプタが見つかりませんでした。")
                    } else {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val success = gatt.writeDescriptor(descriptor)
                        if (!success) {
                            Log.e("GattCallback", "デスクリプタの書き込み要求が送信できませんでした。再試行します...")
                            attemptWriteDescriptor(gatt, descriptor)
                        }else{
                            false
                        }
                    }
                } else {
                    Log.e("GattCallback", "加速度キャラクタリスティックは通知をサポートしていません。")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptWriteDescriptor(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?) {
        // デスクリプタの再試行に必要なロジックを追加可能
        handler.postDelayed({
            if (descriptor != null) {
                val success = gatt?.writeDescriptor(descriptor) ?: false
                if (success) {
                    Log.d("GattCallback", "デスクリプタの書き込み要求が再度送信されました。")
                } else {
                    Log.e("GattCallback", "デスクリプタの再書き込み要求も送信できませんでした。")
                }
            }
        }, 3000) // 3秒後に再試行
    }

    @SuppressLint("MissingPermission")
    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        this.gatt = gatt // GATTオブジェクトを保持
        if (status == BluetoothGatt.GATT_SUCCESS) {
            characteristic?.value?.let { data ->
                val dataString = data.toString(Charsets.UTF_8)
                Log.d("BLEData", "読み取ったデータ: $dataString")
            } ?: Log.d("BLEData", "データが null です")
        } else {
            Log.e("BLEData", "キャラクタリスティックの読み取りに失敗しました。ステータス: $status")
            attemptReadCharacteristic(gatt, characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptReadCharacteristic(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
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
    fun writeValueToCharacteristic(value: ByteArray) {
        val service: BluetoothGattService? = gatt?.getService(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200021"))
        val controlCharacteristic: BluetoothGattCharacteristic? = service?.getCharacteristic(UUID.fromString("88888888-4abd-ba0d-b7c6-ff0a00200022"))

        controlCharacteristic?.let {
            it.value = value
            val success = gatt?.writeCharacteristic(it) ?: false
            if (success) {
                Log.d("GattCallback", "キャラクタリスティックに値が書き込まれました: ${value.toHexString()}")
            } else {
                Log.e("GattCallback", "キャラクタリスティックへの値の書き込みに失敗しました。")
            }
        } ?: Log.e("GattCallback", "コントロールキャラクタリスティックが見つかりません。")
    }

    @SuppressLint("MissingPermission")
    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        characteristic?.let {
            val value = it.value
            val dataString = value.toString(Charsets.UTF_8).trim() // 受信データをUTF-8として文字列に変換

            // データが空でないかをチェック
            if (dataString.isEmpty()) return

            // データをカンマで分割
            val dataParts = dataString.split(",").filter { it.isNotEmpty() } // 空の要素を除去

            // 必要な数のデータが揃っているか確認
            if (dataParts.size < 5) {
                Log.d("GattCallback", "加速度: $dataString")

            }

            // 不正な値をチェック
            if (dataParts.any { it == "-" }) {
                Log.d("GattCallback", "受: $dataString")
                return
            }

            try {
                val elapsedTime = dataParts[0].toLong() // 秒
                val aX = dataParts[1].toFloat() // X軸の加速度
                val aY = dataParts[2].toFloat() // Y軸の加速度
                val aZ = dataParts[3].toFloat() // Z軸の加速度

                // データリスナーを呼び出してデータを通知
                dataListener?.invoke(dataString)

            } catch (e: NumberFormatException) {
                Log.e("GattCallback", "数値変換エラー: ${e.message}")
            }
        }
    }



    fun setDataListener(listener: (String) -> Unit) {
        this.dataListener = listener
    }

    fun ByteArray.toHexString() = joinToString(separator = " ") { "%02x".format(it) }
}