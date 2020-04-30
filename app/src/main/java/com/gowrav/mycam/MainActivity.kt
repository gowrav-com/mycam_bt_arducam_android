package com.gowrav.mycam

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ceylonlabs.imageviewpopup.ImagePopup
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothDeviceDecorator
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity(), BluetoothService.OnBluetoothScanCallback,
    BluetoothService.OnBluetoothEventCallback, DeviceItemAdapter.OnAdapterItemClickListener {

    private var pgBar: ProgressBar? = null
    private var mMenu: Menu? = null
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DeviceItemAdapter? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mService: BluetoothService? = null
    private var mScanning: Boolean = false
    private var mWriter: BluetoothWriter? = null

    private var mImageString: String = ""
    private var mImageMode: Char = '0'
    private var mResetMode: Char = 'A'
    private var mHandler: Handler = Handler()
    private lateinit var imagePopup: ImagePopup
    private lateinit var selectedDevice: BluetoothDevice

    private var captureImagedelay: Long = 30000
    private var deviceReconnectdelay: Long = 10000

    private var mRunnable: Runnable = Runnable {
        takeImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        imagePopup = ImagePopup(this)
        imagePopup.isHideCloseIcon = true
        imagePopup.isImageOnClickClose = true
        pgBar = findViewById<View>(R.id.pg_bar) as ProgressBar
        pgBar!!.visibility = View.GONE

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mRecyclerView = findViewById<View>(R.id.rv) as RecyclerView

        val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = lm

        mAdapter = DeviceItemAdapter(this, mBluetoothAdapter!!.bondedDevices)
        mAdapter!!.setOnAdapterItemClickListener(this)
        mRecyclerView!!.adapter = mAdapter

        mService = BluetoothService.getDefaultInstance()

        mService!!.setOnScanCallback(this)
        mService!!.setOnEventCallback(this)

        mWriter = BluetoothWriter(mService)

        fab.setOnLongClickListener {
            val builder = AlertDialog.Builder(this)
            //set title for alert dialog
            builder.setTitle("Capture Mode")

            //performing positive action
            builder.setPositiveButton("Sunny") { dialogInterface, which ->
                mImageMode = '1'
                Toast.makeText(
                    applicationContext,
                    "Now mode is Outside and Sunny",
                    Toast.LENGTH_LONG
                ).show()
                dialogInterface.dismiss();
            }
            //performing cancel action
            builder.setNeutralButton("Office") { dialogInterface, which ->
                mImageMode = '0'
                Toast.makeText(
                    applicationContext,
                    "Now mode is Indoor and Office",
                    Toast.LENGTH_LONG
                ).show()
                dialogInterface.dismiss();
            }
            //performing negative action
            builder.setNegativeButton("Cloudy") { dialogInterface, which ->
                    mImageMode = '2'
                    Toast.makeText(
                        applicationContext,
                        "Now mode is Outdoor and Cloudy",
                        Toast.LENGTH_LONG
                    ).show()
                    dialogInterface.dismiss();
            }
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()

            return@setOnLongClickListener true
        }

        fab.setOnClickListener {
            mRecyclerView!!.isEnabled = false
            mHandler.post(mRunnable)
            fab.isEnabled = false
        }


    }

    private fun takeImage() {
        if (mService?.status == BluetoothStatus.NONE) {
            Toast.makeText(this, "Finding : /${selectedDevice.name}", Toast.LENGTH_SHORT).show()
            mService!!.connect(selectedDevice)
            mHandler.postDelayed(mRunnable, deviceReconnectdelay);
        } else {
            if (mImageString != "") {
                if(imagePopup.isShown){
                    imagePopup.performClick()
                }

                val file = File(this.getExternalFilesDir(null), "/$mImageString")
                imagePopup.initiatePopupWithPicasso(file);
                imagePopup.viewPopup();
            }
            Toast.makeText(this, "Capturing Image...", Toast.LENGTH_SHORT).show()
            mImageString =
                LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")) + ".jpg"
            mWriter!!.write(mImageMode)
            mHandler.postDelayed(mRunnable, captureImagedelay);

        }
    }

    override fun onResume() {
        super.onResume()
        mService!!.setOnEventCallback(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_scan) {
            mRecyclerView?.isEnabled = true
            startStopScan()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startStopScan() {
        if (!mScanning) {
            mService!!.startScan()
        } else {
            mService!!.stopScan()
        }
    }

    override fun onDeviceDiscovered(device: BluetoothDevice, rssi: Int) {
        Log.d(
            TAG,
            "onDeviceDiscovered: " + device.name + " - " + device.address + " - " + Arrays.toString(
                device.uuids
            )
        )
        val dv = BluetoothDeviceDecorator(device, rssi)
        val index = mAdapter!!.devices.indexOf(dv)
        if (index < 0) {
            mAdapter!!.devices.add(dv)
            mAdapter!!.notifyItemInserted(mAdapter!!.devices.size - 1)
        } else {
            mAdapter!!.devices[index].device = device
            mAdapter!!.devices[index].rssi = rssi
            mAdapter!!.notifyItemChanged(index)
        }
    }

    override fun onStartScan() {
        Log.d(TAG, "onStartScan")
        mScanning = true
        pgBar!!.visibility = View.VISIBLE
        mMenu!!.findItem(R.id.action_scan).setTitle(R.string.action_stop)
    }

    override fun onStopScan() {
        Log.d(TAG, "onStopScan")
        mScanning = false
        pgBar!!.visibility = View.GONE
        mMenu!!.findItem(R.id.action_scan).setTitle(R.string.action_scan)
    }

    override fun onDataRead(buffer: ByteArray, length: Int) {
        val path = this.getExternalFilesDir(null)
        File(path, "/" + mImageString).appendBytes(buffer)
        if (length < 1023) {
            val mbuf = ByteArray(1)
            mbuf[0] = 'Ù'.toByte()
            File(path, "/" + mImageString).appendBytes(mbuf)
        }
    }

    override fun onStatusChange(status: BluetoothStatus) {
        Log.d(TAG, "onStatusChange: $status")
        Toast.makeText(this, status.toString(), Toast.LENGTH_SHORT).show()

        if (status == BluetoothStatus.CONNECTED) {
            Toast.makeText(this, status.toString(), Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Reset Interface", Toast.LENGTH_SHORT).show()
            mWriter!!.write(mResetMode)
        }

    }

    override fun onDeviceName(deviceName: String) {
        Log.d(TAG, "onDeviceName: $deviceName")
    }

    override fun onToast(message: String) {
        Log.d(TAG, "onToast: $message")
    }

    override fun onDataWrite(buffer: ByteArray) {
        Log.d(TAG, "onDataWrite: $buffer")
//        mWriter!!.writeln(mEdWrite.getText().toString())
    }

    override fun onItemClick(device: BluetoothDeviceDecorator, position: Int) {
        selectedDevice = device.device
        mService!!.connect(device.device)
    }

    companion object {
        const val TAG = "MyCam"
    }

}
