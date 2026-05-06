package com.hostelops

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.hostelops.databinding.ActivityMainBinding
import com.hostelops.utils.CloudinaryHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CloudinaryHelper.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.roleSelectionFragment, R.id.loginFragment, R.id.signupFragment -> {
                    binding.appBarLayout.visibility = View.GONE
                }
                else -> {
                    binding.appBarLayout.visibility = View.VISIBLE
                    updateProfileIcon()
                }
            }
            binding.toolbarTitle.text = destination.label
        }

        binding.ivUserProfile.setOnClickListener {
            if (navController.currentDestination?.id != R.id.profileFragment) {
                navController.navigate(R.id.profileFragment)
            }
        }
    }

    private fun updateProfileIcon() {
        val user = auth.currentUser
        if (user != null) {
            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                binding.ivUserProfile.load(photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_placeholder)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivUserProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }
}
