package com.example.dronemr

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
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
import com.example.dronemr.place.Place
import com.example.dronemr.place.PlaceRenderer
import com.example.dronemr.place.PlacesReader
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection


class MainActivity : AppCompatActivity(), GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback {


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

    /**list of places to display */
    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }

    /**list of markers added */
    private val markers: MutableList<Marker> = arrayListOf()


    /** Bicycle icon TO CHANGE*/
    private val bicycleIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.black)
        BitmapHelper.vectorToBitmap(this, R.drawable.ic_directions_bike_black_24dp, color)
    }

    private var circle: Circle? = null

    /**
     * Adds a [Circle] around the provided [item]
     */
    private fun addCircle(googleMap: GoogleMap, item: Place) {
        circle?.remove()
        circle = googleMap.addCircle(
            CircleOptions()
                .center(item.latLng)
                .radius(1000.0)
                .fillColor(ContextCompat.getColor(this, R.color.purple_500_Translucent))
                .strokeColor(ContextCompat.getColor(this, R.color.purple_500))
        )
    }




    /**
     * Adds marker representations of the places list on the provided GoogleMap object
     */
    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(place.latLng)
                    .anchor(0.5F, 0.5F)
                    .icon(bicycleIcon)
                    .draggable(true)
            )
            // Set place as the tag on the marker object so it can be referenced within
            // com.example.dronemr.MarkerInfoWindowAdapter
            if (marker != null) {
                marker.tag = place

            }
        }
    }

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
        clusterManager.addItems(places)
        clusterManager.cluster()

        clusterManager.setOnClusterItemClickListener { item ->
            addCircle(googleMap, item)
            return@setOnClusterItemClickListener false
        }


        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        googleMap.setOnCameraIdleListener {
            clusterManager.onCameraIdle()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        print("creating")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Map reference
        val mapFragment = supportFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment?
        mapFragment?.getMapAsync(this)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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

        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.


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
                        takeOffLandBt.text = "Take off"
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
                .anchor(0.5F, 0.5F)
                .icon(bicycleIcon)
                .draggable(true)
        )
        // Set place as the tag on the marker object so it can be referenced within
        // com.example.dronemr.MarkerInfoWindowAdapter
        if (marker != null) {
            marker.tag = "test"
            markers.add(marker)

        }

    }

    @SuppressLint("SuspiciousIndentation")
    override fun onMapLongClick(p0: LatLng) {

        val size = markers.lastIndex + 1
        markers.clear()
        mMap.clear()
        if(size == 1){
            val myToast = Toast.makeText(this, "marker removed", Toast.LENGTH_SHORT)
            myToast.show()}
        if(size > 1){
            val myToast = Toast.makeText(this, "markers removed", Toast.LENGTH_SHORT)
            myToast.show()}

    }

    override fun onMapReady(p0: GoogleMap) {
        print("getting ready")
        mMap = p0 ?: return
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL;
        //addClusteredMarkers(mMap)
        mMap.setOnMapClickListener(this)
        mMap.setOnMapLongClickListener(this)
        //mMap.setOnCameraIdleListener(this)

    }

    override fun onCameraIdle() {
        TODO("Not yet implemented")
        print("idk")
    }
}



