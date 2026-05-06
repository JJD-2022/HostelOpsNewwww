package com.hostelops

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.hostelops.databinding.ActivityMainBinding
import com.hostelops.utils.AvatarUtils
import com.hostelops.utils.CloudinaryHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var notificationListener: ListenerRegistration? = null
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        CloudinaryHelper.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.navigationView.setupWithNavController(navController)
        
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.logoutItem) {
                auth.signOut()
                navController.navigate(R.id.loginFragment)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                if (handled) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
                handled
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.roleSelectionFragment, R.id.loginFragment, R.id.signupFragment -> {
                    binding.appBarLayout.visibility = View.GONE
                    stopNotificationListener()
                }
                else -> {
                    binding.appBarLayout.visibility = View.VISIBLE
                    updateProfileIcon()
                    startNotificationListener()
                }
            }
            binding.toolbarTitle.text = destination.label
        }

        binding.ivUserProfile.setOnClickListener {
            if (navController.currentDestination?.id != R.id.profileFragment) {
                navController.navigate(R.id.profileFragment)
            }
        }
        
        createNotificationChannel()
    }

    private fun startNotificationListener() {
        val user = auth.currentUser ?: return
        if (notificationListener != null) return

        // First get the role if we don't have it
        if (userRole == null) {
            db.collection("users").document(user.uid).get().addOnSuccessListener {
                userRole = it.getString("role")
                updateNavigationMenu(userRole)
                setupListener(user.uid, userRole)
            }
        } else {
            updateNavigationMenu(userRole)
            setupListener(user.uid, userRole)
        }
    }

    private fun updateNavigationMenu(role: String?) {
        binding.navigationView.menu.clear()
        when (role) {
            "Student" -> binding.navigationView.inflateMenu(R.menu.menu_student_drawer)
            "Staff" -> binding.navigationView.inflateMenu(R.menu.menu_staff_drawer)
            "Admin" -> binding.navigationView.inflateMenu(R.menu.menu_admin_drawer)
        }
    }

    private fun setupListener(uid: String, role: String?) {
        val query = db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        notificationListener = query.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            
            for (dc in snapshots.documentChanges) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    val targetUid = dc.document.getString("targetUid")
                    val targetRole = dc.document.getString("targetRole")
                    val title = dc.document.getString("title") ?: "HostelOps"
                    val message = dc.document.getString("message") ?: ""
                    
                    // Priority Check (Optional: highlight urgent notifications)
                    val isUrgent = title.contains("Urgent", ignoreCase = true) || message.contains("Escalated", ignoreCase = true)
                    
                    // Trigger if it's for me
                    if (targetUid == uid || (targetRole != null && targetRole == role)) {
                        val timestamp = dc.document.getTimestamp("timestamp")
                        if (timestamp != null && (System.currentTimeMillis() - timestamp.toDate().time) < 15000) {
                            showLocalNotification(title, message, isUrgent)
                        }
                    }
                }
            }
        }
    }

    private fun showLocalNotification(title: String, message: String, isUrgent: Boolean) {
        val channelId = if (isUrgent) "hostelops_urgent" else "hostelops_local"
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(if (isUrgent) android.R.drawable.stat_sys_warning else R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Standard Channel
            val channel = NotificationChannel("hostelops_local", "Standard Updates", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
            
            // Urgent Channel
            val urgentChannel = NotificationChannel("hostelops_urgent", "Urgent Escalations", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                setVibrationPattern(longArrayOf(0, 500, 200, 500))
            }
            notificationManager.createNotificationChannel(urgentChannel)
        }
    }

    private fun stopNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
    }

    private fun updateProfileIcon() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get().addOnSuccessListener {
                val name = it.getString("name") ?: user.displayName ?: "User"
                val photoUrl = it.getString("photoUrl") ?: user.photoUrl?.toString()
                
                if (photoUrl != null) {
                    binding.ivUserProfile.load(photoUrl) {
                        crossfade(true)
                        placeholder(AvatarUtils.getLetterAvatar(this@MainActivity, name))
                        transformations(CircleCropTransformation())
                    }
                } else {
                    binding.ivUserProfile.setImageDrawable(AvatarUtils.getLetterAvatar(this, name))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationListener()
    }
}
