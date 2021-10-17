package assistant.man.blind.com.blindassistantapp
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.EditText
import java.util.*
import android.widget.Toast
import android.content.Intent
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDeviceConnection
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import android.widget.TextView
import android.content.IntentFilter
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var editText: EditText
    private lateinit var submitButton: Button
    private lateinit var usbManager: UsbManager
    private lateinit var device: UsbDevice
    private lateinit var connection: UsbDeviceConnection
    private lateinit var serialPort: UsbSerialDevice
    private val distanceList = LinkedList<Int>()
    private val minimumDistanceEnd = 10
    private val minimumDistanceStart = 3
    private val thresholdForObstacleSignal = 2
    private val delayForSpeechCompletion = 50L
    private val maxListSize = 100000
    private val ACTION_USB_PERMISSION = "assistant.man.blind.com.blindassistantapp.USB_PERMISSION"
    var mCallback: UsbSerialInterface.UsbReadCallback = UsbSerialInterface.UsbReadCallback { 
        // Defining a Callback Which Triggers Whenever Data Is Read
        arg0 -> var data: String? = null
        try {
            data = String(arg0, StandardCharsets.UTF_8)
            val distanceListInCm = data.split("\n")
            for (distanceInCm in distanceListInCm) {
                distanceList.add(convertToInt(distanceInCm))
            }
            checkIfSingnallingRequired(distanceList)
        } 
        catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }
    fun checkIfSingnallingRequired(list: LinkedList<Int>) {
        val count = list.filter { 
            item -> ((item < minimumDistanceEnd) && (item > minimumDistanceStart))
        }.count()
        if (count >= thresholdForObstacleSignal) {
            textToSpeech.speak("Alert! Obstacle Ahead", TextToSpeech.QUEUE_FLUSH, null)
            while (textToSpeech.isSpeaking)
                Thread.sleep(delayForSpeechCompletion)
            list.clear()
        } 
        else if (list.size > maxListSize) {
            list.clear()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textToSpeech = TextToSpeech(this, this)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(broadcastReceiver, filter)
    }
    private fun convertToInt(input: String): Int {
        try {
            return input.toInt()
        } 
        catch (ex: Exception) {
            return Int.MAX_VALUE
        }
    }
    override fun onInit(status: Int) {
        if (status != TextToSpeech.ERROR)
            textToSpeech.language = Locale.UK
    }
    private fun speakOut() {
        val toSpeak = editText.text.toString()
        Toast.makeText(applicationContext, toSpeak, Toast.LENGTH_SHORT).show()
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null)
    }
    public override fun onDestroy() {
        // Shutdown TTS
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
        super.onDestroy()
    }
    fun onSetupButtonClick(view: View) {
        val usbDevices = usbManager.deviceList
        if (!usbDevices.isEmpty()) {
            var keep = true
            for (entry in usbDevices.entries) {
                device = entry.value
                val deviceVID = device.getVendorId()
                //Arduino Vendor ID
                if (deviceVID == 9025) {
                    val pi = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                    usbManager.requestPermission(device, pi)
                    keep = false
                }
                if (!keep)
                    break
            }
        }
    }
    private val broadcastReceiver = object: BroadcastReceiver() { 
        // Broadcast Receiver to Automatically Start and Stop the Serial Connection
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    connection = usbManager.openDevice(device)
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
                    if (serialPort != null) {
                        if (serialPort.open()) { 
                            // Set Serial Connection Parameters
                            serialPort.setBaudRate(9600)
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE)
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                            serialPort.read(mCallback)
                            tvAppend(textview, "Serial Connection Opened!\n")

                        }
                    }
                }
            }
        }
    }
    fun tvAppend(tv: TextView, text: CharSequence) {
        runOnUiThread { 
            tv.append(text) 
        }
    }
}