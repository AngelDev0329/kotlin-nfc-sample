package com.salman.nfcreader


import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    private var intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(NfcV::class.java.name))
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var pendingIntent: PendingIntent? = null
    private var copiedNFCData: ByteArray = byteArrayOf()
    private var nfcCommand = "read"


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
            }
        } catch (ex:Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
        if (nfcCommand == "read") {
            if (copiedNFCData.isNotEmpty()) txt_content.text = "Please scan first card you want to read.\nAnd you can write these data to other card."
            else txt_content.text = "Please scan first card you want to read"
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                try {
                    val inNdefMessage = this[0] as NdefMessage
                    if (nfcCommand == "read") {
                        copiedNFCData = inNdefMessage.toByteArray()
                        Toast.makeText( applicationContext, "Successfully has read!", Toast.LENGTH_SHORT).show()
                        setWriteRadio()
                    }

                    if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                        val nfcv: NfcV = NfcV.get(tag) ?: return
                        if (nfcCommand == "write") {
                            nfcv.connect()
                            val response = nfcv.transceive(copiedNFCData)
                            nfcv.close()
                            Toast.makeText( applicationContext, "Successfully Wroted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (ex: Exception) {
                    Toast.makeText(applicationContext,"Something went wrong, please try again!", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
}
