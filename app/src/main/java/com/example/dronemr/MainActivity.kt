package com.example.dronemr

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat
import com.example.dronemr.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.instrument.Gps
import com.parrot.drone.groundsdk.device.instrument.Altimeter
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.mavlink.ChangeSpeedCommand
import com.parrot.drone.groundsdk.mavlink.LandCommand
import com.parrot.drone.groundsdk.mavlink.MavlinkCommand
import com.parrot.drone.groundsdk.mavlink.MavlinkFiles
import com.parrot.drone.groundsdk.mavlink.NavigateToWaypointCommand
import com.parrot.drone.groundsdk.mavlink.ReturnToLaunchCommand
import com.parrot.drone.groundsdk.mavlink.TakeOffCommand
import com.parrot.drone.groundsdk.mavlink.standard.NavigateToWaypointCommand.Companion
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.json.JSONObject.NULL
import java.io.File

const val TAG = "Sussy"

class MainActivity : AppCompatActivity(), GoogleMap.OnMapClickListener,
GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback, View.OnClickListener {

    private val droneIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.black)
        BitmapHelper.vectorToBitmap(this, R.drawable.drone_icon, color)
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var groundSdk: GroundSdk

    /** get Drone **/
    private var drone: Drone? = null

    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView

    /** Drone battery charge level text view. */
    private lateinit var droneBatteryTxt: TextView

    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null

    /** Reference to the current drone battery info instrument. */
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null

    /** Reference to the current Gps info instrument */
    private var droneGPSInfoRef: Ref<Gps>? = null
    private lateinit var latitudeTxt: TextView
    private lateinit var longitudeTxt: TextView
    private lateinit var numberOfSatellites: TextView
    private var droneAltitudeInfoRef: Ref<Altimeter>? = null
    private var droneMarker: Marker? = null
    private lateinit var mavlinkFile: File

    /** Reference to the current altitude info instrument */
    //private var droneAltitudeInfoRef : Ref<Altimeter>? = null
    private lateinit var altitudeTxt: TextView

    /** Reference to the current flightPlan info instrument */
    private var flightPlanPilotingItfRef: Ref<FlightPlanPilotingItf>? = null

    /** Current remote control instance. */
    private var rc: RemoteControl? = null

    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null

    /** Reference to the current remote control battery info instrument. */
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null

    /** Remote state level text view. */
    private lateinit var rcStateTxt: TextView

    /** Remote battery charge level text view. */
    private lateinit var rcBatteryTxt: TextView

    /** Take off / land button. */
    private lateinit var takeOffLandBt: Button

    /** Reference to a current drone piloting interface. */
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null

    /**map */
    private lateinit var mMap: GoogleMap

    /**list of markers added */
    private val markers: MutableList<Marker> = arrayListOf()

    /**buttons */
    private lateinit var config: Button
    private lateinit var generate: Button
    private lateinit var start: Button
    private lateinit var stop: Button

    //mission settings
    private val missionList = mutableListOf<MavlinkCommand>()
    private val waypointList = mutableListOf<LatLng>()
    private var mAltitude: Double = 3.0
    private var mSpeed: Double = 3.0
    private var mFinishedAction: String = "autoland"

    //Http connection
    private var client = OkHttpClient()
    private lateinit var request : OkHttpRequest

    //Camera option to follow drone
    private var followingDrone : Boolean = false

    //JSON to send position to server
    private var team: String = "test"
    private var auth: String = "egtj-3jqa-z6fh-ete7-wrml"
    private var source: String = "3_AIR_DRONE-PATROLLER"
    private var serverUrl: String = "https://6bus5bof45.execute-api.eu-west-3.amazonaws.com/dev/trackers"
    private lateinit var position: JSONObject
    private lateinit var positionJSON: JSONObject

    //handler to send data to server every X second
    private var mHandler: Handler? = null
    /**
    private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer.
        val clusterManager = ClusterManager<Place>(this, googleMap)
        clusterManager.renderer =
            PlaceRenderer(
                this,
                googleMap,
                clusterManager
            )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(MarkerInfoWindowAdapter(this))

        // Add the places to the ClusterManager.

        clusterManager.cluster()

        clusterManager.setOnClusterItemClickListener { item ->

            return@setOnClusterItemClickListener false
        }


        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        googleMap.setOnCameraIdleListener {
            clusterManager.onCameraIdle()
        }
    }
    */

    /**
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }
    */

    /**
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Construct a FusedLocationProviderClient.
        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        setSupportActionBar(binding.appBarMain.toolbar)


        setSupportActionBar(binding.appBarMain.toolbar)

        //Button with mail
        binding.appBarMain.fabFollow.setOnClickListener {
        //Snackbar.make(view, "Follow drone", Snackbar.LENGTH_LONG)
        //.setAction("Action", null).show()
            if(followingDrone) {
                val myToast = Toast.makeText(this, "stopped following drone", Toast.LENGTH_SHORT)
                myToast.show()

            }
            else {
                val myToast = Toast.makeText(this, "started following drone", Toast.LENGTH_SHORT)
                myToast.show()
                val zoomLevel = 18.0.toFloat()
                val cu = CameraUpdateFactory.zoomTo(zoomLevel)
                mMap.animateCamera(cu)
            }

            followingDrone = !followingDrone
        }

        binding.appBarMain.fabFindUser.setOnClickListener {
            //Snackbar.make(view, "Finding user", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()
            val drone = drone
            if (drone == null) {
                val myToast = Toast.makeText(this, "no drone connected", Toast.LENGTH_SHORT)
                myToast.show()
            } else {
                droneGPSInfoRef = drone.getInstrument(Gps::class.java) { gps ->
                    gps?.lastKnownLocation().let { location ->
                        if (location != null) {
                            cameraUpdate(location.latitude, location.longitude, true)
                        } else {
                            val myToast = Toast.makeText(this, "location not found", Toast.LENGTH_SHORT)
                            myToast.show()
                        }
                    }
                }
            }
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Get user interface instances.
        droneStateTxt = findViewById(R.id.droneStateTxt)
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt)
        rcStateTxt = findViewById(R.id.rcStateTxt)
        rcBatteryTxt = findViewById(R.id.rcBatteryTxt)
        takeOffLandBt = findViewById(R.id.takeOffLandBt)
        takeOffLandBt.setOnClickListener { onTakeOffLandClick() }
        latitudeTxt = findViewById(R.id.labelDroneLat)
        longitudeTxt = findViewById(R.id.labelDroneLng)
        altitudeTxt = findViewById(R.id.altitudeTxt)
        numberOfSatellites = findViewById(R.id.numberOfSatellitesTxt)


        config = findViewById(R.id.config)
        generate = findViewById(R.id.generate)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)



        config.setOnClickListener(this)
        generate.setOnClickListener(this)
        start.setOnClickListener(this)
        stop.setOnClickListener(this)
        start.isEnabled = false
        stop.isEnabled = false



        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.

        position = JSONObject()
        position.put("latitude", NULL)
        position.put("longitude", NULL)

        positionJSON = JSONObject()
        positionJSON.put("team", team)
        positionJSON.put("auth",auth)
        positionJSON.put("source", source)
        positionJSON.put("position", position)
        positionJSON.put("altitude", NULL)
        positionJSON.put("timestamp", System.currentTimeMillis())

        //initial request
        request = OkHttpRequest(client)

        //handler
        mHandler = Handler(mainLooper)



    }

    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let {
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {

                    if (drone != null) {
                        // Stop monitoring the old drone.
                        stopDroneMonitors()

                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if (drone != null) {
                        //sendLocationToServer(positionJSON.toString(), serverUrl)
                        startDroneMonitors()
                    }
                }
                // If the remote control has changed.
                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        // Stop monitoring the old remote.
                        stopRcMonitors()

                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()
        // Monitor drone battery charge level.
        monitorDroneBatteryChargeLevel()
        // Monitor piloting interface.
        monitorPilotingInterface()
        // Monitor drone GPS
        monitorDroneGPS()
        // Monitor drone Altitude
        monitorDroneAltitude()
        //send data
        //positionJSON.put("timestamp", System.currentTimeMillis())
        //sendLocationToServer(positionJSON.toString(), serverUrl)
        startSending()
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        droneStateRef?.close()
        droneStateRef = null

        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null

        droneGPSInfoRef?.close()
        droneGPSInfoRef = null

        droneAltitudeInfoRef?.close()
        droneAltitudeInfoRef = null
    }

    /**
     * Monitors current drone battery charge level.
     */
    private fun monitorDroneBatteryChargeLevel() {
        // Monitor the battery info instrument.
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                droneBatteryTxt.text = "${it.charge} %"
            }
        }
    }


    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
        // Monitor current drone state.
        droneStateRef = drone?.getState {
            // Called at each drone state update.

            it?.let {
                // Update drone connection state view.
                droneStateTxt.text = it.connectionState.toString()

            }
        }
    }

    /**
     * Monitors current drone piloting interface.
     */
    private fun monitorPilotingInterface() {
        // Monitor a piloting interface.
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            // Called when the manual copter piloting Interface is available
            // and when it changes.

            // Disable the button if the piloting interface is not available.
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }

        flightPlanPilotingItfRef = drone?.getPilotingItf(FlightPlanPilotingItf::class.java) {
            if (it != null) {
                manageAutoPilotingItfState(it)

            }
        }
    }
    private fun monitorDroneGPS() {
        droneGPSInfoRef = drone?.getInstrument(Gps::class.java) { gps ->
            gps?.lastKnownLocation().let { location ->
                ("lng: " + location?.latitude.toString()).also { latitudeTxt.text = it }
                ("lat: " + location?.longitude.toString()).also { longitudeTxt.text = it }
//                Log.d(TAG, "Updated Location: ${it?.latitude}, ${it?.longitude}")
                if (location != null) {
                    updateDroneLocation(location.latitude, location.longitude)
                    if(followingDrone){
                        cameraUpdate(location.latitude, location.longitude, false)
                    }
                    //update data to send to server
                    position.put("latitude", location.latitude)
                    position.put("longitude", location.longitude)
                    positionJSON.put("position", position)
                }
            }
            if (gps != null) {
                numberOfSatellites.text = gps.satelliteCount.toString()
            }
        }
    }

    private fun makeJSON(latitude: Double, longitude: Double) {

    }

    private fun updateDroneLocation(
        latitude: Double,
        longitude: Double
    ) { // this will draw the aircraft as it moves
        if (latitude.isNaN() || longitude.isNaN()) {
            return
        }

        val pos = LatLng(latitude, longitude)
        // the following will draw the aircraft on the screen
        val markerOptions = MarkerOptions()
            .title("drone")
            .position(pos)
            .anchor(0.5F, 0.5F)
            .icon(droneIcon)
            .draggable(false)


        this.runOnUiThread {
            droneMarker?.remove()
            if(checkGpsCoordination(latitude, longitude)){
            droneMarker = mMap.addMarker(markerOptions)
            }
        }
    }

    private fun checkGpsCoordination(
        latitude: Double,
        longitude: Double
    ): Boolean { // this will check if your gps coordinates are valid
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    private fun cameraUpdate(latitude: Double, longitude: Double, zoom : Boolean = false) {
        val pos = LatLng(latitude, longitude)
        if(!zoom) {
            val cu = CameraUpdateFactory.newLatLng(pos)
            mMap.animateCamera(cu)
        }
        else {
            val zoomLevel = 18.0.toFloat()
            val cu = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
            mMap.animateCamera(cu)
        }

    }


    private fun monitorDroneAltitude() {
        droneAltitudeInfoRef = drone?.getInstrument(Altimeter::class.java) { altimeter ->
            altimeter?.groundRelativeAltitude.let { altitude ->
                ("alt: " + altitude?.value.toString()).also { altitudeTxt.text = it }
                if (altitude != null) {
                    positionJSON.put("altitude", altitude.value)
                }
            }
        }
    }

    private fun manageAutoPilotingItfState(itf: FlightPlanPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                Log.d(TAG, "the state is unavailable")
            }

            Activable.State.IDLE -> {
//                val status = itf.activate(true)
//                Log.d(TAG, "activation status: $status - state is idle")
            }

            Activable.State.ACTIVE -> {
                Log.d(TAG, "state is active")
            }

        }
    }


    /**
     * Manage piloting interface state.
     *
     * @param itf the piloting interface
     */
    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                // Piloting interface is unavailable.
                takeOffLandBt.isEnabled = false
            }

            Activable.State.IDLE -> {
                // Piloting interface is idle.
                takeOffLandBt.isEnabled = false

                // Activate the interface.
                itf.activate()
            }

            Activable.State.ACTIVE -> {
                // Piloting interface is active.

                when {
                    itf.canTakeOff() -> {
                        // Drone can take off.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = "Take Off"

                    }

                    itf.canLand() -> {
                        // Drone can land.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = "Land"
                    }

                    else -> // Disable the button.
                        takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    /**
     * Resets drone user interface part.
     */
    private fun resetDroneUi() {
        // Reset drone user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = ""
        takeOffLandBt.isEnabled = false

    }

    /**
     * Called on take off/land button click.
     */
    private fun onTakeOffLandClick() {

        // Get the piloting interface from its reference.
        pilotingItfRef?.get()?.let { itf ->
            // Do the action according to the interface capabilities
            if (itf.canTakeOff()) {
                // Take off
                itf.takeOff()
            } else if (itf.canLand()) {
                // Land
                itf.land()
            }
        }
    }

    /**
     * Resets remote user interface part.
     */
    private fun resetRcUi() {
        // Reset remote control user interface views.
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcBatteryTxt.text = ""
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor remote state
        monitorRcState()

        // Monitor remote battery charge level
        monitorRcBatteryChargeLevel()
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.

        rcStateRef?.close()
        rcStateRef = null

        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    /**
     * Monitor current remote control state.
     */
    private fun monitorRcState() {
        // Monitor current drone state.
        rcStateRef = rc?.getState {
            // Called at each remote state update.

            it?.let {
                // Update remote connection state view.
                rcStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Monitors current remote control battery charge level.
     */
    private fun monitorRcBatteryChargeLevel() {
        // Monitor the battery info instrument.
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                rcBatteryTxt.text = "${it.charge} ?"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onMapClick(p0: LatLng) {
        val marker = mMap.addMarker(
            MarkerOptions()
                .title((markers.lastIndex + 2).toString())
                .position(p0)
                .draggable(true)
                //.anchor(0.5F, 0.5F)
            //.icon(bicycleIcon)
        )
        // Set place as the tag on the marker object so it can be referenced within
        // com.example.dronemr.MarkerInfoWindowAdapter
        if (marker != null) {
            marker.tag = "test"
            markers.add(marker)
            waypointList.add(p0)

        }

    }

    @SuppressLint("SuspiciousIndentation")
    override fun onMapLongClick(p0: LatLng) {

        val size = markers.lastIndex + 1
        markers.clear()
        mMap.clear()
        waypointList.clear()
        if (size == 1) {
            val myToast = Toast.makeText(this, "marker removed", Toast.LENGTH_SHORT)
            myToast.show()
        }
        if (size > 1) {
            val myToast = Toast.makeText(this, "markers removed", Toast.LENGTH_SHORT)
            myToast.show()
        }

    }

    override fun onMapReady(p0: GoogleMap) {

        print("getting ready")
        mMap = p0
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        //addClusteredMarkers(mMap)
        mMap.setOnMapClickListener(this)
        mMap.setOnMapLongClickListener(this)
        //mMap.setOnCameraIdleListener(this)

        // Turn on the My Location layer and the related control on the map.
        //updateLocationUI()

        // Get the current location of the device and set the position of the map.
        //getDeviceLocation()

    }

    /**
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    */


    override fun onCameraIdle() {
        TODO("Not yet implemented")
    }

    private fun sendLocationToServer(jsonMessage: String, url : String) {
        // Lancement d'une coroutine sur le Dispatcher par défaut (Main)

        GlobalScope.launch {
            request.sendLocation(jsonMessage, url)

        }

    }

    private fun startSending() {
        Thread {
            while(true) {
                try {
                    //sleep for 10 seconds
                    Thread.sleep(10000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                positionJSON.put("timestamp", System.currentTimeMillis())
                sendLocationToServer(positionJSON.toString(), serverUrl)
            }
        }
    }


    private fun generateMission() {
        /**
        Toast.makeText(this, "Generating mavlink mission file...", Toast.LENGTH_SHORT).show()

        val location = drone?.getInstrument(Gps::class.java)?.lastKnownLocation()

        missionList.add(
            0,
            ChangeSpeedCommand(ChangeSpeedCommand.SpeedType.GROUND_SPEED, mSpeed)
        )

        if (location != null) {
            when (mFinishedAction) {
                "gohome" -> {

                    missionList.add(
                        1,
                        TakeOffCommand()
                    )
                    waypointList.forEach { point ->
                        missionList.add(
                            NavigateToWaypointCommand(
                                point.latitude,
                                point.longitude,
                                mAltitude,
                                0.0,
                                Companion.DEFAULT_HOLD_TIME,
                                Companion.DEFAULT_ACCEPTANCE_RADIUS
                            )
                        )
                    }
                    missionList.add(ReturnToLaunchCommand())
                    missionList.add(LandCommand())
                }

                "autoland" -> {
                    missionList.add(
                        1,
                        TakeOffCommand()
                    )
                    waypointList.forEach { point ->
                        missionList.add(
                            NavigateToWaypointCommand(
                                point.latitude,
                                point.longitude,
                                mAltitude,
                                0.0,
                                Companion.DEFAULT_HOLD_TIME,
                                Companion.DEFAULT_ACCEPTANCE_RADIUS
                            )
                        )
                    }
                    missionList.add(LandCommand())
                }

                "none" -> {
                    missionList.add(
                        1,
                        TakeOffCommand()
                    )
                    waypointList.forEach { point ->
                        missionList.add(
                            NavigateToWaypointCommand(
                                point.latitude,
                                point.longitude,
                                mAltitude,
                                0.0,
                                Companion.DEFAULT_HOLD_TIME,
                                Companion.DEFAULT_ACCEPTANCE_RADIUS
                            )
                        )
                    }
                }

                "firstwaypoint" -> {
                    missionList.add(
                        1,
                        TakeOffCommand()
                    )
                    waypointList.forEach { point ->
                        missionList.add(
                            NavigateToWaypointCommand(
                                point.latitude,
                                point.longitude,
                                mAltitude,
                                0.0,
                                Companion.DEFAULT_HOLD_TIME,
                                Companion.DEFAULT_ACCEPTANCE_RADIUS
                            )
                        )
                    }
                    missionList.add(missionList[2])
                    missionList.add(LandCommand())
                }
            }
        }
        val folder = getExternalFilesDir("flight_plan")
        mavlinkFile = File(folder, "flight_plan.txt")

        MavlinkFiles.generate(
            mavlinkFile,
            missionList,
        )
        start.isEnabled = true
        stop.isEnabled = true
        */

    }

    private fun startMission() {
        val missionControl = drone?.getPilotingItf(FlightPlanPilotingItf::class.java)
        if (missionControl != null) {
            missionControl.clearRecoveryInfo()
            Log.d(TAG, mavlinkFile.absolutePath)
            missionControl.uploadFlightPlan(mavlinkFile)
            missionControl.returnHomeOnDisconnect.isEnabled = true
            Log.d(TAG, "latest mission: " + missionControl.latestMissionItemExecuted.toString())
            Log.d(
                TAG,
                "it.returnHomeOnDisconnect: " + missionControl.returnHomeOnDisconnect.isEnabled.toString()
            )
            if (missionControl.state == Activable.State.IDLE) {
                val missionStarted = missionControl.activate(true)
                if (missionStarted) {
                    Toast.makeText(this, "mission started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "mission couldn't be started", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopMission() {
        val missionControl = drone?.getPilotingItf(FlightPlanPilotingItf::class.java)
        val isStopped = missionControl?.stop()
        if (isStopped == true) {
            Toast.makeText(this, "mission has been stopped", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "mission couldn't be stopped", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "mission is stopped = $isStopped")
    }

    private fun showSettingsDialog() {

        val wayPointSettings = layoutInflater.inflate(R.layout.dialog_waypointsetting, null) as LinearLayout

        val altitudeEditText = wayPointSettings.findViewById<View>(R.id.altitude) as EditText
        altitudeEditText.setText(mAltitude.toString())

        val speedEditText = wayPointSettings.findViewById<View>(R.id.speed) as EditText
        speedEditText.setText(mSpeed.toString())


        val actionAfterFinishedRG = wayPointSettings.findViewById<View>(R.id.actionAfterFinished) as RadioGroup
        actionAfterFinishedRG.setOnCheckedChangeListener { _, checkedId -> // set the action after finishing the mission
            Log.d(TAG, "Select finish action")

            when (checkedId) {
                R.id.finishNone -> {
                    mFinishedAction = "none"
                }
                R.id.finishGoHome -> {
                    mFinishedAction = "gohome"
                }
                R.id.finishAutoLanding -> {
                    mFinishedAction = "autoland"
                }
                R.id.finishToFirst -> {
                    mFinishedAction = "firstwaypoint"
                }
            }
        }


        val nameEditText = wayPointSettings.findViewById<View>(R.id.team) as EditText
        nameEditText.setText(team)

        val authEditText = wayPointSettings.findViewById<View>(R.id.auth) as EditText
        authEditText.setText(auth)

        val sourceEditText = wayPointSettings.findViewById<View>(R.id.source) as EditText
        sourceEditText.setText(source)

        val serverUrlEditText = wayPointSettings.findViewById<View>(R.id.serverUrl) as EditText
        serverUrlEditText.setText(serverUrl)

        AlertDialog.Builder(this) // creates the dialog
            .setTitle("")
            .setView(wayPointSettings)
            .setPositiveButton("Finish") { _, _ ->
                mAltitude = altitudeEditText.text.toString().toDouble()
                mSpeed = speedEditText.text.toString().toDouble()
                team = nameEditText.text.toString()
                auth = authEditText.text.toString()
                source = sourceEditText.text.toString()
                serverUrl = serverUrlEditText.text.toString()
                Log.e(TAG, "altitude $mAltitude")
                Log.e(TAG, "speed $mSpeed")
                Log.e(TAG, "mFinishedAction $mFinishedAction")
                positionJSON.put("team", team)
                positionJSON.put("auth",auth)
                positionJSON.put("source", source)
                positionJSON.put("position", position)
                generate.isEnabled = true
                Toast.makeText(this, "Finished configuring mission settings", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }




    override fun onClick(p0: View?) {
        when (p0?.id) {
            /**R.id.locate -> { // will draw the drone and move camera to the position of the drone on the map
                val location =  drone?.getInstrument(Gps::class.java)?.lastKnownLocation()
                latitudeTxt.text = location?.latitude.toString()
                longitudeTxt.text = location?.longitude.toString()
                Log.d(TAG, "Location on Btn Click: ${location?.latitude}, ${location?.longitude}")
                if (location != null) {
                    updateDroneLocation(location.latitude, location.longitude)
                    cameraUpdate(location.latitude, location.longitude)
                }
            }
            R.id.add -> { // this will toggle the adding of the waypoints
                enableDisableAdd()
            }
            R.id.clear -> { // clear the waypoints on the map
                runOnUiThread {
                    mMap.clear()
                    clearMission()
                }
            } */

            R.id.config -> { // this will show the settings
                showSettingsDialog()
            }

            R.id.generate -> { // this will upload the mission to the drone so that it can execute it
                generateMission()
            }
            R.id.start -> { // this will let the drone start navigating to the waypoints
                startMission()
            }
            R.id.stop -> { // this will immediately stop the waypoint mission
                stopMission()
            } else -> {}
        }
    }

}





