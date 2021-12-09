package ru.fxy7ci.surlight

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.fxy7ci.surlight.BT.StoreVals
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red


//TODO  добавить сканирование как в master(c уровнями сигналов)

class MainActivity : AppCompatActivity() {
    private var myAppState: AppState = AppState.AP_LOAD
    lateinit var clrCnt: ColorCont
    private val btnSlide: Button by lazy {findViewById(R.id.btnSlide)}
    private lateinit var mDetector: GestureDetector
    private val mainHandler = Handler(Looper.getMainLooper())
    // насыщенность
    private val btnH1: Button by lazy {findViewById(R.id.btnHue25)}
    private val btnH2: Button by lazy {findViewById(R.id.btnHue50)}
    private val btnH3: Button by lazy {findViewById(R.id.btnHue75)}
    private val btnH4: Button by lazy {findViewById(R.id.btnHue100)}

    // все что относится к BT
    private var mBluetoothLeService: BluetoothLeService? = null
    //private val mBluetoothAdapter: BluetoothAdapter? = null
    private var mConnected = false
    private var charFound = false

    private lateinit var fldState : TextView
    private lateinit var fldBTState : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Интерфейс
        fldState= findViewById(R.id.fldState)
        fldBTState= findViewById(R.id.fldBTState)

        // получение всех разрешений
        getBtPermission()

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

        showTxState()

        setColorClass()
        btnSlide.setBackgroundColor(clrCnt.getColor())

        setGest()

        mainHandler.post(object : Runnable {
            override fun run() {
                showTxState()
                mainHandler.postDelayed(this, 1000)
            }
        })

        btnH1.setOnClickListener (hueClick)
        btnH2.setOnClickListener (hueClick)
        btnH3.setOnClickListener (hueClick)
        btnH4.setOnClickListener (hueClick)
    }

    private val hueClick = View.OnClickListener {

        val newSat = (it as Button).tag.toString().toFloat()
        clrCnt.settValue(newSat)
        btnSlide.setBackgroundColor(clrCnt.getColor())
//        Log.d("MyLog", "Hue" + it.tag)
    }


    override fun onResume() {
        super.onResume()
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        // TODO bluetooth enable
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        //
    }

    override fun onPause() {
        savePreferences()
        super.onPause()
        mainHandler.removeCallbacksAndMessages(null)
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        mBluetoothLeService.let{
            val isConnected = mConnected
            menu?.findItem(R.id.menu_connect)?.isVisible = !isConnected
            menu?.findItem(R.id.menu_disconnect)?.isVisible = isConnected
        }
        menu?.findItem(R.id.menu_btOff)?.isVisible = (myAppState ==AppState.AP_BT_PROBLEM)
        menu?.findItem(R.id.menu_Found)?.isVisible = charFound

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_connect -> mBluetoothLeService?.connect(StoreVals.DeviceAddress)
            R.id.menu_disconnect -> {
                mBluetoothLeService?.disconnect()
                mConnected = false
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //------------------------------------------------------ своё
    //==============================================================================

    private fun setColorClass(){
        val sharedPreferences = getSharedPreferences(StoreVals.APP_PREFERENCES , MODE_PRIVATE)
        val myRed = sharedPreferences.getInt("RED", 127)
        val myGreen = sharedPreferences.getInt("GREEN", 127)
        val myBlue = sharedPreferences.getInt("BLUE", 127)
        clrCnt   = ColorCont(myRed,myGreen,myBlue)
    }

    private fun savePreferences(){
        val sharedPreferences = getSharedPreferences(StoreVals.APP_PREFERENCES , MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val color = clrCnt.getColor()
        editor.putInt("RED", color.red)
        editor.putInt("GREEN",color.green)
        editor.putInt("BLUE",color.blue)
        editor.apply()
    }

    private fun showTxState(){

        when(mBluetoothLeService?.connectionState) {
            StoreVals.STATE_DISCONNECTED -> fldBTState.text = getString(R.string.state_disconnected)
            StoreVals.STATE_CONNECTED -> fldBTState.text = getString(R.string.state_connected)
            StoreVals.STATE_CONNECTING -> fldBTState.text = getString(R.string.state_OnCconnect)
        }

//        //TODO учимся работать со структурами
        if (mConnected) {
            val value = ByteArray(6)
            val color = clrCnt.getColor()
            value[0] = 3
            value[1] = color.green.toByte()
            value[2] = color.red.toByte()
            value[3] = color.blue.toByte()
            value[4] = 0xAB.toByte()
            value[5] = 0xBA.toByte()
            mBluetoothLeService!!.sendDataToBLM(value)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setGest() {
        mDetector =GestureDetector(this, MyGestureListener())


            //GestureDetector (MyGestureListener())
        btnSlide.setOnTouchListener { _, event ->
            mDetector.onTouchEvent(event)
        }
    }


    // Get All permissions
    private fun getBtPermission() {
        myAppState = AppState.AP_BT_PROBLEM
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                StoreVals.BT_REQUEST_PERMISSION )
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

        val getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK){
//                val value = it.data?.getStringExtra("input")
                Toast.makeText(this, "BLU ON!!!", Toast.LENGTH_SHORT).show()
                //TODO перегрузка соединений
                invalidateOptionsMenu()
            }
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getResult.launch(enableBtIntent)
        }
        else{
            myAppState = AppState.AP_DISCONNET
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == StoreVals.BT_REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            //fln не работает как BluetoothLeService().LocalBinder().service
            if (!mBluetoothLeService!!.initialize()) {
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d("MyLog", "try connect ")
            mBluetoothLeService!!.connect(StoreVals.DeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    //TODO updateConnectionState(R.string.connected)
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    charFound = false
                    //TODO updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    //TODO    displayGattServices(mBluetoothLeService!!.supportedGattServices)
                }
//                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
//                    //TODO displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
//                    //val mBluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
//                    // тут пришли данные
//                }
                BluetoothLeService.FOUND_CHAR_EVRIKA -> {
                    charFound = true
                    invalidateOptionsMenu()
                }
            }
        }
    }


    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.FOUND_CHAR_EVRIKA)
        return intentFilter
    }


    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean {
            //TODO запускаем отсчет
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
//            btnSlide.setBackgroundColor(Color.BLUE)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            //TODO меню на компоненте
            clrCnt.setDefault()
            btnSlide.setBackgroundColor(clrCnt.getColor())
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            clrCnt.setOff()
            btnSlide.setBackgroundColor(clrCnt.getColor())
            return true
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            clrCnt.moveHue((distanceX * (ColorCont.MAX_HUE/3)  / btnSlide.width)*-1)
            clrCnt.moveValue(distanceY / btnSlide.width )
            btnSlide.setBackgroundColor(clrCnt.getColor())
            return true
        }

        override fun onFling(
            event1: MotionEvent, event2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
//        Log.d("MyLog", "onFling: ")
            return true
        }
    }

    enum class AppState {
        AP_LOAD,
        AP_BT_PROBLEM,
        AP_PERMISION,
        AP_DISCONNET,
        AP_CONNECT
    }


}
// END CLASS








