package com.hocel.pytorchsample

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val CLASSNUM = 21
    private val DOG = 12
    private val PERSON = 15
    private val SHEEP = 17

    private var mImagename = "deeplab.jpg"
    private var mModule: Module? = null

    var mBitmap: MutableState<Bitmap?> = mutableStateOf(null)

    init {
        try {
            mModule = LiteModuleLoader.load(
                assetFilePath(
                    application,
                    "deeplabv3_scripted_optimized.ptl"
                )
            )
        } catch (e: Exception) {
            Log.d("AppTest", "${e.message}")
        }

    }

    fun loadImage() {
        mImagename = if (mImagename === "deeplab.jpg") "dog.jpg" else "deeplab.jpg"
        try {
            mBitmap.value = BitmapFactory.decodeStream(application.assets.open(mImagename))
        } catch (e: IOException) {
            Log.e("ImageSegmentation", "Error reading assets", e)
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    fun run() {
        viewModelScope.launch {
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                mBitmap.value,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )
            val outTensors = mModule!!.forward(IValue.from(inputTensor)).toDictStringKey()
            val outputTensor = outTensors["out"]!!.toTensor()
            val scores = outputTensor.dataAsFloatArray
            val width: Int = mBitmap.value!!.width
            val height: Int = mBitmap.value!!.height
            val intValues = IntArray(width * height)
            for (j in 0 until height) {
                for (k in 0 until width) {
                    var maxi = 0
                    var maxj = 0
                    var maxk = 0
                    var maxnum = -Double.MAX_VALUE
                    for (i in 0 until CLASSNUM) {
                        val score = scores[i * (width * height) + j * width + k]
                        if (score > maxnum) {
                            maxnum = score.toDouble()
                            maxi = i
                            maxj = j
                            maxk = k
                        }
                    }

                    when (maxi) {
                        PERSON -> {
                            intValues[maxj * width + maxk] = -0x10000
                        }

                        DOG -> {
                            intValues[maxj * width + maxk] = -0xff0100
                        }

                        SHEEP -> {
                            intValues[maxj * width + maxk] = -0xffff01
                        }

                        else -> {
                            intValues[maxj * width + maxk] = -0x1000000
                        }
                    }
                }
            }
            val bmpSegmentation = Bitmap.createScaledBitmap(mBitmap.value!!, width, height, true)
            val outputBitmap = bmpSegmentation.copy(bmpSegmentation.config, true)
            outputBitmap.setPixels(
                intValues,
                0,
                outputBitmap.width,
                0,
                0,
                outputBitmap.width,
                outputBitmap.height
            )
            val transferredBitmap =
                Bitmap.createScaledBitmap(
                    outputBitmap,
                    mBitmap.value!!.width,
                    mBitmap.value!!.height,
                    true
                )
            mBitmap.value = transferredBitmap
        }
    }
}