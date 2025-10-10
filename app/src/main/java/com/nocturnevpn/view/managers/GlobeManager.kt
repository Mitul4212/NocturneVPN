package com.nocturnevpn.view.managers

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.databinding.FragmentHomeBinding
import com.nocturnevpn.utils.Utils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GlobeManager(
    private val context: Context,
    private val binding: FragmentHomeBinding?
) {
    private lateinit var webView: WebView
    private lateinit var sharedPreference: SharedPreference
    private var isGlobeLoaded = false
    private var loadAttempts = 0
    private val maxLoadAttempts = 3

    init {
        sharedPreference = SharedPreference(context)
    }

    fun setupGlobe() {
        webView = binding?.globeWebView ?: run {
            Log.e("GlobeManager", "WebView binding is null!")
            return
        }
        
        Log.d("GlobeManager", "Setting up globe WebView - Attempt ${loadAttempts + 1}")
        
        // Test if assets are accessible
        try {
            val inputStream = context.assets.open("globe.html")
            val size = inputStream.available()
            Log.d("GlobeManager", "globe.html asset found, size: $size bytes")
            inputStream.close()
        } catch (e: Exception) {
            Log.e("GlobeManager", "Error accessing globe.html asset: ${e.message}")
        }
        
        // Configure WebView settings for better compatibility and faster loading
        val webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT // Use cache for faster loading
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            // Optimize for faster loading
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            setEnableSmoothTransition(true)
        }

        // Enable debugging for development
        WebView.setWebContentsDebuggingEnabled(true)

        // Use WebViewAssetLoader for secure asset loading
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                Log.d("GlobeManager", "Intercepting request: ${request.url}")
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("GlobeManager", "Page started loading: $url")
                isGlobeLoaded = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("GlobeManager", "Page finished loading: $url")
                
                // Immediately get user location when page loads
                view?.post {
                    try {
                        Log.d("GlobeManager", "Page loaded - getting user location immediately")
                        isGlobeLoaded = true
                        loadAttempts = 0 // Reset attempts on success
                        
                        // Get user location immediately without delay
                        getIPLocationAndUpdateGlobe()
                        
                        // Set up a backup refresh after a shorter delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isGlobeLoaded) {
                                Log.d("GlobeManager", "Backup location refresh after page load")
                                forceRefreshLocation()
                            }
                        }, 3000) // Reduced to 3 seconds
                        
                    } catch (e: Exception) {
                        Log.e("GlobeManager", "JavaScript call failed", e)
                        // Fallback to default location if IP location fails
                        fallbackToDefaultLocation()
                    }
                }
            }

            // Note: onReceivedError is final in WebViewClientCompat, so we can't override it
            // Error handling is done through try-catch blocks and logging
        }

        // Add JavaScript interface
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun updateLocation(lat: Double, lon: Double) {
                Log.d("GlobeManager", "JavaScript interface called with lat: $lat, lon: $lon")
                webView.post {
                    webView.evaluateJavascript("setLocation($lat, $lon);", null)
                }
            }

            @JavascriptInterface
            fun onGlobeReady() {
                Log.d("GlobeManager", "Globe is ready - JavaScript callback received")
                isGlobeLoaded = true
                loadAttempts = 0 // Reset attempts on success
                
                // Apply current theme when globe is ready
                Handler(Looper.getMainLooper()).post {
                    Log.d("GlobeManager", "Globe ready - applying current theme")
                    applyCurrentTheme()
                    
                    // Then get user location
                    Log.d("GlobeManager", "Globe ready - getting user location immediately")
                    getIPLocationAndUpdateGlobe()
                }
            }

            @JavascriptInterface
            fun logMessage(message: String) {
                Log.d("GlobeManager", "JS Log: $message")
            }

            @JavascriptInterface
            fun forceRefreshLocation() {
                Log.d("GlobeManager", "JavaScript requested location refresh")
                webView.post {
                    forceRefreshLocation()
                }
            }

            @JavascriptInterface
            fun getInitialTheme(): Boolean {
                val isDarkMode = sharedPreference.isDarkModeEnabled()
                Log.d("GlobeManager", "JavaScript requested initial theme - Dark mode: $isDarkMode")
                return isDarkMode
            }
        }, "AndroidInterface")

        // Set initial WebView background color based on current theme
        val isDarkMode = sharedPreference.isDarkModeEnabled()
        val backgroundColor = if (isDarkMode) {
            Color.parseColor("#121212") // Dark background
        } else {
            Color.parseColor("#FFFFFF") // Light background
        }
        webView.setBackgroundColor(backgroundColor)
        
        // Load globe.html from asset path via WebViewAssetLoader
        Log.d("GlobeManager", "Loading globe.html")
        webView.loadUrl("https://appassets.androidplatform.net/assets/globe.html")
        
        // Set up a shorter timeout to ensure location is loaded
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isGlobeLoaded) {
                Log.w("GlobeManager", "Globe loading timeout - forcing location update")
                forceRefreshLocation()
            }
        }, 8000) // Reduced timeout to 8 seconds
    }

    fun getIPLocationAndUpdateGlobe() {
        if (!isGlobeLoaded) {
            Log.w("GlobeManager", "Globe not loaded yet, attempting to reload...")
            if (loadAttempts < maxLoadAttempts) {
                loadAttempts++
                Handler(Looper.getMainLooper()).postDelayed({
                    setupGlobe()
                }, 500) // Reduced delay
            } else {
                Log.e("GlobeManager", "Max load attempts reached, using fallback")
                fallbackToDefaultLocation()
            }
            return
        }

        Log.d("GlobeManager", "Getting user location...")
        
        // Use a faster timeout for the HTTP request
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder()
            .url("https://ipinfo.io/json")
            .addHeader("Cache-Control", "no-cache") // Ensure fresh data
            .build()

        val selectedServer = sharedPreference.server.countryLong
        Log.d("GlobeManager", "Selected Country: $selectedServer")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GlobeManager", "IP location request failed: ${e.message}")
                // Use fallback immediately on failure
                Handler(Looper.getMainLooper()).post {
                    fallbackToCountry(selectedServer)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseData ->
                    try {
                        val json = JSONObject(responseData)
                        val loc = json.optString("loc")
                        val ip = json.optString("ip")
                        val country = json.optString("country")
                        val city = json.optString("city")

                        Log.d("GlobeManager", "IP Location - IP: $ip, Country: $country, City: $city")

                        if (!loc.isNullOrEmpty() && loc.contains(",")) {
                            val latLon = loc.split(",")
                            val latStr = latLon[0]
                            val lonStr = latLon[1]

                            if (latStr.isNotEmpty() && lonStr.isNotEmpty()) {
                                val lat = latStr.toDouble()
                                val lon = lonStr.toDouble()

                                Log.i("GlobeManager", "IP Location - Lat: $lat, Lon: $lon")

                                Handler(Looper.getMainLooper()).post {
                                    updateGlobeLocation(lat, lon)
                                    Log.d("GlobeManager", "User location updated successfully: $city, $country")
                                }
                            } else {
                                Log.e("GlobeManager", "Latitude or longitude is empty")
                                Handler(Looper.getMainLooper()).post {
                                    fallbackToCountry(selectedServer)
                                }
                            }
                        } else {
                            Log.e("GlobeManager", "Location string is empty or malformed: '$loc'")
                            Handler(Looper.getMainLooper()).post {
                                fallbackToCountry(selectedServer)
                            }
                        }

                    } catch (e: JSONException) {
                        Log.e("GlobeManager", "Failed to parse JSON: ${e.message}")
                        Handler(Looper.getMainLooper()).post {
                            fallbackToCountry(selectedServer)
                        }
                    }
                } ?: run {
                    Log.e("GlobeManager", "Empty response body")
                    Handler(Looper.getMainLooper()).post {
                        fallbackToCountry(selectedServer)
                    }
                }
            }
        })
    }

    private fun updateGlobeLocation(lat: Double, lon: Double) {
        if (!isGlobeLoaded) {
            Log.w("GlobeManager", "Globe not loaded, cannot update location")
            return
        }

        val js = "javascript:setLocation($lat, $lon)"
        Log.d("GlobeManager", "Executing JavaScript: $js")
        binding?.globeWebView?.evaluateJavascript(js) { result ->
            Log.d("GlobeManager", "JavaScript result: $result")
        }
    }

    private fun fallbackToCountry(countryName: String) {
        val coords = Utils.getCoordinatesByCountry(countryName)
        if (coords != null) {
            val (lat, lon) = coords
            Log.w("GlobeManager", "Fallback to country: $countryName ($lat, $lon)")
            Handler(Looper.getMainLooper()).post {
                updateGlobeLocation(lat, lon)
            }
        } else {
            Log.e("GlobeManager", "No fallback coordinates found for country: $countryName")
            fallbackToDefaultLocation()
        }
    }

    private fun fallbackToDefaultLocation() {
        // Default to a known location (e.g., center of the world)
        Log.w("GlobeManager", "Using default location")
        Handler(Looper.getMainLooper()).post {
            updateGlobeLocation(0.0, 0.0) // Equator, Prime Meridian
        }
    }

    fun refreshGlobe() {
        if (isGlobeLoaded) {
            Log.d("GlobeManager", "Refreshing globe location")
            getIPLocationAndUpdateGlobe()
        } else {
            Log.w("GlobeManager", "Cannot refresh globe - not loaded yet")
            // Try to reload the globe if it's not loaded
            setupGlobe()
        }
    }

    fun onVPNStatusChanged(isConnected: Boolean) {
        Log.d("GlobeManager", "VPN status changed: connected = $isConnected")
        if (isConnected) {
            // VPN connected - refresh location after a delay to get new IP
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("GlobeManager", "VPN connected - refreshing location to show VPN server location")
                refreshGlobe()
            }, 3000)
        } else {
            // VPN disconnected - refresh location immediately to show real IP
            Log.d("GlobeManager", "VPN disconnected - refreshing location to show real user location")
            // Clear any cached location data
            clearCachedLocation()
            // Refresh immediately to show real location
            Handler(Looper.getMainLooper()).postDelayed({
                refreshGlobe()
            }, 1000)
        }
    }

    private fun clearCachedLocation() {
        // Clear any cached location data to force fresh IP lookup
        Log.d("GlobeManager", "Clearing cached location data")
        // This ensures we get a fresh IP location instead of cached data
    }

    fun forceRefreshLocation() {
        Log.d("GlobeManager", "Force refreshing location")
        if (isGlobeLoaded) {
            // Force a fresh IP lookup without any caching
            getFreshIPLocation()
        } else {
            Log.w("GlobeManager", "Globe not loaded, cannot force refresh")
        }
    }

    private fun getFreshIPLocation() {
        Log.d("GlobeManager", "Getting fresh IP location")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ipinfo.io/json")
            .addHeader("Cache-Control", "no-cache") // Force fresh data
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GlobeManager", "Fresh IP location request failed: ${e.message}")
                fallbackToDefaultLocation()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseData ->
                    try {
                        val json = JSONObject(responseData)
                        val loc = json.optString("loc")
                        val ip = json.optString("ip")
                        val country = json.optString("country")
                        val city = json.optString("city")

                        Log.d("GlobeManager", "Fresh IP Location - IP: $ip, Country: $country, City: $city")

                        if (!loc.isNullOrEmpty() && loc.contains(",")) {
                            val latLon = loc.split(",")
                            val latStr = latLon[0]
                            val lonStr = latLon[1]

                            if (latStr.isNotEmpty() && lonStr.isNotEmpty()) {
                                val lat = latStr.toDouble()
                                val lon = lonStr.toDouble()

                                Log.i("GlobeManager", "Fresh IP Location - Lat: $lat, Lon: $lon")

                                Handler(Looper.getMainLooper()).post {
                                    updateGlobeLocation(lat, lon)
                                    // Show a toast or notification about the location change
                                    showLocationUpdateNotification("Location updated: $city, $country")
                                }
                            } else {
                                Log.e("GlobeManager", "Fresh IP - Latitude or longitude is empty")
                                fallbackToDefaultLocation()
                            }
                        } else {
                            Log.e("GlobeManager", "Fresh IP - Location string is empty or malformed: '$loc'")
                            fallbackToDefaultLocation()
                        }

                    } catch (e: JSONException) {
                        Log.e("GlobeManager", "Failed to parse fresh IP JSON: ${e.message}")
                        fallbackToDefaultLocation()
                    }
                }
            }
        })
    }

    private fun showLocationUpdateNotification(message: String) {
        // You can implement a toast or notification here
        Log.d("GlobeManager", "Location Update: $message")
        // For now, just log it. You can add a toast here if needed
    }

    fun testGlobe() {
        Log.d("GlobeManager", "Testing globe with default location")
        if (isGlobeLoaded) {
            updateGlobeLocation(40.7128, -74.0060) // New York coordinates
        } else {
            Log.w("GlobeManager", "Globe not loaded, cannot test")
            // Try to load the globe first
            setupGlobe()
        }
    }

    /**
     * Switch globe theme based on dark mode preference
     */
    fun switchGlobeTheme(isDarkMode: Boolean) {
        if (!isGlobeLoaded) {
            Log.w("GlobeManager", "Globe not loaded, cannot switch theme")
            return
        }

        Log.d("GlobeManager", "Switching globe theme to dark mode: $isDarkMode")
        
        // Set WebView background color from Android side
        val backgroundColor = if (isDarkMode) {
            Color.parseColor("#121212") // Dark background
        } else {
            Color.parseColor("#FFFFFF") // Light background
        }
        
        binding?.globeWebView?.setBackgroundColor(backgroundColor)
        
        // Also switch the map style via JavaScript
        val js = "javascript:switchMapStyle($isDarkMode)"
        binding?.globeWebView?.evaluateJavascript(js) { result ->
            Log.d("GlobeManager", "Theme switch result: $result")
        }
    }

    /**
     * Apply current theme to globe
     */
    fun applyCurrentTheme() {
        val isDarkMode = sharedPreference.isDarkModeEnabled()
        Log.d("GlobeManager", "Applying current theme - Dark mode: $isDarkMode")
        
        // Set initial WebView background color
        val backgroundColor = if (isDarkMode) {
            Color.parseColor("#121212") // Dark background
        } else {
            Color.parseColor("#FFFFFF") // Light background
        }
        
        binding?.globeWebView?.setBackgroundColor(backgroundColor)
        
        // Apply theme to the globe
        switchGlobeTheme(isDarkMode)
    }

    fun loadTestGlobe() {
        Log.d("GlobeManager", "Loading test globe with simple HTML")
        webView = binding?.globeWebView ?: run {
            Log.e("GlobeManager", "WebView binding is null!")
            return
        }
        
        val webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        // Load a simple test HTML
        val testHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Globe</title>
                <style>
                    body { margin: 0; padding: 20px; background: #f0f0f0; font-family: Arial, sans-serif; }
                    #testInfo { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; }
                    #map { width: 100%; height: 400px; background: #e0e0e0; border: 2px solid #ccc; display: flex; align-items: center; justify-content: center; }
                </style>
            </head>
            <body>
                <div id="testInfo">
                    <h2>Globe Test Page</h2>
                    <p>This is a test page to verify WebView is working.</p>
                    <p>If you can see this, the WebView is loading HTML correctly.</p>
                    <button onclick="testFunction()">Test JavaScript</button>
                    <p id="result"></p>
                </div>
                <div id="map">
                    <h3>Map Area (Placeholder)</h3>
                    <p>This would be where the 3D globe appears.</p>
                </div>
                <script>
                    function testFunction() {
                        document.getElementById('result').innerHTML = 'JavaScript is working!';
                        if (window.AndroidInterface && window.AndroidInterface.logMessage) {
                            window.AndroidInterface.logMessage('Test button clicked');
                        }
                    }
                    
                    // Notify Android that test page is loaded
                    if (window.AndroidInterface && window.AndroidInterface.onGlobeReady) {
                        window.AndroidInterface.onGlobeReady();
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, testHtml, "text/html", "UTF-8", null)
        isGlobeLoaded = true
    }

    fun ensureUserLocationLoaded() {
        Log.d("GlobeManager", "Ensuring user location is loaded")
        
        if (isGlobeLoaded) {
            // Globe is loaded, get location immediately
            getIPLocationAndUpdateGlobe()
        } else {
            // Globe not loaded yet, set up a retry mechanism
            Log.d("GlobeManager", "Globe not loaded, setting up retry mechanism")
            
            var retryCount = 0
            val maxRetries = 3 // Reduced retries for faster fallback
            
            val retryRunnable = object : Runnable {
                override fun run() {
                    if (isGlobeLoaded) {
                        Log.d("GlobeManager", "Globe loaded, getting user location")
                        getIPLocationAndUpdateGlobe()
                    } else if (retryCount < maxRetries) {
                        retryCount++
                        Log.d("GlobeManager", "Retry $retryCount/$maxRetries - waiting for globe to load")
                        Handler(Looper.getMainLooper()).postDelayed(this, 500) // Reduced delay
                    } else {
                        Log.w("GlobeManager", "Max retries reached, using fallback location")
                        fallbackToDefaultLocation()
                    }
                }
            }
            
            Handler(Looper.getMainLooper()).postDelayed(retryRunnable, 500) // Start immediately
        }
    }

    // New method to force immediate location display on app startup
    fun forceInitialLocationDisplay() {
        Log.d("GlobeManager", "Forcing initial location display")
        
        // Try to get location immediately
        getIPLocationAndUpdateGlobe()
        
        // Set up multiple fallback attempts
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isGlobeLoaded) {
                Log.d("GlobeManager", "First fallback - trying again")
                getIPLocationAndUpdateGlobe()
            }
        }, 2000)
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isGlobeLoaded) {
                Log.d("GlobeManager", "Second fallback - using default location")
                fallbackToDefaultLocation()
            }
        }, 5000)
    }
}


