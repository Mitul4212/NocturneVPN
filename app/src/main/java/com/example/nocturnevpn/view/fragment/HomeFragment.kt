package com.example.nocturnevpn.view.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.example.nocturnevpn.CheckInternetConnection
import com.example.nocturnevpn.R
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.databinding.FragmentHomeBinding
import com.example.nocturnevpn.model.Server
import com.example.nocturnevpn.utils.Utils
import com.example.nocturnevpn.utils.toast
import com.example.nocturnevpn.view.activitys.ChangeServerActivity
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.VpnStatus
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.chromium.base.ThreadUtils.runOnUiThread
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class HomeFragment : Fragment(), VpnStatus.StateListener {

    private lateinit var mContext: Context


    private var _binding : FragmentHomeBinding?= null
    private val binding get() = _binding
    private var connection: CheckInternetConnection? = null
    private var vpnStart = false
    private lateinit var webView: WebView

//    private lateinit var globalServer: Server
    private lateinit var vpnThread: OpenVPNThread
    private lateinit var vpnService: OpenVPNService
    private lateinit var sharedPreference: SharedPreference

    private var isServerSelected: Boolean = false
    var wasConnectedOnce = false

//    private lateinit var themeViewModel: ThemeViewModel



    private val getServerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedServer = result.data!!.getParcelableExtra<Server>("serverextra");

                if (selectedServer != null) {
                    // Save to SharedPreferences (ensure it's persisted)
                    sharedPreference.saveServer(selectedServer)

                    // Update UI with new server
                    updateServerUI(selectedServer)

                    Log.d("ServerRetrieve", "Selected Server IP: ${selectedServer.ipAddress}")
                    isServerSelected = true
                }


            }
        }

    private val vpnResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vpnResult ->
            if (vpnResult.resultCode == Activity.RESULT_OK) {
                //Permission granted, start the VPN
                startVpn()
            } else {
                mContext.toast("For a successful VPN connection, permission must be granted.")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vpnThread = OpenVPNThread()
        vpnService = OpenVPNService()
        connection = CheckInternetConnection()
        sharedPreference = SharedPreference(mContext)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout first
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    var isConnected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load country coordinates from JSON
        Utils.loadCountryCoordinates(requireContext())

        // Checking is vpn already running or not
        isServiceRunning()
        VpnStatus.initLogCache(mContext.cacheDir)

        binding?.goProButton?.setOnClickListener{
            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        }



//        themeViewModel = ViewModelProvider(requireActivity())[ThemeViewModel::class.java]
//
//        themeViewModel.triggerMapThemeChange.observe(viewLifecycleOwner) {
//            applyMapTheme()
//        }


        setupGlobe()

        webView = binding!!.globeWebView
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun updateLocation(lat: Double, lon: Double) {
                // Dynamically call the setLocation function in JavaScript
                webView.post {
                    webView.evaluateJavascript("setLocation($lat, $lon);", null)
                }
            }
        }, "AndroidInterface")



        val proLayout = view.findViewById<ConstraintLayout>(R.id.go_pro_button)
        val proText = view.findViewById<TextView>(R.id.go_pro) // Assuming you have a TextView inside the layout

        // Apply gradient text effect
        val paint = proText.paint
        val width = paint.measureText(proText.text.toString())
        proText.paint.shader = LinearGradient(
            0f, 0f, width, proText.textSize,
            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
            null, Shader.TileMode.REPEAT
        )

        // Click listeners with proper context

        //new code

        binding!!.chooseServer.setOnClickListener {
//            getServerResult.launch(
//                Intent(mContext, ChangeServerActivity::class.java)
//            )
            if (!vpnStart) {
                getServerResult.launch(
                    Intent(mContext, ChangeServerActivity::class.java)
                )
            } else {
                mContext.toast(resources.getString(R.string.disconnect_first))
            }
        }

        binding!!.connectButton.setOnClickListener {
            if(vpnStart){
                confirmDisconnect()
            }else{

                if (!vpnStart && isServerSelected) {
                    prepareVpn()
                } else if (!isServerSelected && !vpnStart) {
                    getServerResult.launch(
                        Intent(mContext, ChangeServerActivity::class.java)
                    )
                } else if (vpnStart && !isServerSelected) {
                    mContext.toast(resources.getString(R.string.disconnect_first))
                } else {
                    mContext.toast("Unable to connect the VPN")
                }

            }

        }


    }

    private fun getIPLocationAndUpdateGlobe() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ipinfo.io/json")
            .build()

        val selectedServer = this.sharedPreference.server.countryLong

        Log.d("SelectedServerLoction", "Selected Country: ${selectedServer}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("IPLocation", "Request failed: ${e.message}")
                fallbackToCountry(selectedServer)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseData ->
                    try {
                        val json = JSONObject(responseData)
                        val loc = json.optString("loc") // "latitude,longitude"


                        if (!loc.isNullOrEmpty() && loc.contains(",")) {
                            val latLon = loc.split(",")
                            val latStr = latLon[0]
                            val lonStr = latLon[1]

                            // using this for when loc is give empty string and cause app crash
                            if (latStr.isNotEmpty() && lonStr.isNotEmpty()) {
                                val lat = latStr.toDouble()
                                val lon = lonStr.toDouble()

                                Log.i("IPLocation", "Lat: $lat, Lon: $lon")

                                runOnUiThread {
                                    updateGlobeLocation(lat, lon)
                                }
                            } else {
                                Log.e("IPLocation", "Latitude or longitude is empty")
                                fallbackToCountry(selectedServer)
                            }
                        } else {
                            Log.e("IPLocation", "Location string is empty or malformed: '$loc'")
                            fallbackToCountry(selectedServer)
                        }

//                        val latLon = loc.split(",")
//                        val lat = latLon[0].toDouble()
//                        val lon = latLon[1].toDouble()
//
//                        Log.i("IPLocation", "Lat: $lat, Lon: $lon")
//                        runOnUiThread {
//                            updateGlobeLocation(lat, lon)
//                        }
                    } catch (e: JSONException) {
                        Log.e("IPLocation", "Failed to parse JSON: ${e.message}")
                        fallbackToCountry(selectedServer)
                    }
                }
            }
        })
    }

    private fun updateGlobeLocation(lat: Double, lon: Double) {
        val js = "javascript:setLocation($lat, $lon)"
        binding!!.globeWebView.evaluateJavascript(js, null)
    }

    private fun fallbackToCountry(countryName: String) {
//        val coords = countryCoordinates[countryName]
        val coords = Utils.getCoordinatesByCountry(countryName)
        if (coords != null) {
            val (lat, lon) = coords
            Log.w("IPLocation", "Fallback to country: $countryName ($lat, $lon)")
            runOnUiThread {
                updateGlobeLocation(lat, lon)
            }
        } else {
            Log.e("IPLocation", "No fallback coordinates found for country: $countryName")
        }
    }


//    private fun setupGlobe() {
//        val webSettings: WebSettings = binding!!.globeWebView.settings
//        webSettings.javaScriptEnabled = true
//        webSettings.domStorageEnabled = true
//        webSettings.allowFileAccess = true
//        webSettings.allowContentAccess = true
////        webSettings.allowFileAccessFromFileURLs = true
////        webSettings.allowUniversalAccessFromFileURLs = true
////        binding.globeWebView.setBackgroundColor(Color.TRANSPARENT)
////        binding.globeWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
//
//
//
//        // Enable remote debugging in Chrome
//        WebView.setWebContentsDebuggingEnabled(true)
//
//        // Setup WebViewAssetLoader to serve assets from assets folder
//        val assetLoader = WebViewAssetLoader.Builder()
//            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
//            .build()
//
//        // Load your local globe.html file from assets
////        binding.globeWebView.loadUrl("file:///android_asset/globe.html")      //when file loade with loacl save
//        binding!!.globeWebView.loadUrl("https://appassets.androidplatform.net/assets/globe.html")  //If globe.html loads JSON or other resources, change their URLs from relative or file:///android_asset/... paths to
//
//
//        //old code
//        // Ensure getIPLocationAndUpdateGlobe() is only called after the page has loaded
////        binding.globeWebView.webViewClient = object : android.webkit.WebViewClient() {
////            override fun onPageFinished(view: WebView?, url: String?) {
////                super.onPageFinished(view, url)
////                getIPLocationAndUpdateGlobe() // ✅ JS can be safely called now
////            }
////        }
//
//        binding!!.globeWebView.webViewClient = object : WebViewClientCompat() {
//            override fun shouldInterceptRequest(
//                view: WebView,
//                request: android.webkit.WebResourceRequest
//            ): android.webkit.WebResourceResponse? {
//                return assetLoader.shouldInterceptRequest(request.url)
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//
////                applyMapTheme()
//
//                // Safe to call JS after page load
//                getIPLocationAndUpdateGlobe()
//            }
//        }
//
////        binding.globeWebView.settings.javaScriptEnabled = true
////        binding.globeWebView.loadUrl("file:///android_asset/globe.html")
//    }

    private fun setupGlobe() {
        val webSettings = binding!!.globeWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // Optional: Allow CORS from file:// only if your HTML/JS uses local JSON or textures
        // webSettings.allowFileAccessFromFileURLs = true
        // webSettings.allowUniversalAccessFromFileURLs = true

        // ✅ Debugging enabled (only for development)
        WebView.setWebContentsDebuggingEnabled(true)

        // 🌐 Use WebViewAssetLoader for secure asset loading
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()

        binding!!.globeWebView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // ✅ Use a small delay to ensure all JS/3D context is loaded before injecting JS
                view?.postDelayed({
                    try {
                        getIPLocationAndUpdateGlobe()
                    } catch (e: Exception) {
                        Log.e("WebView", "JavaScript call failed", e)
                    }
                }, 500)
            }
        }

        // ✅ Load globe.html from asset path via WebViewAssetLoader
        binding!!.globeWebView.loadUrl("https://appassets.androidplatform.net/assets/globe.html")
    }


//    fun applyMapTheme() {
//        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
//        val mode = if (isDarkMode) "dark" else "light"
//        binding?.globeWebView?.evaluateJavascript("switchToMode('$mode')", null)
//    }



    private fun updateServerUI(server: Server?) {
        // Ensure the server is not null before trying to update the UI
        if (server != null) {
            // Update the UI elements with the server details
            binding?.serverFlagName?.text = server.getCountryLong()   // Assuming getCountryLong() returns the country name
            binding?.serverFlagDes?.text = server.getIpAddress()           // Assuming getIpAddress() returns the server IP address
            binding?.connectionIp?.text = server.getIpAddress()          // Update the second IP TextView (if necessary)


            // Mark the server as selected
            isServerSelected = true
        } else {
            // Handle the case when the server is null (e.g., display a message or clear the UI)
            binding?.serverFlagName?.text = "No server selected"
            binding?.serverFlagDes?.text = "N/A"
            binding?.connectionIp?.text = "N/A"


        }


    }

    private fun isServiceRunning() {
        setStatus(OpenVPNService.getStatus())
    }

    private fun getInternetStatus(): Boolean {
        return connection!!.netCheck(mContext)
    }

    fun setStatus(connectionState: String?) {
        if (connectionState != null) when (connectionState) {
            "DISCONNECTED" -> {
                status("Connect")
                vpnStart = false
                OpenVPNService.setDefaultStatus()
                binding!!.connectionTextStatus.text = "Disconnected"
            }
            "NOPROCESS" -> binding!!.connectionTextStatus.text = "Not Connected"
            "CONNECTED" -> {
                vpnStart = true // it will use after restart this activity
                status("Connected")
                binding!!.connectionTextStatus.text = "Connected"
            }
            "WAIT" -> binding!!.connectionTextStatus.text = "Waiting for server connection"
            "AUTH" -> binding!!.connectionTextStatus.text = "Authenticating server"
            "RECONNECTING" -> {
                status("Connecting")
                binding!!.connectionTextStatus.text = "Reconnecting..."
            }
            "NONETWORK" -> binding!!.connectionTextStatus.text = "No network connection"
        }
    }

    private fun status(status: String) {
        //update UI here
        when (status) {
            "Connect" -> {
                onDisconnectDone()
                getIPLocationAndUpdateGlobe()
                wasConnectedOnce = true
            }
            "Connecting" -> {
            }
            "Connected" -> {
                onConnectionDone()
                // After VPN connects
                Handler(Looper.getMainLooper()).postDelayed({
                    getIPLocationAndUpdateGlobe()

                }, 100) // Optional delay
            }
            "tryDifferentServer" -> {
            }
            "loading" -> {
            }
            "invalidDevice" -> {
            }
            "authenticationCheck" -> {
            }
        }
    }

    private fun prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {
                Log.d("OpenVPN", "Preparing VPN...")
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    vpnResult.launch(intent)
                } else {
                    Log.d("OpenVPN", "VPN already prepared, starting VPN")
                    startVpn()
                }
                status("Connecting")
            } else {
                mContext.toast("No Internet Connection")
            }
        } else if (stopVpn()) {
            mContext.toast("Disconnect Successfully")
        }
    }

    private fun confirmDisconnect() {
        val builder = AlertDialog.Builder(
            mContext
        )
        builder.setMessage(mContext.getString(R.string.connection_close_confirm))
        builder.setPositiveButton(
            mContext.getString(R.string.yes)
        ) { dialog, id -> stopVpn() }
        builder.setNegativeButton(
            mContext.getString(R.string.no)
        ) { dialog, id ->
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun stopVpn(): Boolean {
        try {
            OpenVPNThread.stop()
            status("Connect")
            vpnStart = false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun startVpn() {
        try {

            mContext.toast("Starting VPN...")
            val selectedServer = this.sharedPreference.getServer()


            if (selectedServer == null) {
                mContext.toast("No server selected.")
                return
            }

            val conf = selectedServer.getOvpnConfigData()

            if (conf.isNullOrEmpty()) {
                mContext.toast("VPN configuration data is missing.")
                return
            }

            // ✅ Log to confirm correct server is being used
            Log.d("VPN_START", "Starting VPN with server:")
            Log.d("VPN_START", "Country: ${selectedServer.getCountryLong()} (${selectedServer.getCountryShort()})")
            Log.d("VPN_START", "IP Address: ${selectedServer.getIpAddress()}")

            OpenVpnApi.startVpn(mContext, conf, selectedServer.getCountryShort(), "vpn", "vpn")
            binding!!.connectionTextStatus.text = "Connecting..."
            vpnStart = true
        } catch (exception: IOException) {
            exception.printStackTrace()
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }


    }

    /**
     * Broadcast receivers ***************************
     */

    var broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setStatus(intent.getStringExtra("state"))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            try {
                var duration = intent.getStringExtra("duration")
                var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                var byteIn = intent.getStringExtra("byteIn")
                var byteOut = intent.getStringExtra("byteOut")
                if (duration == null) duration = "00:00:00"
                if (lastPacketReceive == null) lastPacketReceive = "0"
                if (byteIn == null) byteIn = "0.0"
                if (byteOut == null) byteOut = "0.0"
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Update status UI
     * @param duration: running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn: incoming data
     * @param byteOut: outgoing data
     */
    fun updateConnectionStatus(
        duration: String,
        lastPacketReceive: String,
        byteIn: String,
        byteOut: String
    ) {
        binding!!.vpnConnectionTime.setText("$duration")
        binding!!.downloadSpeed.text = "$byteIn"
        binding!!.uploadSpeed.text = "$byteOut"
    }

    override fun onResume() {

        super.onResume()
        VpnStatus.addStateListener(this)

        val sharedPreference = SharedPreference(context)
        if (sharedPreference.isPrefsHasServer()) {
//                globalServer = sharedPreference.server
            val selectedServer = sharedPreference.getServer()
            //update selected server
            if (selectedServer != null) {
                Log.d("ServerRetrieve", "Retrieved server IP: ${selectedServer.ipAddress}")
                binding!!.serverFlagName.text = selectedServer.getCountryLong()
                binding!!.serverFlagDes.text = selectedServer.getIpAddress()
                binding!!.connectionIp.text = selectedServer.getIpAddress()
                isServerSelected = true
            }

        } else {
            binding!!.serverFlagName.text = resources.getString(R.string.country_name)
            binding!!.serverFlagDes.text = resources.getString(R.string.IP_address)

            binding!!.connectionIp.text = resources.getString(R.string.IP_address)
        }
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(
            broadcastReceiver!!
        )
        super.onPause()
        VpnStatus.removeStateListener(this)
    }

    /**
     * Save current selected server on local shared preference
     */
    override fun onStop() {

        val selectedServer = SharedPreference(mContext).getServer()
        if (selectedServer != null) {
            sharedPreference.saveServer(selectedServer)
        }
        super.onStop()
    }

    private fun onConnectionDone() {
//        binding!!.connectionTextBlock.visibility = View.GONE
//        binding!!.connectionButtonBlock.visibility = View.GONE
//        binding!!.serverSelectionBlock.visibility = View.GONE
//
//        binding!!.afterConnectionDetailBlock.visibility = View.VISIBLE
//        binding!!.disconnectButton.visibility = View.VISIBLE
    }

    private fun onDisconnectDone() {
//        binding!!.connectionTextBlock.visibility = View.VISIBLE
//        binding!!.connectionButtonBlock.visibility = View.VISIBLE
//        binding!!.serverSelectionBlock.visibility = View.VISIBLE
//        binding!!.afterConnectionDetailBlock.visibility = View.GONE
//        binding!!.disconnectButton.visibility = View.GONE
    }





    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        Intent: Intent?
    ) {
        requireActivity().runOnUiThread {
            when (state) {
                "CONNECTED" -> {
                    vpnStart = true
                    binding?.connectionTextStatus?.text = "Connected"
                    status("Connected")

                }
                "DISCONNECTED","NOPROCESS" -> {
                    vpnStart = false
//                    binding.statusText.text = "Disconnected"
                    // Show "Disconnected" only if VPN was previously connected
                    binding?.connectionTextStatus?.text = if (wasConnectedOnce) "Disconnected" else "Not Connected"

                    status("Connect")


                }
                "WAIT" -> binding?.connectionTextStatus?.text = "Waiting for server connection"
                "AUTH" -> binding?.connectionTextStatus?.text = "Authenticating"
                "RECONNECTING" -> binding?.connectionTextStatus?.text = "Reconnecting..."
                "NONETWORK" -> binding?.connectionTextStatus?.text = "No network"
                else -> binding?.connectionTextStatus?.text = state
            }
        }
    }

    override fun setConnectedVPN(uuid: String?) {
        // Check if the UUID is not null
        if (uuid != null) {
            // Update the UI with the connected VPN UUID (for example, display it)
//            binding.statusText.text = "Connected to VPN: $uuid"

            // Save this connection info in SharedPreferences
            val sharedPreferences =
                context?.getSharedPreferences("VPNPreferences", AppCompatActivity.MODE_PRIVATE)
            val editor = sharedPreferences?.edit()
            editor?.putString("connectedVPN", uuid)  // Save the UUID of the connected VPN
            editor?.apply()

            // Optionally, trigger any other actions when the VPN is connected, like updating a map or enabling/disabling features
            updateVPNStatus(true)  // This example assumes a method that updates the UI to show VPN is connected
        } else {
            // If UUID is null, update the UI to reflect disconnection
            binding?.connectionTextStatus?.text = "Not connected to VPN"
            updateVPNStatus(false)
        }
    }

    private fun updateVPNStatus(isConnected: Boolean) {
        // You can modify this method to update various UI components, such as a button or icon
        if (isConnected) {
            binding?.connectionTextStatus?.setTextColor(Color.BLACK)  // Example: green when connected
        } else {
            binding?.connectionTextStatus?.setTextColor(Color.RED)  // Example: red when disconnected
        }
    }

}














//package com.example.nocturnevpn.view.fragment
//
//import android.app.Activity
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.LinearGradient
//import android.graphics.Shader
//import android.net.VpnService
//import android.os.Bundle
//import android.os.RemoteException
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.fragment.app.Fragment
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import androidx.navigation.fragment.findNavController
//import com.example.nocturnevpn.CheckInternetConnection
//import com.example.nocturnevpn.R
//import com.example.nocturnevpn.SharedPreference
//import com.example.nocturnevpn.databinding.FragmentHomeBinding
//import com.example.nocturnevpn.model.Server
//import com.example.nocturnevpn.utils.toast
//import com.example.nocturnevpn.view.activitys.ChangeServerActivity
//import de.blinkt.openvpn.OpenVpnApi
//import de.blinkt.openvpn.core.ConnectionStatus
//import de.blinkt.openvpn.core.OpenVPNService
//import de.blinkt.openvpn.core.OpenVPNThread
//import de.blinkt.openvpn.core.VpnStatus
//import java.io.IOException
//
//
//class HomeFragment : Fragment(), VpnStatus.StateListener {
//
//    private lateinit var mContext: Context
//
//
//    private var _binding : FragmentHomeBinding?= null
//    private val binding get() = _binding
//    private var connection: CheckInternetConnection? = null
//    private var vpnStart = false
//
//    //    private lateinit var globalServer: Server
//    private lateinit var vpnThread: OpenVPNThread
//    private lateinit var vpnService: OpenVPNService
//    private lateinit var sharedPreference: SharedPreference
//
//    private var isServerSelected: Boolean = false
//    var wasConnectedOnce = false
//
//    private val getServerResult =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val selectedServer = result.data!!.getParcelableExtra<Server>("serverextra");
//
//                if (selectedServer != null) {
//                    // Save to SharedPreferences (ensure it's persisted)
//                    sharedPreference.saveServer(selectedServer)
//
//                    // Update UI with new server
//                    updateServerUI(selectedServer)
//
//                    Log.d("ServerRetrieve", "Selected Server IP: ${selectedServer.ipAddress}")
//                    isServerSelected = true
//                }
//
////                globalServer = selectedServer!!
////                // Save to SharedPreferences (ensure it's persisted)
////
////                //update selected server
////                binding!!.serverFlagName.text = selectedServer.getCountryLong()
////                binding!!.serverFlagDes.text = selectedServer.getIpAddress()
////
////
//////                binding!!.connectionIp.text = selectedServer.getIpAddress() if add ip adress in home dashbored then uncomment this
////                isServerSelected = true
//            }
//        }
//
//    private val vpnResult =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vpnResult ->
//            if (vpnResult.resultCode == Activity.RESULT_OK) {
//                //Permission granted, start the VPN
//                startVpn()
//            } else {
//                mContext.toast("For a successful VPN connection, permission must be granted.")
//            }
//        }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        mContext = context
//    }
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        vpnThread = OpenVPNThread()
//        vpnService = OpenVPNService()
//        connection = CheckInternetConnection()
//        sharedPreference = SharedPreference(mContext)
//
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout first
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding?.root
//    }
//
//    var isConnected = false
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Checking is vpn already running or not
//        isServiceRunning()
//        VpnStatus.initLogCache(mContext.cacheDir)
//
//        binding?.goProButton?.setOnClickListener{
//            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
//        }
//
////        val webView = view.findViewById<WebView>(R.id.backgroundWebView)
////        webView.settings.javaScriptEnabled = true
//////        webView.loadUrl("file:///android_asset/globe.html") // Or any other URL
////        webView.setBackgroundColor(Color.TRANSPARENT)
////        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
//
//
////        setupGlobe()
//
//
//
////        val chooseSerevr = view.findViewById<CardView>(R.id.choose_server)
////        val connectButton = view.findViewById<CardView>(R.id.connect_button)
//
//        val proLayout = view.findViewById<ConstraintLayout>(R.id.go_pro_button)
//        val proText = view.findViewById<TextView>(R.id.go_pro) // Assuming you have a TextView inside the layout
//
//        // Apply gradient text effect
//        val paint = proText.paint
//        val width = paint.measureText(proText.text.toString())
//        proText.paint.shader = LinearGradient(
//            0f, 0f, width, proText.textSize,
//            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
//            null, Shader.TileMode.REPEAT
//        )
//
//        // Click listeners with proper context
//
////        chooseSerevr.setOnClickListener{
////            Toast.makeText(requireContext(),"Choose server", Toast.LENGTH_SHORT).show()
////            val intent = Intent(requireActivity(), ChangeServerActivity::class.java)
////            startActivity(intent)
////        }
//
////        connectButton.setOnClickListener {
////
////            isConnected = !isConnected // Toggle state
////            val message = if (isConnected) "Connected" else "Disconnected"
////            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
////        }
////
////        connectButton.setOnClickListener{
////            Toast.makeText(requireContext(),"Connected", Toast.LENGTH_SHORT).show()
////        }
//
//
//        //new code
//
//        binding!!.chooseServer.setOnClickListener {
//            if (!vpnStart) {
//                getServerResult.launch(
//                    Intent(mContext, ChangeServerActivity::class.java)
//                )
//            } else {
//                mContext.toast(resources.getString(R.string.disconnect_first))
//            }
//        }
//
//        binding!!.connectButton.setOnClickListener {
//            if(vpnStart){
//                confirmDisconnect()
//            }else{
//
//                if (!vpnStart && isServerSelected) {
//                    prepareVpn()
//                } else if (!isServerSelected && !vpnStart) {
//                    getServerResult.launch(
//                        Intent(mContext, ChangeServerActivity::class.java)
//                    )
//                } else if (vpnStart && !isServerSelected) {
//                    mContext.toast(resources.getString(R.string.disconnect_first))
//                } else {
//                    mContext.toast("Unable to connect the VPN")
//                }
//
//            }
////            if (!vpnStart && isServerSelected) {
////                prepareVpn()
////            } else if (!isServerSelected && !vpnStart) {
////                getServerResult.launch(
////                    Intent(mContext, ChangeServerActivity::class.java)
////                )
////            } else if (vpnStart && !isServerSelected) {
////                mContext.toast(resources.getString(R.string.disconnect_first))
////            } else {
////                mContext.toast("Unable to connect the VPN")
////            }
//        }
//
////        binding!!.connectButton.setOnClickListener {
////            if (vpnStart) {
////                confirmDisconnect()
////            }
////        }
//
////       this is alrady define so " binding?.goProButton?.setOnClickListener{\n" +
////                "            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)\n" +
////                "        }"
////        i can not define this function
////        binding!!.subscriptionButton.setOnClickListener {
////            val subscriptionActivity = Intent(mContext, SubscriptionActivity::class.java)
////            startActivity(subscriptionActivity)
////        }
//
//    }
//
//    private fun updateServerUI(server: Server?) {
//        // Ensure the server is not null before trying to update the UI
//        if (server != null) {
//            // Update the UI elements with the server details
//            binding?.serverFlagName?.text = server.getCountryLong()   // Assuming getCountryLong() returns the country name
//            binding?.serverFlagDes?.text = server.getIpAddress()           // Assuming getIpAddress() returns the server IP address
//            binding?.connectionIp?.text = server.getIpAddress()          // Update the second IP TextView (if necessary)
//
//            // Mark the server as selected
//            isServerSelected = true
//        } else {
//            // Handle the case when the server is null (e.g., display a message or clear the UI)
//            binding?.serverFlagName?.text = "No server selected"
//            binding?.serverFlagDes?.text = "N/A"
//            binding?.connectionIp?.text = "N/A"
//        }
//    }
//
//    private fun isServiceRunning() {
//        setStatus(OpenVPNService.getStatus())
//    }
//
//    private fun getInternetStatus(): Boolean {
//        return connection!!.netCheck(mContext)
//    }
//
//    fun setStatus(connectionState: String?) {
//        if (connectionState != null) when (connectionState) {
//            "DISCONNECTED" -> {
//                status("Connect")
//                vpnStart = false
//                OpenVPNService.setDefaultStatus()
//                binding!!.connectionTextStatus.text = "Disconnected"
//            }
//            "NOPROCESS" -> binding!!.connectionTextStatus.text = "Not Connected"
//            "CONNECTED" -> {
//                vpnStart = true // it will use after restart this activity
//                status("Connected")
//                binding!!.connectionTextStatus.text = "Connected"
//            }
//            "WAIT" -> binding!!.connectionTextStatus.text = "Waiting for server connection"
//            "AUTH" -> binding!!.connectionTextStatus.text = "Authenticating server"
//            "RECONNECTING" -> {
//                status("Connecting")
//                binding!!.connectionTextStatus.text = "Reconnecting..."
//            }
//            "NONETWORK" -> binding!!.connectionTextStatus.text = "No network connection"
//        }
//    }
//
//    private fun status(status: String) {
//        //update UI here
//        when (status) {
//            "Connect" -> {
//                onDisconnectDone()
//                wasConnectedOnce = true
//            }
//            "Connecting" -> {
//            }
//            "Connected" -> {
//                onConnectionDone()
//            }
//            "tryDifferentServer" -> {
//            }
//            "loading" -> {
//            }
//            "invalidDevice" -> {
//            }
//            "authenticationCheck" -> {
//            }
//        }
//    }
//
//    private fun prepareVpn() {
//        if (!vpnStart) {
//            if (getInternetStatus()) {
//                Log.d("OpenVPN", "Preparing VPN...")
//                val intent = VpnService.prepare(context)
//                if (intent != null) {
//                    vpnResult.launch(intent)
//                } else {
//                    Log.d("OpenVPN", "VPN already prepared, starting VPN")
//                    startVpn()
//                }
//                status("Connecting")
//            } else {
//                mContext.toast("No Internet Connection")
//            }
//        } else if (stopVpn()) {
//            mContext.toast("Disconnect Successfully")
//        }
//    }
//
//    private fun confirmDisconnect() {
//        val builder = AlertDialog.Builder(
//            mContext
//        )
//        builder.setMessage(mContext.getString(R.string.connection_close_confirm))
//        builder.setPositiveButton(
//            mContext.getString(R.string.yes)
//        ) { dialog, id -> stopVpn() }
//        builder.setNegativeButton(
//            mContext.getString(R.string.no)
//        ) { dialog, id ->
//        }
//
//        val dialog = builder.create()
//        dialog.show()
//    }
//
//    private fun stopVpn(): Boolean {
//        try {
//            OpenVPNThread.stop()
//            status("Connect")
//            vpnStart = false
//            return true
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return false
//    }
//
//    private fun startVpn() {
//        try {
////            val conf = globalServer.getOvpnConfigData()
////            OpenVpnApi.startVpn(context, conf, globalServer.getCountryShort(), "vpn", "vpn")
////            binding!!.connectionTextStatus.text = "Connecting..."
////            vpnStart = true
//            mContext.toast("Starting VPN...")
//            val selectedServer = this.sharedPreference.getServer()
//
//
//            if (selectedServer == null) {
//                mContext.toast("No server selected.")
//                return
//            }
//
//            val conf = selectedServer.getOvpnConfigData()
//
//            if (conf.isNullOrEmpty()) {
//                mContext.toast("VPN configuration data is missing.")
//                return
//            }
//
//            // ✅ Log to confirm correct server is being used
//            Log.d("VPN_START", "Starting VPN with server:")
//            Log.d("VPN_START", "Country: ${selectedServer.getCountryLong()} (${selectedServer.getCountryShort()})")
//            Log.d("VPN_START", "IP Address: ${selectedServer.getIpAddress()}")
//
//            OpenVpnApi.startVpn(mContext, conf, selectedServer.getCountryShort(), "vpn", "vpn")
//            binding!!.connectionTextStatus.text = "Connecting..."
//            vpnStart = true
//        } catch (exception: IOException) {
//            exception.printStackTrace()
//        } catch (exception: RemoteException) {
//            exception.printStackTrace()
//        }
//
//
//    }
//
//    /**
//     * Broadcast receivers ***************************
//     */
//
//    var broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            try {
//                setStatus(intent.getStringExtra("state"))
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
//            try {
//                var duration = intent.getStringExtra("duration")
//                var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
//                var byteIn = intent.getStringExtra("byteIn")
//                var byteOut = intent.getStringExtra("byteOut")
//                if (duration == null) duration = "00:00:00"
//                if (lastPacketReceive == null) lastPacketReceive = "0"
//                if (byteIn == null) byteIn = "0.0"
//                if (byteOut == null) byteOut = "0.0"
//                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    /**
//     * Update status UI
//     * @param duration: running time
//     * @param lastPacketReceive: last packet receive time
//     * @param byteIn: incoming data
//     * @param byteOut: outgoing data
//     */
//    fun updateConnectionStatus(
//        duration: String,
//        lastPacketReceive: String,
//        byteIn: String,
//        byteOut: String
//    ) {
//        binding!!.vpnConnectionTime.setText("$duration")
//        binding!!.downloadSpeed.text = "$byteIn"
//        binding!!.uploadSpeed.text = "$byteOut"
//    }
//
//    override fun onResume() {
////        LocalBroadcastManager.getInstance(mContext).registerReceiver(
////            broadcastReceiver!!, IntentFilter("connectionState")
////        )
////        if (!this::globalServer.isInitialized) {
////            if (sharedPreference.isPrefsHasServer) {
//////                globalServer = sharedPreference.server
////                val selectedServer = sharedPreference.getServer()
////                //update selected server
////                binding!!.serverFlagName.text = selectedServer.getCountryLong()
////                binding!!.serverFlagDes.text = selectedServer.getIpAddress()
////
////                binding!!.connectionIp.text = selectedServer.getIpAddress()
////                isServerSelected = true
////
////            } else {
////                binding!!.serverFlagName.text = resources.getString(R.string.country_name)
////                binding!!.serverFlagDes.text = resources.getString(R.string.IP_address)
////
////                binding!!.connectionIp.text = resources.getString(R.string.IP_address)
////            }
////        }
//        super.onResume()
//        VpnStatus.addStateListener(this)
//
//        val sharedPreference = SharedPreference(context)
//        if (sharedPreference.isPrefsHasServer()) {
////                globalServer = sharedPreference.server
//            val selectedServer = sharedPreference.getServer()
//            //update selected server
//            if (selectedServer != null) {
//                Log.d("ServerRetrieve", "Retrieved server IP: ${selectedServer.ipAddress}")
//                binding!!.serverFlagName.text = selectedServer.getCountryLong()
//                binding!!.serverFlagDes.text = selectedServer.getIpAddress()
//                binding!!.connectionIp.text = selectedServer.getIpAddress()
//                isServerSelected = true
//            }
//
//        } else {
//            binding!!.serverFlagName.text = resources.getString(R.string.country_name)
//            binding!!.serverFlagDes.text = resources.getString(R.string.IP_address)
//
//            binding!!.connectionIp.text = resources.getString(R.string.IP_address)
//        }
//    }
//
//    override fun onPause() {
//        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(
//            broadcastReceiver!!
//        )
//        super.onPause()
//        VpnStatus.removeStateListener(this)
//    }
//
//    /**
//     * Save current selected server on local shared preference
//     */
//    override fun onStop() {
////        if (this::globalServer.isInitialized) {
////            sharedPreference.saveServer(globalServer)
////        }
//        val selectedServer = SharedPreference(mContext).getServer()
//        if (selectedServer != null) {
//            sharedPreference.saveServer(selectedServer)
//        }
//        super.onStop()
//    }
//
//    private fun onConnectionDone() {
////        binding!!.connectionTextBlock.visibility = View.GONE
////        binding!!.connectionButtonBlock.visibility = View.GONE
////        binding!!.serverSelectionBlock.visibility = View.GONE
////
////        binding!!.afterConnectionDetailBlock.visibility = View.VISIBLE
////        binding!!.disconnectButton.visibility = View.VISIBLE
//    }
//
//    private fun onDisconnectDone() {
////        binding!!.connectionTextBlock.visibility = View.VISIBLE
////        binding!!.connectionButtonBlock.visibility = View.VISIBLE
////        binding!!.serverSelectionBlock.visibility = View.VISIBLE
////        binding!!.afterConnectionDetailBlock.visibility = View.GONE
////        binding!!.disconnectButton.visibility = View.GONE
//    }
//
//
//    //    private fun setupGlobe() {
////        val webView = view?.findViewById<WebView>(R.id.backgroundWebView)
////        val webSettings: WebSettings = webView!!.settings
////        webSettings.javaScriptEnabled = true
////        webSettings.domStorageEnabled = true
////        webSettings.allowFileAccess = true
////        webSettings.allowContentAccess = true
//////        binding.globeWebView.setBackgroundColor(Color.TRANSPARENT)
//////        binding.globeWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
////
////
////        // Enable remote debugging in Chrome
////        WebView.setWebContentsDebuggingEnabled(true)
////
////        // Setup WebViewAssetLoader to serve assets from assets folder
////        val assetLoader = WebViewAssetLoader.Builder()
////            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
////            .build()
////
////        // Load your local globe.html file from assets
//////        binding.globeWebView.loadUrl("file:///android_asset/globe.html")      //when file loade with loacl save
////        if (webView != null) {
////            webView.loadUrl("https://appassets.androidplatform.net/assets/globe.html")
////        }  //If globe.html loads JSON or other resources, change their URLs from relative or file:///android_asset/... paths to
////
////
////        //old code
////        // Ensure getIPLocationAndUpdateGlobe() is only called after the page has loaded
//////        binding.globeWebView.webViewClient = object : android.webkit.WebViewClient() {
//////            override fun onPageFinished(view: WebView?, url: String?) {
//////                super.onPageFinished(view, url)
//////                getIPLocationAndUpdateGlobe() // ✅ JS can be safely called now
//////            }
//////        }
////
////        if (webView != null) {
////            webView.webViewClient = object : WebViewClientCompat() {
////                override fun shouldInterceptRequest(
////                    view: WebView,
////                    request: android.webkit.WebResourceRequest
////                ): android.webkit.WebResourceResponse? {
////                    return assetLoader.shouldInterceptRequest(request.url)
////                }
////
////                override fun onPageFinished(view: WebView?, url: String?) {
////                    super.onPageFinished(view, url)
////                    // Safe to call JS after page load
////    //                    getIPLocationAndUpdateGlobe()
////                }
////            }
////        }
////
//////        binding.globeWebView.settings.javaScriptEnabled = true
//////        binding.globeWebView.loadUrl("file:///android_asset/globe.html")    }
////
////    }
//    companion object {
//
//    }
//
//    override fun updateState(
//        state: String?,
//        logmessage: String?,
//        localizedResId: Int,
//        level: ConnectionStatus?,
//        Intent: Intent?
//    ) {
//        requireActivity().runOnUiThread {
//            when (state) {
//                "CONNECTED" -> {
//                    vpnStart = true
//                    binding?.connectionTextStatus?.text = "Connected"
//                    status("Connected")
//
//                }
//                "DISCONNECTED","NOPROCESS" -> {
//                    vpnStart = false
////                    binding.statusText.text = "Disconnected"
//                    // Show "Disconnected" only if VPN was previously connected
//                    binding?.connectionTextStatus?.text = if (wasConnectedOnce) "Disconnected" else "Not Connected"
//
//                    status("Connect")
//
//
//                }
//                "WAIT" -> binding?.connectionTextStatus?.text = "Waiting for server connection"
//                "AUTH" -> binding?.connectionTextStatus?.text = "Authenticating"
//                "RECONNECTING" -> binding?.connectionTextStatus?.text = "Reconnecting..."
//                "NONETWORK" -> binding?.connectionTextStatus?.text = "No network"
//                else -> binding?.connectionTextStatus?.text = state
//            }
//        }
//    }
//
//    override fun setConnectedVPN(uuid: String?) {
//        // Check if the UUID is not null
//        if (uuid != null) {
//            // Update the UI with the connected VPN UUID (for example, display it)
////            binding.statusText.text = "Connected to VPN: $uuid"
//
//            // Save this connection info in SharedPreferences
//            val sharedPreferences = getSharedPreference("VPNPreferences", AppCompatActivity.MODE_PRIVATE)
//            val editor = sharedPreferences.edit()
//            editor.putString("connectedVPN", uuid)  // Save the UUID of the connected VPN
//            editor.apply()
//
//            // Optionally, trigger any other actions when the VPN is connected, like updating a map or enabling/disabling features
//            updateVPNStatus(true)  // This example assumes a method that updates the UI to show VPN is connected
//        } else {
//            // If UUID is null, update the UI to reflect disconnection
//            binding?.connectionTextStatus?.text = "Not connected to VPN"
//            updateVPNStatus(false)
//        }
//    }
//
//    private fun updateVPNStatus(isConnected: Boolean) {
//        // You can modify this method to update various UI components, such as a button or icon
//        if (isConnected) {
//            binding?.connectionTextStatus?.setTextColor(Color.BLACK)  // Example: green when connected
//        } else {
//            binding?.connectionTextStatus?.setTextColor(Color.RED)  // Example: red when disconnected
//        }
//    }
//
//}














//package com.example.nocturnevpn.fragment
//
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.LinearGradient
//import android.graphics.Shader
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.cardview.widget.CardView
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.example.nocturnevpn.R
//import com.example.nocturnevpn.ServerActivity
//import com.example.nocturnevpn.databinding.FragmentHomeBinding
//
//
//class HomeFragment : Fragment() {
//
//    private var _binding : FragmentHomeBinding?= null
//    private val binding get() = _binding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout first
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding?.root
//    }
//
//    var isConnected = false
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding?.goProButton?.setOnClickListener{
//            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
//        }
//
//
//
//        val chooseSerevr = view.findViewById<CardView>(R.id.choose_server)
//        val connectButton = view.findViewById<CardView>(R.id.connect_button)
//
//        val proLayout = view.findViewById<LinearLayout>(R.id.go_pro_button)
//        val proText = view.findViewById<TextView>(R.id.go_pro) // Assuming you have a TextView inside the layout
//
//        // Apply gradient text effect
//        val paint = proText.paint
//        val width = paint.measureText(proText.text.toString())
//        proText.paint.shader = LinearGradient(
//            0f, 0f, width, proText.textSize,
//            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
//            null, Shader.TileMode.REPEAT
//        )
//
//        // Click listeners with proper context
//
//        chooseSerevr.setOnClickListener{
//            Toast.makeText(requireContext(),"Choose server", Toast.LENGTH_SHORT).show()
//            val intent = Intent(requireActivity(), ServerActivity::class.java)
//            startActivity(intent)
//        }
//
//        connectButton.setOnClickListener {
//
//            isConnected = !isConnected // Toggle state
//            val message = if (isConnected) "Connected" else "Disconnected"
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//        }
//
////        connectButton.setOnClickListener{
////            Toast.makeText(requireContext(),"Connected", Toast.LENGTH_SHORT).show()
////        }
//
//    }
//
//    companion object {
//
//    }
//}