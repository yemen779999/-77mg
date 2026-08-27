package com.example.utils

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object PrintHelper {
    fun printPdf(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var inStream: InputStream? = null
                var outStream: OutputStream? = null

                try {
                    inStream = FileInputStream(pdfFile)
                    outStream = FileOutputStream(destination?.fileDescriptor)

                    val buf = ByteArray(16384)
                    var size: Int
                    while (inStream.read(buf).also { size = it } >= 0 && cancellationSignal?.isCanceled == false) {
                        outStream.write(buf, 0, size)
                    }

                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                    } else {
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try { inStream?.close() } catch (e: Exception) {}
                    try { outStream?.close() } catch (e: Exception) {}
                }
            }

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                callback?.onLayoutFinished(info, newAttributes != oldAttributes)
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
