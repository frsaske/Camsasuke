package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.CameraFlashMode
import com.example.model.CameraLensFacing
import com.example.model.CameraSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // Lens facing state
    private val _lensFacing = MutableStateFlow(CameraLensFacing.BACK)
    val lensFacing: StateFlow<CameraLensFacing> = _lensFacing.asStateFlow()

    // Flash mode state
    private val _flashMode = MutableStateFlow(CameraFlashMode.AUTO)
    val flashMode: StateFlow<CameraFlashMode> = _flashMode.asStateFlow()

    // Screen flash simulation trigger for front camera
    private val _screenFlashTrigger = MutableStateFlow(false)
    val screenFlashTrigger: StateFlow<Boolean> = _screenFlashTrigger.asStateFlow()

    // Shutter animation trigger
    private val _shutterAnimTrigger = MutableStateFlow(false)
    val shutterAnimTrigger: StateFlow<Boolean> = _shutterAnimTrigger.asStateFlow()

    // Video recording states
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    // Capabilities
    private val _hasFlashUnit = MutableStateFlow(false)
    val hasFlashUnit: StateFlow<Boolean> = _hasFlashUnit.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L

    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _lensFacing.value = if (_lensFacing.value == CameraLensFacing.BACK) {
            CameraLensFacing.FRONT
        } else {
            CameraLensFacing.BACK
        }
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun setFlashMode(mode: CameraFlashMode) {
        _flashMode.value = mode
        applyFlashMode()
    }

    private fun applyFlashMode() {
        val capture = imageCapture ?: return
        if (_lensFacing.value == CameraLensFacing.FRONT) {
            capture.flashMode = ImageCapture.FLASH_MODE_OFF
            return
        }

        capture.flashMode = when (_flashMode.value) {
            CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
            CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    @SuppressLint("RestrictedApi")
    fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val selector = if (_lensFacing.value == CameraLensFacing.BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // Preview
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        applyFlashMode()

        // VideoCapture
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.SD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture,
                videoCapture
            )

            // Inspect capabilities
            camera?.cameraInfo?.let { info ->
                _hasFlashUnit.value = info.hasFlashUnit()
                info.zoomState.observe(lifecycleOwner) { state ->
                    _zoomRatio.value = state.zoomRatio
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Use cases binding failed with video, retrying without video", e)
            try {
                // Fallback to photo only if device cannot bind both together
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Camera bind fallback failed", ex)
            }
        }
    }

    /**
     * Tap to focus implementation.
     */
    fun tapToFocus(factory: MeteringPointFactory, x: Float, y: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cameraControl.startFocusAndMetering(action)
    }

    private var currentLinearZoom = 0f

    /**
     * Zoom In / Zoom Out step controls for the viewfinder buttons (+ / -).
     */
    fun zoomIn() {
        currentLinearZoom = (currentLinearZoom + 0.18f).coerceAtMost(1f)
        camera?.cameraControl?.setLinearZoom(currentLinearZoom)
    }

    fun zoomOut() {
        currentLinearZoom = (currentLinearZoom - 0.18f).coerceAtLeast(0f)
        camera?.cameraControl?.setLinearZoom(currentLinearZoom)
    }

    fun setLinearZoom(zoom: Float) {
        currentLinearZoom = zoom.coerceIn(0f, 1f)
        camera?.cameraControl?.setLinearZoom(currentLinearZoom)
    }

    /**
     * Captures photo, processes with RetroImageProcessor, and saves to MediaStore.
     */
    fun takePhoto(
        settings: CameraSettings,
        onCaptureSuccess: (Uri, String, Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera is not ready")
            return
        }

        // Trigger screen flash animation if front camera & flash is ON or AUTO
        val isFront = _lensFacing.value == CameraLensFacing.FRONT
        if (isFront && (_flashMode.value == CameraFlashMode.ON || _flashMode.value == CameraFlashMode.AUTO)) {
            _screenFlashTrigger.value = true
        }
        _shutterAnimTrigger.value = true

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    coroutineScope.launch {
                        try {
                            val bitmap = imageProxyToBitmap(imageProxy)
                            imageProxy.close()

                            // Turn off screen flash
                            _screenFlashTrigger.value = false

                            // Apply retro processing
                            val processed = RetroImageProcessor.processImage(
                                source = bitmap,
                                settings = settings,
                                isFrontCamera = isFront
                            )

                            // Save to MediaStore
                            val filename = mediaStoreManager.getNextPhotoFilename()
                            val saveResult = mediaStoreManager.saveBitmap(
                                bitmap = processed,
                                quality = settings.jpegQuality,
                                customFilename = filename
                            )

                            saveResult.onSuccess { uri ->
                                withContext(Dispatchers.Main) {
                                    onCaptureSuccess(uri, filename, processed.width, processed.height)
                                }
                            }.onFailure { err ->
                                withContext(Dispatchers.Main) {
                                    onError(err.message ?: "Failed to save photo")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in image processing", e)
                            _screenFlashTrigger.value = false
                            withContext(Dispatchers.Main) {
                                onError(e.message ?: "Processing error")
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    _screenFlashTrigger.value = false
                    coroutineScope.launch(Dispatchers.Main) {
                        onError(exception.message ?: "Capture failed")
                    }
                }
            }
        )
    }

    /**
     * Start video recording.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        hasAudioPermission: Boolean,
        settings: CameraSettings,
        onRecordingStarted: () -> Unit,
        onRecordingSaved: (Uri, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val vCapture = videoCapture ?: run {
            onError("Video recording is not supported on this camera")
            return
        }

        if (activeRecording != null) {
            onError("Recording already in progress")
            return
        }

        val tempVideoFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        currentRecordingFile = tempVideoFile

        val outputOptions = FileOutputOptions.Builder(tempVideoFile).build()
        val pendingRecording: PendingRecording = vCapture.output.prepareRecording(context, outputOptions)

        if (hasAudioPermission && settings.recordAudio) {
            try {
                pendingRecording.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(TAG, "Audio recording permission missing", e)
            }
        }

        recordingStartTime = SystemClock.elapsedRealtime()

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _isRecording.value = true
                    _recordingDurationSeconds.value = 0L
                    onRecordingStarted()
                    startTimer()
                }
                is VideoRecordEvent.Finalize -> {
                    _isRecording.value = false
                    activeRecording = null
                    val file = currentRecordingFile
                    if (!event.hasError() && file != null && file.exists() && file.length() > 0) {
                        coroutineScope.launch {
                            val result = mediaStoreManager.saveVideoFile(file)
                            result.onSuccess { uri ->
                                withContext(Dispatchers.Main) {
                                    onRecordingSaved(uri, file.name)
                                }
                            }.onFailure { err ->
                                withContext(Dispatchers.Main) {
                                    onError(err.message ?: "Failed to save video to Gallery")
                                }
                            }
                        }
                    } else {
                        val errMsg = event.cause?.message ?: "Recording stopped or interrupted"
                        file?.delete()
                        onError(errMsg)
                    }
                }
            }
        }
    }

    private fun startTimer() {
        coroutineScope.launch {
            while (_isRecording.value) {
                val elapsed = (SystemClock.elapsedRealtime() - recordingStartTime) / 1000L
                _recordingDurationSeconds.value = elapsed
                kotlinx.coroutines.delay(500)
            }
        }
    }

    /**
     * Stop video recording.
     */
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        _isRecording.value = false
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val bitmap = image.toBitmap()
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun dismissFlashAnim() {
        _shutterAnimTrigger.value = false
        _screenFlashTrigger.value = false
    }
}
