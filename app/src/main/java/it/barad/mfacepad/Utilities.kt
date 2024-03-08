package it.barad.mfacepad

import java.io.File
import java.io.IOException
import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.util.UUID

object Utilities {
    private val privateFileName: String
        get() = UUID.randomUUID().toString().take(32)
    fun createPrivateFile(context: Context, parentDir: String, fileExtension: String = ""): File {
        val internalDir = File(context.filesDir, parentDir).apply { createIfDoesNotExist() }
        return File(internalDir, "$privateFileName$fileExtension")
    }

    @Throws(IOException::class)
    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    fun File.createIfDoesNotExist(): Boolean = if (!exists()) mkdirs() else true

    /**
     * A function that crops the photo according to the coordinates of the face.
     *
     * @param sampleMat Mat object contains image.
     * @param rectangle Face coordinates.
     * @return Mat object contains face image
     */
    fun crop(sampleMat: Mat, rectangle : MatOfRect): Mat {
        val rect = rectangle.toArray()[0]
        val rectcd = Rect(rect.x, rect.y, rect.width, rect.height)
        return sampleMat.submat(rectcd)
    }

    fun detectFace(imgPath: String, faceDetector: CascadeClassifier): MatOfRect {
        var sampleBitmap = ImgUtils.rotateBitmap(imgPath)
        val sampleMat = Mat(sampleBitmap.width, sampleBitmap.height, CvType.CV_8UC1)
        Utils.bitmapToMat(sampleBitmap, sampleMat)
//        val NewWidth = 1024.0
//        val NewHeight = (sampleBitmap.height * (NewWidth / sampleBitmap.width))
//        Imgproc.resize(sampleMat, sampleMat, Size(NewWidth, NewHeight))
//        val faceBmp = Bitmap.createBitmap(sampleMat.width(), sampleMat.height(), Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(sampleMat, faceBmp)

        return FaceDetection.detect(sampleMat, faceDetector)
    }

}