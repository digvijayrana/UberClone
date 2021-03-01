package com.example.myapplication.activities.ui.home


import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.activities.CommonData
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private  lateinit var  mapFragment: SupportMapFragment

    //location
    private lateinit var  locationRequest:LocationRequest
    private  lateinit var  locationCallback: LocationCallback
    private  lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Online System

    private lateinit var  onlineref:DatabaseReference
    private  lateinit var  currentUser:DatabaseReference
    private  lateinit var  driverLocationRef:DatabaseReference
    private lateinit var   geofire: GeoFire

    private  val  onLinevalueListener = object :ValueEventListener{
        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
        }

        override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                    currentUser.onDisconnect().removeValue()
        }

    }


    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
       geofire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineref.removeEventListener(onLinevalueListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineref.addValueEventListener(onLinevalueListener)
    }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ):
            View? { homeViewModel =ViewModelProvider(this).get(HomeViewModel::class.java)
                val root = inflater.inflate(R.layout.fragment_home, container, false)
                 init()
                val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                 mapFragment.getMapAsync(this)
        return root
    }

    @SuppressLint("UseRequireInsteadOfGet", "MissingPermission")
    private fun init() {
        onlineref = FirebaseDatabase.getInstance().getReference().child("info/connected")
        driverLocationRef= FirebaseDatabase.getInstance().getReference(CommonData.DRIVER_LOCATION_REFRENCE)
        currentUser = FirebaseDatabase.getInstance().getReference(CommonData.DRIVER_LOCATION_REFRENCE).child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        geofire  = GeoFire(driverLocationRef)
        registerOnlineSystem()

        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setFastestInterval(3000)
        locationRequest.interval =5000
        locationRequest.setSmallestDisplacement(10f)

        locationCallback = object :LocationCallback(){
            override fun onLocationResult(locationResult:LocationResult?) {
                super.onLocationResult(locationResult)
                val newPos = LatLng(locationResult!!.lastLocation.latitude,locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                //update location
                geofire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude)
                ){key:String?,error:DatabaseError?->
                    if (error!=null)
                        Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
                    else
                        Snackbar.make(mapFragment.requireView(),"You Are Online",Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        fusedLocationProviderClient =LocationServices.getFusedLocationProviderClient(context!!)
      fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())

    }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!


        //Request Permission

//        Dexter.withContext(context!!).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            .withListener(object:MultiplePermissionsListener,PermissionListener{
//                @SuppressLint("MissingPermission")
//                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
//
//                    mMap.isMyLocationEnabled =true
//                    mMap.uiSettings.isMyLocationButtonEnabled =true
//                    mMap.setOnMyLocationClickListener {
//                        fusedLocationProviderClient.lastLocation
//                            .addOnFailureListener{e->
//                                Toast.makeText(context,"Permission"+e.message,Toast.LENGTH_SHORT).show()
//
//                            }.addOnSuccessListener { location ->
//                                val  userLating = LatLng(location.latitude,location.longitude)
//                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLating,18f))
//                            }
//                        true
//                    }
//
//                    val locationButton = (mapFragment.requireView()!!
//                        .findViewById<View>("1".toInt())!!
//                        .parent!! as View).findViewById<View>("2".toInt());
//
//                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
//                    params.addRule(RelativeLayout.ALIGN_TOP,0)
//                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
//                    params.bottomMargin = 50
//                }
//
//                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
//                    Toast.makeText(context,"Permission"+p0!!.permissionName+"was denied",Toast.LENGTH_SHORT).show()
//                }
//
//            }).check()

        mMap.uiSettings.isZoomControlsEnabled=true
        try{
                val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.uber_maps_style))
            if (!success){
                Log.e("Error","Style parsing error")
            }

        }catch (e:Resources.NotFoundException){
            Log.d("Error", e.message.toString())
        }

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}



