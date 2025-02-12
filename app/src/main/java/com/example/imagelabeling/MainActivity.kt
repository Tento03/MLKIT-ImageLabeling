package com.example.imagelabeling

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.imagelabeling.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var imageUri: Uri?=null
    private val pickImageLauncher=registerForActivityResult(ActivityResultContracts.GetContent()){ uri->
        uri?.let {
            imageUri=it
            binding.ivPost.setImageURI(it)
            analyzeImageFromGallery()
        }
    }

    private val cameraLauncher=registerForActivityResult(ActivityResultContracts.TakePicture()){ success->
        if (success){
            imageUri?.let {
                binding.ivPost.setImageURI(it)
                analyzeImageFromCamera()
            }
        }
    }

    private lateinit var imageLabeler:ImageLabeler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageLabeler=ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        binding.btnPickImage.setOnClickListener(){
            pickImageLauncher.launch("image/*")
        }
        binding.btnStartCamera.setOnClickListener(){
            var photoFile=createImageFile()
            photoFile?.also {
                imageUri=FileProvider.getUriForFile(this,"${applicationContext.packageName}.fileprovider",it)
                cameraLauncher.launch(imageUri)
            }
        }
        checkPermissions()
    }
    private fun analyzeImageFromGallery() {
        imageUri?.let {
            var image=InputImage.fromFilePath(this,it)
            imageLabeler.process(image)
                .addOnSuccessListener { labels->
                    var labelBuilder=StringBuilder()
                    for (label in labels){
                        var text=label.text
                        var confidence=label.confidence
                        labelBuilder.append("Name:$text, Confidence:$confidence\n")
                    }
                    binding.tvOutput.text=labelBuilder.toString()
                }
        }
    }

    private fun analyzeImageFromCamera() {
        imageUri?.let {
            var image=InputImage.fromFilePath(this,it)
            imageLabeler.process(image).addOnSuccessListener { labels->
                var labelBuilder=StringBuilder()
                for (label in labels){
                    var text=label.text
                    var confidence=label.confidence
                    labelBuilder.append("Name:$text, Confidence:$confidence\n")
                }
                binding.tvOutput.text=labelBuilder.toString()
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val CAMERA_PERMISSION_REQUEST_CODE=100
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val REQUEST_CAMERA_PERMISSION=100
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("ObjectDetection", "Permissions granted")
            } else {
                Log.e("ObjectDetection", "Permission denied")
            }
        }
    }
}