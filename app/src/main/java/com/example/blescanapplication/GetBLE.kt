package com.example.blescanapplication

import GattCallback
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log


class GetBLE (private val context: Context){
    //hoge
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var scanCallback: ScanCallback? = null
    private var accessDevice:String?=null
    private var bluetoothGatt: BluetoothGatt? = null
    private val gattCallback = GattCallback()

    @SuppressLint("MissingPermission")
    fun startScan() {
        bluetoothLeScanner?.let { scanner ->
            if (scanCallback == null) {
                scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        super.onScanResult(callbackType, result)
                        val device: BluetoothDevice = result.device
                        val uuids = result.scanRecord?.serviceUuids
                        val receiveRssi = result.rssi

                        uuids?.forEach { uuid ->
                            val uuidString = uuid.uuid.toString()
                            val logMessage = "Device: ${device.address}, UUID: $uuidString, RSSI: $receiveRssi"
                            Log.d("GetBLE", logMessage) // リアルタイムでログに出力
                            if (uuidString=="88888888-4abd-ba0d-b7c6-ff0a00200021"){
                                accessDevice=device.address
                                Log.d("GetBLE", "発見した(${accessDevice})")
                                stopScan()
                                connectToDevice(device)
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        super.onScanFailed(errorCode)
                        Log.e("GetBLE", "Scan failed with error code $errorCode")
                    }
                }
                scanner.startScan(scanCallback)
                Log.d("GetBLE", "scan started")
            } else {
                Log.d("GetBLE", "Scan already running")
            }
        } ?: run {
            Log.e("GetBLE", "BluetoothLeScanner is not initialized")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        Log.d("GetBLE", "Connecting to device...")
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let { bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        Log.d("GetBLE", "scan stopped")
    }

}