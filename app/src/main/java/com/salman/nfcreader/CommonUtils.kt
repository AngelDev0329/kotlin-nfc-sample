package com.salman.nfcreader

class CommonUtils {
}

fun ByteArray.toHexUpperString() = joinToString("") { "%02X".format(it) }



fun byteArrayToHexString(byteArray: ByteArray) = byteArray.joinToString("") { "%02X".format(it) }
fun byteToHexString(byte: Byte) =  byte.toString(16).padStart(2, '0')
fun shortToHexString(short: Short) = short.toString(16).padStart(2, '0')
