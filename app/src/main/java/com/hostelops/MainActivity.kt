package com.hostelops

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications permission denied. You won't receive updates.", Toast.LENGTH_SHORT).show()
        }
    }

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
        
        // Update last opened session
        getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
            .edit().putLong("last_opened", System.currentTimeMillis()).apply()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        userRole = intent.getStringExtra("ROLE")
        
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
                userRole = null
                navController.navigate(R.id.action_global_roleSelectionFragment)
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
        
        askNotificationPermission()
        createNotificationChannel()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
        when (role?.uppercase()) {
            "STUDENT" -> binding.navigationView.inflateMenu(R.menu.menu_student_drawer)
            "STAFF" -> binding.navigationView.inflateMenu(R.menu.menu_staff_drawer)
            "ADMIN" -> binding.navigationView.inflateMenu(R.menu.menu_admin_drawer)
        }
    }

    private fun setupListener(uid: String, role: String?) {
        val startTime = System.currentTimeMillis()
        // No orderBy to avoid index requirement, we'll filter in code
        val query = db.collection("notifications")

        notificationListener = query.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            
            for (dc in snapshots.documentChanges) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    val notif = dc.document.toObject(com.hostelops.models.Notification::class.java)
                    
                    // Only show if it's NEW (added after app session started)
                    if (notif.timestamp.toDate().time > (startTime - 5000)) {
                        if (notif.targetUid == uid || (notif.targetRole != null && notif.targetRole == role)) {
                            showLocalNotification(notif.title, notif.message, notif.title.contains("Urgent", true))
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
            val headerView = binding.navigationView.getHeaderView(0)
            val ivHeaderAvatar = headerView.findViewById<ShapeableImageView>(R.id.ivHeaderAvatar)
            val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
            val tvHeaderEmail = headerView.findViewById<TextView>(R.id.tvHeaderEmail)

            db.collection("users").document(user.uid).get().addOnSuccessListener {
                val name = it.getString("name") ?: user.displayName ?: "User"
                val email = it.getString("email") ?: user.email ?: ""
                val photoUrl = it.getString("photoUrl") ?: user.photoUrl?.toString()
                
                tvHeaderName.text = name
                tvHeaderEmail.text = email

                if (photoUrl != null) {
                    binding.ivUserProfile.load(photoUrl) {
                        crossfade(true)
                        placeholder(AvatarUtils.getLetterAvatar(this@MainActivity, name))
                        transformations(CircleCropTransformation())
                    }
                    ivHeaderAvatar.load(photoUrl) {
                        crossfade(true)
                        placeholder(AvatarUtils.getLetterAvatar(this@MainActivity, name))
                        transformations(CircleCropTransformation())
                    }
                } else {
                    binding.ivUserProfile.setImageDrawable(AvatarUtils.getLetterAvatar(this, name))
                    ivHeaderAvatar.setImageDrawable(AvatarUtils.getLetterAvatar(this, name))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationListener()
    }
}
