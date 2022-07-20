package com.educative.mycameraapp

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.educative.mycameraapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var bitmap: Bitmap? = null
    private val TAG = "BOKEH"
    private var preview: Preview? = null
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        previewView = binding.previewView
        requestCameraPermission()
        //initPreview()
        binding.fabCapture.setOnClickListener {
            takePicture()
        }

        setContentView(view)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun initPreview() {
        preview = Preview.Builder().build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            startCamera()
        }
    }


    private fun startCamera() {
        initPreview()
        initImageCapture()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        lifecycleScope.launch {
            val cameraProvider = ProcessCameraProvider.getInstance(this@MainActivity).await()
            val extensionsManager =
                ExtensionsManager.getInstanceAsync(this@MainActivity, cameraProvider).await()

            try {
                if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)) {
                    Log.d(TAG, "startCamera: Bokeh effect is available")
                    cameraProvider.unbindAll()
                    val bokehSelector = extensionsManager.getExtensionEnabledCameraSelector(
                        cameraSelector,
                        ExtensionMode.BOKEH
                    )
                    cameraProvider.bindToLifecycle(
                        this@MainActivity,
                        bokehSelector,
                        preview,
                        imageCapture
                    )
                } else {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this@MainActivity,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    Log.d(TAG, "startCamera: The BOKEH effect is not available")
                }

            } catch (ex: Exception) {
                Log.e(TAG, "Failed to bind lifecycle", ex)
            }
        }
    }

    private fun initImageCapture() {
        imageCapture = ImageCapture.Builder()
            .setJpegQuality(100)
            .build()
    }

    private fun takePicture() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    bitmap = image.convertImageProxyToBitmap()
                    binding.imageView.setImageBitmap(bitmap)
                    //binding.imageView.rotation = 90.0f
                    binding.previewView.visibility = View.VISIBLE
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "Image capture failed ${exception.message}")
                }
            })

    }


}


/**
 * Attribution [Stackoverflow post]
 * (https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap)
 */

fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}