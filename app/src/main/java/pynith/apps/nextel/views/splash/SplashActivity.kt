package pynith.apps.nextel.views.splash

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import pynith.apps.nextel.R
import pynith.apps.nextel.views.BaseActivity
import pynith.apps.nextel.views.auth.LoginActivity


class SplashActivity : BaseActivity() {
    var isShowingRubberEffect: Boolean = false

    private lateinit var activity: Activity

    private lateinit var mLogoOuterIv: ImageView
    private lateinit var mLogoInnerIv: ImageView
    private lateinit var mAppNameTv: TextView
    private lateinit var mLogoDivTv: FrameLayout
    private lateinit var intro: Animation
    private lateinit var slide: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        activity = this



        mLogoOuterIv = findViewById<View>(R.id.logo_outer_iv) as ImageView
        mLogoInnerIv = findViewById<View>(R.id.logo_inner_iv) as ImageView
        mAppNameTv = findViewById<View>(R.id.app_name_tv) as TextView
        mLogoDivTv = findViewById<View>(R.id.logo_div) as FrameLayout

        intro = AnimationUtils.loadAnimation(activity, R.anim.logo_intro)
        slide = AnimationUtils.loadAnimation(activity, R.anim.logo_slide_up)

        initAnimation()
    }

    private fun initAnimation() {
        startLogoInner1()
        startLogoOuterAndAppName()
    }

    private fun startLogoInner1() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.anim_top_in)
        mLogoInnerIv!!.startAnimation(animation)
    }

    private fun startLogoOuterAndAppName() {
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.setDuration(1000)
        valueAnimator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val fraction = animation.getAnimatedFraction()

                if (fraction >= 0.8 && !isShowingRubberEffect) {
                    isShowingRubberEffect = true
                    startLogoOuter()
                    startShowAppName()
                    finishActivity()
                }
                if (fraction >= 0.95) {
                    valueAnimator.cancel()
                    startLogoInner2()
                }
            }
        })
        valueAnimator.start()
    }

    private fun startLogoOuter() {
        YoYo.with(Techniques.RubberBand).duration(1000).playOn(mLogoOuterIv)
    }

    private fun startShowAppName() {
        YoYo.with(Techniques.FadeIn).duration(1000).playOn(mAppNameTv)
    }

    private fun startLogoInner2() {
        YoYo.with(Techniques.Bounce).duration(1000).playOn(mLogoInnerIv)
    }

    private fun finishActivity() {
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            mLogoDivTv.startAnimation(intro)
            intro.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    mLogoDivTv.startAnimation(slide)

                    slide.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            // LoginActivity validates any saved API token, then performs
                            // the secure cookie handoff before opening the WebView.
                            startActivity(Intent(activity, LoginActivity::class.java))
                            finish()
                        }

                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }

                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }, 1500)
    }
}
