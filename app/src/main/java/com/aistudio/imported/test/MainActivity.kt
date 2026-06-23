package com.aistudio.imported.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.webkit.PermissionRequest
import android.net.Uri
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    // File gallery/upload support callback & launcher
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val pickFilesLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            var results: Array<Uri>? = null
            if (data != null) {
                val dataString = data.dataString
                val clipData = data.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
            fileChooserCallback?.onReceiveValue(results)
        } else {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = null
    }

    // Dynamic WebView camera/microphone permission callback & launcher
    private var pendingWebViewPermissionRequest: PermissionRequest? = null
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val request = pendingWebViewPermissionRequest
        if (request != null) {
            val resourcesToGrant = mutableListOf<String>()
            for (res in request.resources) {
                if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && grantMap[android.Manifest.permission.CAMERA] == true) {
                    resourcesToGrant.add(res)
                } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && grantMap[android.Manifest.permission.RECORD_AUDIO] == true) {
                    resourcesToGrant.add(res)
                }
            }
            if (resourcesToGrant.isNotEmpty()) {
                request.grant(resourcesToGrant.toTypedArray())
            } else {
                request.deny()
            }
            pendingWebViewPermissionRequest = null
        }
    }

    // Dynamic Geolocation support callback & launcher
    private var pendingGeoCallback: android.webkit.GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private val geolocationLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val callback = pendingGeoCallback
        val origin = pendingGeoOrigin
        if (callback != null && origin != null) {
            val hasFine = grantMap[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val hasCoarse = grantMap[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasFine || hasCoarse) {
                callback.invoke(origin, true, false)
            } else {
                callback.invoke(origin, false, false)
            }
            pendingGeoCallback = null
            pendingGeoOrigin = null
        }
    }


    private val requestPermissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val permissions = mutableListOf<String>()
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(android.Manifest.permission.CAMERA)
        permissions.add(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add("android.permission.READ_MEDIA_IMAGES")
            permissions.add("android.permission.READ_MEDIA_VIDEO")
            permissions.add("android.permission.READ_MEDIA_AUDIO")
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.setGeolocationEnabled(true)
        
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoCallback = callback
                    pendingGeoOrigin = origin
                    geolocationLauncher.launch(arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val permissionsNeeded = mutableListOf<String>()
                for (res in request.resources) {
                    if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(android.Manifest.permission.CAMERA)
                        }
                    } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
                
                if (permissionsNeeded.isNotEmpty()) {
                    pendingWebViewPermissionRequest = request
                    permissionLauncher.launch(permissionsNeeded.toTypedArray())
                } else {
                    request.grant(request.resources)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                
                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                try {
                    pickFilesLauncher.launch(intent)
                } catch (e: Exception) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    return false
                }
                return true
            }
        }

        val nativeBridge = WebAppNativeBridge(this) {
            // On camera request: For build projects, invoke standard action.
        }
        webView.addJavascriptInterface(nativeBridge, "AndroidBridge")
        webView.addJavascriptInterface(nativeBridge, "Android")

        // Load offline client bundle root
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
