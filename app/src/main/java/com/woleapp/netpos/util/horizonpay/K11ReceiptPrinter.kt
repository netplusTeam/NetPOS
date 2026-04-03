package com.woleapp.netpos.util.horizonpay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.RemoteException
import android.util.Log
import com.horizonpay.smartpossdk.aidl.printer.AidlPrinterListener
import com.horizonpay.smartpossdk.data.PrinterConst
import com.woleapp.netpos.app.DeviceHelper
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.formatDate
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
// Import GenerateBitmap and CombBitmap from wherever you placed them
import com.woleapp.netpos.util.horizonpay.CombBitmap
import com.woleapp.netpos.util.horizonpay.GenerateBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object K11ReceiptPrinter {

    private const val TAG = "K11_PRINTER"

    fun printReceipt(
        context: Context, 
        transaction: TransactionResponse, 
        isMerchantCopy: Boolean,
        onComplete: (Boolean, String) -> Unit // Callback for success/error
    ) {
        // Run on IO dispatcher since bitmap generation can be heavy
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val printer = DeviceHelper.getPrinter()
                if (printer == null) {
                    CoroutineScope(Dispatchers.Main).launch { onComplete(false, "Printer hardware not found") }
                    return@launch
                }

                // 1. Generate the Bitmap Receipt
                val receiptBitmap = generateReceiptBitmap(context, transaction, isMerchantCopy)

                // 2. Set Print Quality (LEVEL_3 is usually dark and clear)
                printer.setPrintGray(PrinterConst.Gray.LEVEL_3)

                // 3. Execute Print Command
                printer.printBmp(true, false, receiptBitmap, 0, object : AidlPrinterListener.Stub() {
                    override fun onError(errorCode: Int) {
                        val errorMsg = when (errorCode) {
                            PrinterConst.RetCode.ERROR_PRINT_NOPAPER -> "Out of paper"
                            PrinterConst.RetCode.ERROR_DEV -> "Printer device error"
                            PrinterConst.RetCode.ERROR_DEV_IS_BUSY -> "Printer is busy"
                            -13 -> "Battery too low to print"
                            else -> "Unknown error ($errorCode)"
                        }
                        Log.e(TAG, "Print Error: $errorMsg")
                        CoroutineScope(Dispatchers.Main).launch { onComplete(false, errorMsg) }
                    }

                    override fun onPrintSuccess() {
                        Log.d(TAG, "Print Success")
                        CoroutineScope(Dispatchers.Main).launch { onComplete(true, "Printed successfully") }
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Exception during printing", e)
                CoroutineScope(Dispatchers.Main).launch { onComplete(false, "Printer exception: ${e.localizedMessage}") }
            }
        }
    }

    private fun generateReceiptBitmap(
        context: Context, 
        trans: TransactionResponse, 
        isMerchantCopy: Boolean
    ): Bitmap {
        val combBitmap = CombBitmap()
        
        // --- 1. HEADER ---
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("NIBSS POS RECEIPT", 26, GenerateBitmap.AlignEnum.CENTER, true, false))
        
        val user = Singletons.getCurrentlyLoggedInUser()
        val merchantName = user?.business_name ?: "MERCHANT NAME"
        val merchantAddress = user?.business_address ?: "NIGERIA"
        
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(merchantName, 30, GenerateBitmap.AlignEnum.CENTER, true, false))
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(merchantAddress, 22, GenerateBitmap.AlignEnum.CENTER, false, false))
        
        combBitmap.addBitmap(GenerateBitmap.formatBitmap(GenerateBitmap.generateLine(1), GenerateBitmap.AlignEnum.CENTER))

        // --- 2. TERMINAL INFO ---
        combBitmap.addBitmap(buildRow("TERMINAL ID:", trans.terminalId, 24))
        combBitmap.addBitmap(buildRow("MERCHANT ID:", user?.merchantId ?: "N/A", 24))
        
        // --- 3. TRANSACTION DETAILS ---
        combBitmap.addBitmap(buildRow("STAN:", trans.STAN, 24))
        combBitmap.addBitmap(trans.transactionTimeInMillis.formatDate()
            ?.let { buildRow("DATE/TIME:", it, 24) })
        
        val transType = trans.transactionType.name
        combBitmap.addBitmap(buildRow("TRANS TYPE:", transType, 24))
        combBitmap.addBitmap(buildRow("ACCOUNT:", trans.accountType.name, 24))
        
        // --- 4. CARD DETAILS ---
        val cardLabel = trans.cardLabel ?: "CARD"
        val pan = trans.maskedPan ?: "******"
        combBitmap.addBitmap(buildRow("$cardLabel:", pan, 24))
        
        combBitmap.addBitmap(GenerateBitmap.generateGap(10))

        // --- 5. AMOUNT ---
        val formattedAmount = (trans.amount / 100.0).formatCurrencyAmount("\u20A6")
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("AMOUNT: $formattedAmount", 36, GenerateBitmap.AlignEnum.CENTER, true, false))
        
        combBitmap.addBitmap(GenerateBitmap.generateGap(10))

        // --- 6. RESPONSE ---
        val responseMsg = if (trans.responseCode == "00") "APPROVED" else "DECLINED"
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("RESPONSE: ${trans.responseCode}", 24, GenerateBitmap.AlignEnum.CENTER, true, false))
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(responseMsg, 30, GenerateBitmap.AlignEnum.CENTER, true, false))

        combBitmap.addBitmap(GenerateBitmap.formatBitmap(GenerateBitmap.generateLine(1), GenerateBitmap.AlignEnum.CENTER))

        // --- 7. FOOTER ---
        val copyType = if (isMerchantCopy) "*** MERCHANT COPY ***" else "*** CUSTOMER COPY ***"
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(copyType, 24, GenerateBitmap.AlignEnum.CENTER, true, false))
        
        val appVersion = "App Ver: 1.0.0" // Replace with your actual app version
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(appVersion, 20, GenerateBitmap.AlignEnum.CENTER, false, false))
        combBitmap.addBitmap(GenerateBitmap.generateGap(60)) // Feed paper out

        return combBitmap.combBitmap
    }

    /**
     * Helper to create left/right aligned key-value pairs (e.g. "TERMINAL ID:      12345678")
     */
    private fun buildRow(label: String, value: String, size: Int): Bitmap {
        // GenerateBitmap doesn't have a native "Space Between" feature, 
        // so we fake it by padding the middle with spaces based on character count.
        // For a 384 dot width (58mm paper), roughly 32 characters fit on a line at size 24.
        val maxChars = 32
        val spaceCount = maxChars - label.length - value.length
        val spaces = if (spaceCount > 0) " ".repeat(spaceCount) else " "
        val fullString = "$label$spaces$value"
        return GenerateBitmap.str2Bitmap(fullString, size, GenerateBitmap.AlignEnum.LEFT, false, false)
    }
}