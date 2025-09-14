package com.example.attendancesystem.view.activities

import android.graphics.Color
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.ActivityMainBinding
import com.example.attendancesystem.utils.*
import com.example.attendancesystem.utils.Constants.Preference.IS_FROM
import com.example.attendancesystem.view.fragments.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    // VARIABLE LIST
    private var isExit = false
    private var currentFragment: Fragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        
        init()
        onClicks()
    }
    
    private fun init() {
        binding.root.applySystemBarsPadding(binding.root, binding.toolbar)
        
        
        window.statusBarColor = Color.TRANSPARENT
        
        prefManager().setStringPreference(IS_FROM, getString(R.string.overview))
        
        binding.bottomNavigation.apply {
            when (prefManager().getStringPreference(IS_FROM)) {
                getString(R.string.overview) -> {
                    setText(binding.tvScreenName, "OverView")
                    setToolBarAndBottomNavigationBarName(getString(R.string.overview))
                    navigateToFragment(OverViewFragment(), llOverView, ivOverView)
                }
                
                getString(R.string.attendance) -> {
                    setText(binding.tvScreenName, "Attendance")
                    setToolBarAndBottomNavigationBarName(getString(R.string.attendance))
                    navigateToFragment(AttendanceFragment(), llAttendance, ivAttendance)
                }
                
                else -> {
                    setToolBarAndBottomNavigationBarName(getString(R.string.overview))
                    setText(binding.tvScreenName, "OverView")
                    navigateToFragment(OverViewFragment(), llOverView, ivOverView)
                }
            }
        }
    }
    
    private fun onClicks() {
        binding.apply {
            bottomNavigation.apply {
                llOverView.setOnClickListener {
                    setText(binding.tvScreenName, "OverView")
                    setToolBarAndBottomNavigationBarName(getString(R.string.overview))
                    navigateToFragment(OverViewFragment(), llOverView, ivOverView)
                }
                
                llAttendance.setOnClickListener {
                    setText(binding.tvScreenName, "OverView")
                    setToolBarAndBottomNavigationBarName(getString(R.string.attendance))
                    navigateToFragment(AttendanceFragment(), llAttendance, ivAttendance)
                }
            }
            
            addOnBackPressedDispatcher {
                manageBackPress()
            }
        }
    }
    
    private fun manageBackPress() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
            
            Handler(Looper.getMainLooper()).postDelayed({
                updateBottomNavigationSelection()
            }, 100)
            
        } else {
            finishAndRemoveTask()
        }
    }
    
    private fun updateBottomNavigationSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        
        when (currentFragment) {
            is OverViewFragment -> {
                setToolBarAndBottomNavigationBarName(getString(R.string.overview))
                navigateToFragmentWithoutAddingToBackStack(binding.bottomNavigation.llOverView, binding.bottomNavigation.ivOverView)
            }
            
            is AttendanceFragment -> {
                setToolBarAndBottomNavigationBarName(getString(R.string.attendance))
                navigateToFragmentWithoutAddingToBackStack(binding.bottomNavigation.llAttendance, binding.bottomNavigation.ivAttendance)
            }
        }
    }
    
    private fun navigateToFragmentWithoutAddingToBackStack(linearLayout: View, imageView: View) {
        binding.bottomNavigation.apply {
            llOverView.isSelected = false
            ivOverView.isSelected = false
            llAttendance.isSelected = false
            ivAttendance.isSelected = false
            
            linearLayout.isSelected = true
            imageView.isSelected = true
        }
    }
    
    private fun askForPressBackButtonTwiceForExitApp() {
        if (isExit) {
            finishAndRemoveTask()
            return
        }
        toast("Press One more time to exit")
        isExit = true
        Handler(Looper.getMainLooper()).postDelayed({ isExit = false }, 2000)
    }
    
    private fun navigateToFragment(fragment: Fragment, linearLayout: View, imageView: View) {
        if (currentFragment != null && currentFragment!!::class.java == fragment::class.java) {
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView, fragment)
        transaction.addToBackStack(fragment::class.java.simpleName)
        transaction.commit()
        
        currentFragment = fragment
        
        binding.bottomNavigation.apply {
            llOverView.isSelected = false
            ivOverView.isSelected = false
            llAttendance.isSelected = false
            ivAttendance.isSelected = false
            
            linearLayout.isSelected = true
            imageView.isSelected = true
        }
    }
    
    private fun setToolBarAndBottomNavigationBarName(name: String) {
        binding.apply {
            toolbar.apply {
                tvScreenName.text = name.trim()
            }
        }
        
        binding.bottomNavigation.apply {
            viewGone(tvOverView)
            viewGone(tvAttendance)
            
            when (name) {
                getString(R.string.overview) -> {
                    viewVisible(tvOverView)
                }
                
                getString(R.string.attendance) -> {
                    viewVisible(tvAttendance)
                }
                
                else -> {
                    viewVisible(tvOverView)
                }
            }
        }
    }
    
}