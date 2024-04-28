package it.barad.mfacepad

import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier

object FaceDetection {

    fun detect(mat: Mat, detector: CascadeClassifier): MatOfRect {
        val rectangle = MatOfRect()
        val grayMat = mat.prepare()
        detector.detectMultiScale(grayMat, rectangle, 1.1, 5, 0, Size(100.0, 100.0), Size())
        return rectangle
    }

    private fun Mat.toGray(): Mat =
        if (channels() >= 3) Mat().apply {
            Imgproc.cvtColor(
                this@toGray,
                this,
                Imgproc.COLOR_BGR2GRAY
            )
        }
        else this

    private fun Mat.prepare(): Mat {
        val mat = toGray()
        Imgproc.equalizeHist(mat, mat)
        return mat
    }
}