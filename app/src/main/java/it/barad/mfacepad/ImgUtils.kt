package it.barad.mfacepad

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

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
}