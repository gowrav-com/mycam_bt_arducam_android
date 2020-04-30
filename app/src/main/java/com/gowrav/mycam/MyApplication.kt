/*
 * MIT License
 *
 * Copyright (c) 2015 Douglas Nassif Roma Junior
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gowrav.mycam

import android.app.Application
import android.bluetooth.BluetoothDevice

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.squareup.picasso.Picasso


import java.util.UUID


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = BluetoothConfiguration()

        config.bluetoothServiceClass = BluetoothClassicService::class.java //  BluetoothClassicService.class or BluetoothLeService.class

        config.context = applicationContext
        config.bufferSize = 1024
        config.characterDelimiter =  'Ù'//0xD9
        config.deviceName = "MyBT"
        config.callListenersInMainThread = true

        //config.uuid = null; // When using BluetoothLeService.class set null to show all devices on scan.
        config.uuid = UUID_DEVICE // For Classic

        config.uuidService = UUID_SERVICE // For BLE
        config.uuidCharacteristic = UUID_CHARACTERISTIC // For BLE
        config.transport = BluetoothDevice.TRANSPORT_LE // Only for dual-mode devices

        // For BLE
        // config.connectionPriority = BluetoothGatt.CONNECTION_PRIORITY_HIGH // Automatically request connection priority just after connection is through.
        //or request connection priority manually, mService.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

        BluetoothService.init(config)

        Picasso.setSingletonInstance(Picasso.Builder(this).build()) // Only needed if you are using Picasso

    }

    companion object {

        /*
     * Change for the UUID that you want.
     */
        private val UUID_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private val UUID_SERVICE = UUID.fromString("e7810a71-73ae-499d-8c15-faa9aef0c3f2")
        private val UUID_CHARACTERISTIC = UUID.fromString("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f")
    }
}
