package com.gowrav.mycam

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothDeviceDecorator
import kotlin.collections.ArrayList

/**
 * Created by douglas on 10/04/2017.
 */
class DeviceItemAdapter(private val mContext: Context, devices: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<DeviceItemAdapter.ViewHolder>() {
    val devices: ArrayList<BluetoothDeviceDecorator>
    private val mInflater: LayoutInflater
    private var mOnItemClickListener: OnAdapterItemClickListener? = null

    constructor(context: Context, devices: Set<BluetoothDevice>?) : this(context, ArrayList<BluetoothDevice>(devices!!)) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.device_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = if (TextUtils.isEmpty(device.name)) "---" else device.name
        holder.tvAddress.text = device.address
        holder.tvRSSI.text = device.rssi.toString() + ""
        holder.itemView.setOnClickListener { mOnItemClickListener!!.onItemClick(device, position) }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    fun setOnAdapterItemClickListener(onItemClickListener: OnAdapterItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnAdapterItemClickListener {
        fun onItemClick(device: BluetoothDeviceDecorator, position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView
        val tvAddress: TextView
        val tvRSSI: TextView

        init {
            tvName = itemView.findViewById<View>(R.id.tv_name) as TextView
            tvAddress = itemView.findViewById<View>(R.id.tv_address) as TextView
            tvRSSI = itemView.findViewById<View>(R.id.tv_rssi) as TextView
        }
    }

    companion object {
        fun decorateDevices(btDevices: Collection<BluetoothDevice>): ArrayList<BluetoothDeviceDecorator> {
            val devices: ArrayList<BluetoothDeviceDecorator> = ArrayList()
            for (dev in btDevices) {
                devices.add(BluetoothDeviceDecorator(dev, 0))
            }
            return devices
        }
    }

    init {
        this.devices = decorateDevices(devices)
        mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}