package com.example.myapplication.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.myapplication.R
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreen : AppCompatActivity() {
    companion object{
        private  val LOGIN_REQUEST_CODE =7171
    }

    private  lateinit var  providers:List<AuthUI.IdpConfig>
    private  lateinit var  firebaseAuth:FirebaseAuth
    private  lateinit var  listener: FirebaseAuth.AuthStateListener
    private lateinit var  database:FirebaseDatabase
    private  lateinit var driverInfoRef: DatabaseReference
    private   var url:String= "https://uberclone-1acd9-default-rtdb.firebaseio.com/"

    override fun onStart() {
        super.onStart()
        delaySpashScreen();
    }

    override fun onStop() {
        if (firebaseAuth!=null && listener !=null)firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySpashScreen() {
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
              firebaseAuth.addAuthStateListener(listener);
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
            init();
    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(CommonData.DRIVER_INFO_REFERNCE)
        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth->
            val user = myFirebaseAuth.currentUser
            if (user!=null){
               checkUserFromFirebase();
            }else{
                showLoginLayout();
            }
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreen,p0.message,Toast.LENGTH_SHORT).show();
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()){
//                        Toast.makeText(this@SplashScreen,"User Already Register",Toast.LENGTH_SHORT).show()
                        val model = p0.getValue(DriverInfoModel::class.java)
                        goHomeActivity(model)
                    }else{
                        showRegisterLayout();
                    }

                }

            })
    }

    private fun goHomeActivity(model: DriverInfoModel?) {
        CommonData.currertUser = model
        startActivity(Intent(this,HomeDriverActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this,R.style.DialogTheme);
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_ragister,null);

        val first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val last_name= itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val phone_no= itemView.findViewById<View>(R.id.edit_phone_number) as TextInputEditText

        val btn_continue= itemView.findViewById<View>(R.id.btn_register) as Button

        //set Data
        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber ))
                phone_no.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        //View
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //Event
        btn_continue.setOnClickListener {
            if (TextUtils.isDigitsOnly(first_name.text.toString())){
                Toast.makeText(this@SplashScreen,"Please enter First Name",Toast.LENGTH_SHORT).show();
                return@setOnClickListener
            }
          else  if (TextUtils.isDigitsOnly(phone_no.text.toString())){
                Toast.makeText(this@SplashScreen,"Please enter Phone No",Toast.LENGTH_SHORT).show();
                return@setOnClickListener
            }

          else  if (TextUtils.isDigitsOnly(last_name.text.toString())){
                Toast.makeText(this@SplashScreen,"Please enter Last Name",Toast.LENGTH_SHORT).show();
                return@setOnClickListener
            }
            else{
                val model = DriverInfoModel()
                model.firstName =first_name.text.toString()
                model.lastName = last_name.text.toString()
                model.phoneNo = phone_no.text.toString()
                model.rating=0.0
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener { e->
                        Toast.makeText(this@SplashScreen,""+e.message,Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@SplashScreen,"Register Successfull",Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        goHomeActivity(model)

                    }

            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_login)
            .setPhoneButtonId(R.id.btn_phn_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build();

        startActivity(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(this@SplashScreen,""+response!!.error!!.message,Toast.LENGTH_SHORT).show()
            }
        }
    }

}