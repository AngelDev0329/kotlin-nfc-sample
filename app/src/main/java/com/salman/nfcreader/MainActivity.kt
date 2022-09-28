package com.salman.nfcreader


//import android.nfc.tech.NfcV
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.time.LocalDate
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {
    private var intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(NfcA::class.java.name))
//    private val techListsArray = arrayOf(arrayOf(NfcV::class.java.name))
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var pendingIntent: PendingIntent? = null
    private var copiedNFCData: ByteArray = byteArrayOf()
    private var pageBuffer: ByteArray = byteArrayOf()
    private var nfcCommand = "read"
    private val TAG = "MainActivity"
    val today = LocalDate.now()
    val limitDate = LocalDate.of(2022, 8, 1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        setContentView(R.layout.activity_main)
        setWriteRadio()
        radio_read.isChecked = true
        val today = LocalDate.now()
        val limitDate = LocalDate.of(2022, 7, 28)

        if (today > limitDate) {
            main_lin.visibility = View.GONE
            txt_expire.visibility = View.VISIBLE
        }

        try {
            btnwrite.setOnClickListener {
                val intent = Intent(this, WriteData::class.java)
                startActivity(intent)
            }
            //nfc process start
            pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            intentFiltersArray = arrayOf(ndef)
            if (nfcAdapter == null) {
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setMessage("This device doesn't support NFC.")
                builder.setPositiveButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
                txt_content.text = "THIS DEVICE DOESN'T SUPPORT NFC. PLEASE TRY WITH ANOTHER DEVICE!"
            } else if (!nfcAdapter!!.isEnabled) {
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setTitle("NFC Disabled")
                builder.setMessage("Plesae Enable NFC")
                txt_content.text = "NFC IS NOT ENABLED. PLEASE ENABLE NFC IN SETTINGS->NFC"

                builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                builder.setNegativeButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
            } else if (nfcAdapter!!.isEnabled) {
                Toast.makeText(applicationContext, "You can read NFC data using this app.", Toast.LENGTH_SHORT).show()
            }
        } catch (ex:Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
//        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
//        if (nfcCommand == "read") {
//            if (copiedNFCData.isNotEmpty()) txt_content.text = "Please scan first card you want to read.\nAnd you can write these data to other card."
//            else txt_content.text = "Please scan first card you want to read"
//        }

        if (today <= limitDate) {
            val filters = arrayOfNulls<IntentFilter>(1)
            val techList = arrayOf<Array<String>>()

            filters[0] = IntentFilter()
            with(filters[0]) {
                this?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
                this?.addCategory(Intent.CATEGORY_DEFAULT)
                try {
                    this?.addDataType("text/plain")
                } catch (ex: IntentFilter.MalformedMimeTypeException) {
                    throw RuntimeException(ex)
                }
            }

            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        var tagFromIntent: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val nfcA = NfcA.get(tagFromIntent)
        if(nfcA == null){
            Toast.makeText( applicationContext, "Not a Nfc V tag\n", Toast.LENGTH_SHORT).show()

            // Start the tag communications thread
            Thread().start()
            return
        }

        val atqa: ByteArray = nfcA.atqa
        val sak: Short = nfcA.sak
        nfcA.connect()
        val isConnected= nfcA.isConnected

        if(isConnected) {
            if (nfcCommand == "write") {
//                Toast.makeText( applicationContext, "Successfully Wroted!", Toast.LENGTH_SHORT).show()
            } else {
                //this will send raw data send the values you want in the byte[]
                // just add the raw hex values with commas
                //pageBuffer is an array that will hold the response
                try{

//                    pageBuffer = nfcA.transceive(
//                        byteArrayOf(0x11.toByte(), 0x24.toByte(), 0x11.toByte())
//                    )
                    copiedNFCData = nfcA.transceive(
                        byteArrayOf(
                            0x30.toByte(),  /* CMD = READ */
                            0x01.toByte() /* PAGE = 1  */
                        )
                    )

                    if (tagFromIntent != null) {
                        val dataSet = readeNfcADataset(tagFromIntent, this);

                        var resStrng = ""
                        for (item in dataSet) {
                            resStrng += item.name + ":" + item.value + "\n"
                        }
                        txt_content.text = resStrng
                    }
                    Toast.makeText( applicationContext, "Page 1 was has read successfully from!", Toast.LENGTH_LONG).show()

                } catch(ex: IOException){
                    //handle error here
                    Toast.makeText(applicationContext, ex.message + " when read card page info", Toast.LENGTH_SHORT).show()
                } finally {
                    try {
                        nfcA.close();
                    } catch (e: Exception) {
                    }
                }
            }
        } else {
            Log.e("ans", "Not connected")
            Toast.makeText( applicationContext, "NFC is not connected!", Toast.LENGTH_SHORT).show()
        }








//        if (nfcA != null) {
//            //this will send raw data send the values you want in the byte[]
//            // just add the raw hex values with commas
//            //pageBuffer is an array that will hold the response
//            try{
//                pageBuffer = nfcA.transceive(byteArrayOf(0x11.toByte(), 0x24.toByte(), 0x11.toByte()))
//                Toast.makeText( applicationContext, "NFC Page info was has read successfully from!", Toast.LENGTH_SHORT).show()
//
//            }catch(ex: IOException){
//                //handle error here
//                Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
//            }
//
//
//            val atqa: ByteArray = nfcA.atqa
//            val sak: Short = nfcA.sak
//            nfcA.connect()
//            val isConnected= nfcA.isConnected
//
//            if(isConnected) {
//                if (nfcCommand == "write") {
//
////                Toast.makeText( applicationContext, "Successfully Wroted!", Toast.LENGTH_SHORT).show()
//                } else {
//                    copiedNFCData = nfcA.transceive(
//                        byteArrayOf(
//                            0x30.toByte(),  /* CMD = READ */
//                            0x01.toByte() /* PAGE = 1  */
//                        )
//                    )
//                    //code to handle the received data
//                    // Received data would be in the form of a byte array that can be converted to string
//                    //NFC_READ_COMMAND would be the custom command you would have to send to your NFC Tag in order to read it
//                    Toast.makeText( applicationContext, "Successfully has read data of page 1!", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Log.e("ans", "Not connected")
//                Toast.makeText( applicationContext, "NFC is not connected!", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText( applicationContext, "Not a Nfc V tag\n", Toast.LENGTH_SHORT).show()
//        }

    }

    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    private fun setWriteRadio () {
        radio_write.isEnabled = copiedNFCData.isNotEmpty()
        if (radio_write.isEnabled) {
            val textColor = Color.parseColor("#ffffff");
            radio_write.buttonTintList = ColorStateList.valueOf(textColor)
            radio_write.setTextColor(textColor)
        } else {
            val textColor = Color.parseColor("#afafaf");
            radio_write.buttonTintList = ColorStateList.valueOf(textColor)
            radio_write.setTextColor(textColor)
        }
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked

            when (view.getId()) {
                R.id.radio_read ->
                    if (checked) {
                        nfcCommand = "read"
                        txt_content.text = "Please scan first card you want to read"
                        copiedNFCData = byteArrayOf()
                    }
                R.id.radio_write ->
                    if (checked) {
                        nfcCommand = "write"
                        txt_content.text = "Please put second card you want to write"
                    }
            }
        }
    }


    private fun readeNfcADataset(tag: Tag, cxt: Context): ArrayList<IdNameValue> {
        var excMsg = ""
        val dataSet: ArrayList<IdNameValue> = ArrayList()
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            dataSet.add(
                IdNameValue(
                    "atqa ",
                    "0x" + byteArrayToHexString(nfcA.atqa)
                )
            )
            dataSet.add(
                IdNameValue(
                    "sak ",
                    "0x" + shortToHexString(nfcA.sak)
                )
            )
            var blockNumber: Byte = 0x00
            val bytesNumber = 1
            val pagesNumber = 16
            var counter = 0
            val bytes = Array(pagesNumber) {
                ByteArray(
                    bytesNumber
                )
            }
            excMsg = """
                _________________________________________________
                "picc compliant with iso iec_14443_3A_fnca":
                """.trimIndent()
            Log.i(TAG, excMsg)

            for (i in 0 until pagesNumber) {
                excMsg = ""
                while (!nfcA.isConnected) {
                    try {
                        nfcA.connect()
                        excMsg = "Connecting to tag has been created"
                        Log.i(TAG, "109: $excMsg")
                        bytes[i] = nfcA.transceive(
                            byteArrayOf(
                                0x30.toByte(),  // READ
                                (blockNumber and 0x0FF.toByte()) as Byte
                            )
                        )
                        excMsg = "made to read data from a block"
                            .toString() + Integer.toHexString(blockNumber.toInt())
                        Log.i(TAG, "117: $excMsg")
                        if (nfcA.isConnected) nfcA.close()
                        excMsg += "tag has been closed"
                        break
                    } catch (e: IOException) {
                        counter++
                        excMsg = e.message + ":> " +
                                if (excMsg == "")
                                    "The connection tag is not created"
                                else if (excMsg.contains("connecting to tag has been created") || excMsg.contains("made to read data from a page"))
                                    "Failed to read data from a tag block: " + Integer.toHexString(blockNumber.toInt())
                                else
                                    "error closing tag"
                        Log.e(TAG, "130: $excMsg")
                    }
                    if (nfcA.isConnected) try {
                        nfcA.close()
                    } catch (e1: IOException) {
                        excMsg += ":> " + e1.message
                        Log.e(TAG, "137: $excMsg")
                    }
                    if (counter > 9) {
                        excMsg = "The number of connection attempts exceeded the number of ten"
                        Log.e(TAG, "143: $excMsg")
                        break
                    }
                }
                counter = 0
//                (blockNumber += bytes[i].length as Byte).toByte()
                blockNumber = (i * 4).toByte()

            }
            if (excMsg == "The number of connection attempts has exceeded one hundred") {
                dataSet.add(IdNameValue("Error", " reading the data tag"))
                return dataSet
            }
            dataSet.add(IdNameValue("Page", "    0    1    2    3"))
            var name = ""
            var value = ""
            for (i in 0 until pagesNumber) {
                if (value != "") dataSet.add(IdNameValue(name, value))
                name = (if (i > 9) " " else "    ") + i.toString() + "   "
                value = ""
                for (j in 0 until bytes[i].size) {
                    value += "  " + byteToHexString(bytes[i][j])
                }
            }
            dataSet.add(IdNameValue(name, value))
            return dataSet
        }
        return dataSet
    }

}


data class IdNameValue(val name: String, val value: String)