package com.example.uvce_faculty

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class GetIn : AppCompatActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_in)

        val btnLogin: MaterialButton = findViewById(R.id.btnLogin)
        val btnSignUp: MaterialButton = findViewById(R.id.btnSignUp)

        // Listen for back stack changes
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showRoleSelectionUI()
            }
        }

        btnLogin.setOnClickListener {
            hideRoleSelectionUI()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.transition.slide_in_up,
                    R.transition.fade_out,
                    R.transition.fade_in,
                    R.transition.slide_out_down
                )
                .replace(R.id.login_fragment_container, LogIn())
                .addToBackStack(null)
                .commit()
        }

        btnSignUp.setOnClickListener {
            hideRoleSelectionUI()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.transition.slide_in_up,
                    R.transition.fade_out,
                    R.transition.fade_in,
                    R.transition.slide_out_down
                )
                .replace(R.id.login_fragment_container, SignUp())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun hideRoleSelectionUI() {
        val idsToHide = listOf(
            R.id.appSubtitle, R.id.cardContainer,
            R.id.bottomLinks
        )
        idsToHide.forEach { id -> findViewById<View>(id).visibility = View.GONE }
        findViewById<FrameLayout>(R.id.login_fragment_container).visibility = View.VISIBLE
    }

    fun showRoleSelectionUI() {
        val loginContainer = findViewById<FrameLayout>(R.id.login_fragment_container)

        if (loginContainer.visibility == View.VISIBLE) {
            loginContainer.animate()
                .translationY(loginContainer.height.toFloat())
                .setDuration(700)
                .withEndAction {
                    loginContainer.visibility = View.GONE
                    loginContainer.translationY = 0f
                }.start()
        }

        val idsToShow = listOf( R.id.appSubtitle, R.id.cardContainer, R.id.bottomLinks)
        idsToShow.forEach { id ->
            val view = findViewById<View>(id)
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(300).start()
            } else {
                view.alpha = 1f
            }
        }
    }




}

