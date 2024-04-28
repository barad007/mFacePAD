package it.barad.mfacepad

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.android.Utils

object ImgUtils {
    fun rotateBitmap(samplePath: String): Bitmap {

        var bitmap: Bitmap = BitmapFactory.decodeFile(samplePath)

        return when (ExifInterface(samplePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90F) }, true)
            ExifInterface.ORIENTATION_ROTATE_180 -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(180F) }, true)
            ExifInterface.ORIENTATION_ROTATE_270 -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(-90F) }, true)
            ExifInterface.ORIENTATION_TRANSVERSE -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(-90F) }, true)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(180F) }, true)
            ExifInterface.ORIENTATION_TRANSPOSE -> Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90F) }, true)
            else -> bitmap
        }
    }

    fun crop(sampleMat: Mat, rectangle : MatOfRect): Mat {
        val rect = rectangle.toArray()[0]
        val rectcd = Rect(rect.x, rect.y, rect.width, rect.height)
        return sampleMat.submat(rectcd)
    }

    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    fun centerCrop(mat: Mat, targetSize: Int): Mat {
        val width = mat.width()
        val height = mat.height()

        val x = (width - targetSize) / 2
        val y = (height - targetSize) / 2

        val rect = Rect(x, y, targetSize, targetSize)

        return Mat(mat, rect)
    }

    fun centerCropInplace(mat: Mat, targetSize: Int) {
        val width = mat.width()
        val height = mat.height()

        val x = (width - targetSize) / 2
        val y = (height - targetSize) / 2

        val rect = Rect(x, y, targetSize, targetSize)

        mat.adjustROI(y, y + targetSize, x, x + targetSize)

        mat.setTo(Mat(mat, rect))
    }

}