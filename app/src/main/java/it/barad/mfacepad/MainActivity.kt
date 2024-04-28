package it.barad.mfacepad

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import it.barad.mfacepad.PAD.calculateMobileNetScore
import it.barad.mfacepad.PAD.calculateMobileVITScore
import it.barad.mfacepad.databinding.ActivityMainBinding

import org.pytorch.Module

import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    // This code comes from Google's Getting Started with CameraX manual.
    // https://developer.android.com/codelabs/camerax-getting-started#2
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private var faceCascadeClassifier: CascadeClassifier? = null
    lateinit var faceDetector: CascadeClassifier
    private var padMobileNetv3: Module? = null
    private var mobileNetModel: Module? = null
    private var padMobileVITv2: Module? = null
    private var mobileVITModel: Module? = null
    private val haarcascade = "haarcascade_frontalface_default.xml"
    private val mobileNetv3 = "PADMobileNetV3_232_224.pt"
    private val mobileVitv2 =  "PadMobileVITv2_384_256.pt"
    private var imgPath: String = ""
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takeFacePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.cameraSwitchButton.setOnClickListener { switchCamera() }


        // OpenCV initialization
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // OpenCV faceDetector
        val filePath =  Utilities.getFileFromAssets(this, haarcascade).absolutePath
        faceDetector = faceCascadeClassifier?: CascadeClassifier(Utilities.createPrivateFile(this, "tmp").apply{writeBytes(File(filePath).readBytes())}.path).also { faceCascadeClassifier = it }

        val mobileNetPath =  Utilities.getFileFromAssets(this, fileName = mobileNetv3).absolutePath
        padMobileNetv3 = mobileNetModel?: Module.load(mobileNetPath).also { mobileNetModel = it }

        val imgPADModelPath =  Utilities.getFileFromAssets(this, fileName = mobileVitv2).absolutePath
        padMobileVITv2 = mobileVITModel?: Module.load(imgPADModelPath).also { mobileVITModel = it }

    }

    fun toggleImageViewVisibility(view: View) {
        val currentVisibility = viewBinding.photoImageView.visibility
        if (currentVisibility == View.VISIBLE) {
            viewBinding.photoImageView.visibility = View.INVISIBLE
            viewBinding.bottomTextView.visibility = View.INVISIBLE
            viewBinding.viewFinder.visibility = View.VISIBLE
            viewBinding.goBackButton.visibility = View.INVISIBLE
        } else {
            viewBinding.photoImageView.visibility = View.VISIBLE
            viewBinding.bottomTextView.visibility = View.VISIBLE
            viewBinding.viewFinder.visibility = View.INVISIBLE
            viewBinding.goBackButton.visibility = View.VISIBLE
        }
    }

    private fun takeFacePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SAMPLE_FILENAME
        val picturesDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val photoFile = File(picturesDirectory, name)

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // val msg = "Photo capture succeeded: ${output.savedUri}"
                    val msg = "Image capture was successful."
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    imgPath = output.savedUri!!.path.toString()

                    var sampleBitmap = ImgUtils.rotateBitmap(imgPath)

                    val sampleMat = Mat(sampleBitmap.width, sampleBitmap.height, CvType.CV_8UC1)
                    Utils.bitmapToMat(sampleBitmap, sampleMat)

                    val faceRect = FaceDetection.detect(sampleMat, faceDetector)

                    if (faceRect.toArray().size != 1) {
                        val msg = "Face not detected. \nTo obtain a high-quality biometric sample, the face must be clearly visible."
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    } else {
                        var roi = Utilities.crop(sampleMat, faceRect)
                        val roiBmp = Bitmap.createBitmap(roi.width(), roi.height(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(roi, roiBmp)

                        var MobileNETScore = padMobileNetv3?.let { calculateMobileNetScore(roi, it) }

                        var MobileVITScore = padMobileVITv2?.let { calculateMobileVITScore(roi, it) }

                        viewBinding.photoImageView.visibility = View.VISIBLE
                        viewBinding.goBackButton.visibility = View.VISIBLE
                        viewBinding.viewFinder.visibility = View.INVISIBLE
                        viewBinding.photoImageView.setImageBitmap(roiBmp)
                        viewBinding.bottomTextView.visibility = View.VISIBLE

                        val mobileNETScore = String.format("%.4f", MobileNETScore)
                        val mobileVITScore = String.format("%.4f", MobileVITScore)

                        val mobileNetTH = 0.27 // EER threshold
                        val mobileVITTH = 0.13 // EER threshold
                        val mobileNetDcision = if (MobileNETScore!! < mobileNetTH) "Reject" else "Accept"
                        val mobileVITDecision = if (MobileVITScore!! < mobileVITTH) "Reject" else "Accept"

                        val message = "MobileNET score: $mobileNETScore\n" +
                                "MobileNET Decision: $mobileNetDcision\n " +
                                "MobileVIT score: $mobileVITScore\n" +
                                "MobileVIT Decision: $mobileVITDecision"

                        viewBinding.bottomTextView.text = message
                    }
                }
            }
        )
    }



    // This code comes from Google's Getting Started with CameraX manual.
    // https://developer.android.com/codelabs/camerax-getting-started#3
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "mFacePAD"
        private const val SAMPLE_FILENAME = "face_image_sample.jpg"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}