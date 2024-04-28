package it.barad.mfacepad

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.pytorch.IValue
import org.pytorch.MemoryFormat
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.nio.FloatBuffer

object PAD {

    fun calculateMobileNetScore(mat: Mat, model: Module): Float {
        var mutableMat = mat.clone()
        Imgproc.resize(mutableMat, mutableMat, Size(232.0, 232.0))
        mutableMat = ImgUtils.centerCrop(mutableMat, 224)
        //Imgproc.cvtColor(mutableMat, mutableMat, Imgproc.COLOR_RGBA2BGR)

        val bitmapRGB = Bitmap.createBitmap(mutableMat.width(), mutableMat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mutableMat, bitmapRGB)

        val normMeanBGR = floatArrayOf(0.485f, 0.456f, 0.406f)  // normalization parameters for rgb
        val normStdBGR = floatArrayOf(0.229f, 0.224f, 0.225f)

        val floatBuffer: FloatBuffer = Tensor.allocateFloatBuffer(3 * 224 * 224)

        TensorImageUtils.bitmapToFloatBuffer(
            bitmapRGB,
            0,
            0,
            224,
            224,
            normMeanBGR,
            normStdBGR,
            floatBuffer,
            0,
            MemoryFormat.CONTIGUOUS)

        val bgrArray = FloatArray(3 * 224 * 224)
        floatBuffer.get(bgrArray)

        val inputBGRImage = Tensor.fromBlob(bgrArray, longArrayOf(1, 3, 224.toLong(), 224.toLong())
        )
        val output = model.forward(IValue.from(inputBGRImage))?.toTensor()

        val score: FloatArray = output!!.dataAsFloatArray

        return score[0]

    }

    fun calculateMobileVIT256Score(mat: Mat, model: Module): Float {
        var mutableMat = mat.clone()
        Imgproc.resize(mutableMat, mutableMat, Size(256.0, 256.0))
        mutableMat = ImgUtils.centerCrop(mutableMat, 256)
        Imgproc.cvtColor(mutableMat, mutableMat, Imgproc.COLOR_RGBA2BGR)

        val bitmapBGR = Bitmap.createBitmap(mutableMat.width(), mutableMat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mutableMat, bitmapBGR)

        val normMeanBGR = floatArrayOf(0.0f,  0.0f, 0.0f,)  // normalization parameters for rgb
        val normStdBGR = floatArrayOf(1.0f,1.0f, 1.0f)

        val floatBuffer: FloatBuffer = Tensor.allocateFloatBuffer(3 * 256 * 256)

        TensorImageUtils.bitmapToFloatBuffer(
            bitmapBGR,
            0,
            0,
            256,
            256,
            normMeanBGR,
            normStdBGR,
            floatBuffer,
            0,
            MemoryFormat.CONTIGUOUS
        )

        val bgrArray = FloatArray(3 * 256 * 256)
        floatBuffer.get(bgrArray)
        val bgrImg = Tensor.fromBlob(bgrArray, longArrayOf(1, 3, 256.toLong(), 256.toLong())
        )
        val output = model.forward(IValue.from(bgrImg))?.toTensor()
        val score: FloatArray = output!!.dataAsFloatArray
        return score[0]
    }

    fun calculateMobileVITScore(mat: Mat, model: Module): Float {
        var mutableMat = mat.clone()
        Imgproc.resize(mutableMat, mutableMat, Size(384.0, 384.0))
        mutableMat = ImgUtils.centerCrop(mutableMat, 256)
        Imgproc.cvtColor(mutableMat, mutableMat, Imgproc.COLOR_RGBA2BGR)

        val bitmapBGR = Bitmap.createBitmap(mutableMat.width(), mutableMat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mutableMat, bitmapBGR)

        val normMeanBGR = floatArrayOf(0.0f,  0.0f, 0.0f,)  // normalization parameters for rgb
        val normStdBGR = floatArrayOf(1.0f,1.0f, 1.0f)

        val floatBuffer: FloatBuffer = Tensor.allocateFloatBuffer(3 * 256 * 256)

        TensorImageUtils.bitmapToFloatBuffer(
            bitmapBGR,
            0,
            0,
            256,
            256,
            normMeanBGR,
            normStdBGR,
            floatBuffer,
            0,
            MemoryFormat.CONTIGUOUS
        )

        val bgrArray = FloatArray(3 * 256 * 256)
        floatBuffer.get(bgrArray)
        val bgrImg = Tensor.fromBlob(bgrArray, longArrayOf(1, 3, 256.toLong(), 256.toLong())
        )
        val output = model.forward(IValue.from(bgrImg))?.toTensor()
        val score: FloatArray = output!!.dataAsFloatArray
        return score[0]
    }

}