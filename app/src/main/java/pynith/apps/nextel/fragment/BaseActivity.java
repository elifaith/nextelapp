package pynith.apps.nextel.fragment;

import android.app.Activity;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pynith.apps.nextel.BuildConfig;
import pynith.apps.nextel.helper.RatingHelper;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import pynith.apps.nextel.games.AppGameActivity;
import pynith.apps.nextel.helper.SessionService;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Arrays;

import pynith.apps.template.SlidingRootNav;
import pynith.apps.template.SlidingRootNavBuilder;
import pynith.apps.nextel.App;
import pynith.apps.nextel.R;
import pynith.apps.nextel.helper.menu.DrawerAdapter;
import pynith.apps.nextel.helper.menu.DrawerItem;
import pynith.apps.nextel.helper.menu.SimpleItem;
import pynith.apps.nextel.helper.menu.SpaceItem;
import pynith.apps.nextel.model.CONData;
import pynith.apps.nextel.model.User;
import pynith.apps.nextel.views.main.HomeActivity;
import pynith.apps.nextel.views.settings.AppSettingsActivity;
import pynith.apps.nextel.views.us.AboutActivity;
import pynith.apps.nextel.views.us.ActivityFAQs;
import pynith.apps.nextel.views.us.PrivacyActivity;
import pynith.apps.nextel.views.us.SupportActivity;
import pynith.apps.nextel.views.us.TermsActivity;


public abstract class BaseActivity extends pynith.apps.nextel.views.BaseActivity implements DrawerAdapter.OnItemSelectedListener {
    private static final int POS_DASHBOARD = 0;
    private static final int POS_FEATURE = 1;
    private static final int POS_PROFILE = 2;
    private static final int POS_SUPPORT = 3;
    private static final int POS_GAMES = 4;
    private static final int POS_ABOUT = 5;
    private static final int POS_LOGOUT = 7;

    private BottomSheetDialog mBottomMoreDialog;
    public static SharedPreferences userInfo;
    User user;
    View moreView;

    private String[] screenTitles;
    private Drawable[] screenIcons;
    private Handler mHandler;
    public DrawerAdapter adapter;

    public SlidingRootNav slidingRootNav;
    private boolean isTrue = true;
    private boolean isFalse = false;
    public static Activity mCActivity;
    Fragment cFragment;
    public static boolean mIsHasNavigationView = false;
    private int selected_id = 0;
    ShapeableImageView mImgProfile;
    TextView CurrentUserName, currentEmail;
    private CheckBox navNightMode;

    public abstract int getLayoutId();

    public abstract void initViews();

    public abstract boolean hasNavigation();

    public abstract void initFunction();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getLayoutId();
        setContentView(layoutId);
        mIsHasNavigationView = hasNavigation();
        mHandler = new Handler();

        mCActivity = this;

        userInfo = getSharedPreferences(CONData.APP_USER_EXT, Activity.MODE_PRIVATE);

        initToolBar();

        if (mIsHasNavigationView) {
            initDrawerLayout(savedInstanceState);

            String savedJson = userInfo.getString("user_data", null);
            user = new Gson().fromJson(savedJson, User.class);

            mImgProfile = findViewById(R.id.profile_image);
            CurrentUserName = findViewById(R.id.currentUserName);
            currentEmail = findViewById(R.id.currentEmail);
            navNightMode = findViewById(R.id.nav_night_mode);

            if (user != null ) {
                Glide.with(mImgProfile.getContext())
                        .load(user.getUserDP())
                        .placeholder(R.mipmap.ic_user)
                        .error(R.mipmap.ic_user)
                        .circleCrop()
                        .into(mImgProfile);

                CurrentUserName.setText("Hi " + user.getUname() );
                currentEmail.setText(user.getEmail());
            } else {
                CurrentUserName.setText(R.string.app_name);
                currentEmail.setVisibility(View.GONE);
            }

            applySavedTheme();

        }

        initViews();

        initFunction();

    }


    public void MenuClicked(int position, String tag) {
        slidingRootNav.closeMenu();
    }


    private void loadFragment(final Fragment fg, String tag) {

        cFragment = getSupportFragmentManager().findFragmentById(R.id.container);

        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                if(cFragment != null){
                    Toast.makeText(mCActivity, "Same Fragment loaded", Toast.LENGTH_LONG).show();
                }else{
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(R.anim.anim_pull_in_right,
                            R.anim.anim_push_out_right);
                    fragmentTransaction.replace(R.id.container, fg).addToBackStack(tag);
                    fragmentTransaction.commitAllowingStateLoss();
                }
            }
        };

        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }
    }


    private void initToolBar() {
    }

    @Override
    public void onBackPressed() {
        if (mIsHasNavigationView && slidingRootNav.isMenuOpened()) {
            slidingRootNav.closeMenu(true);
        } else {
            super.onBackPressed();
        }
    }

    private void initDrawerLayout(Bundle bundle)
    {
        slidingRootNav = new SlidingRootNavBuilder(mCActivity)
                .withMenuOpened(false)
                .withContentClickableWhenMenuOpened(false)
                .withSavedState(bundle)
                .withMenuLayout(R.layout.menu_left_drawer)
                .inject();

        screenIcons = loadScreenIcons();
        screenTitles = loadScreenTitles();

        adapter = new DrawerAdapter(Arrays.asList(
                createItemFor(POS_DASHBOARD).setChecked(true),
                createItemFor (POS_FEATURE),
                createItemFor (POS_PROFILE),
                createItemFor(POS_SUPPORT),
                createItemFor(POS_GAMES),
                createItemFor(POS_ABOUT),
                new SpaceItem(48),
                createItemFor(POS_LOGOUT)));
        adapter.setListener(this);

        RecyclerView list = findViewById(R.id.list);
        list.setNestedScrollingEnabled(true);
        list.setLayoutManager(new LinearLayoutManager(mCActivity));
        list.setAdapter(adapter);

        mBottomMoreDialog = new BottomSheetDialog(mCActivity);
        moreView = getLayoutInflater().inflate(R.layout.bottom_menu, null);
        mBottomMoreDialog.setContentView(moreView);
    }

    private DrawerItem createItemFor(int position) {
        return new SimpleItem(screenIcons[position], screenTitles[position])
                .withIconTint(color(R.color.cm_black))
                .withTextTint(color(R.color.cm_black))
                .withSelectedIconTint(color(R.color.colorRed))
                .withSelectedTextTint(color(R.color.colorRed));
    }

    private String[] loadScreenTitles() {
        return getResources().getStringArray(R.array.ld_activityScreenTitles);
    }

    private Drawable[] loadScreenIcons() {
        TypedArray ta = getResources().obtainTypedArray(R.array.ld_activityScreenIcons);
        Drawable[] icons = new Drawable[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            int id = ta.getResourceId(i, 0);
            if (id != 0) {
                icons[i] = ContextCompat.getDrawable(mCActivity, id);
            }
        }
        ta.recycle();
        return icons;
    }

    @ColorInt
    private int color(@ColorRes int res) {
        return ContextCompat.getColor(mCActivity, res);
    }

    public void onTermsClick(View view){
        slidingRootNav.closeMenu(isTrue);
        startActivity( new Intent(mCActivity, TermsActivity.class) );
        overridePendingTransition(R.anim.anim_bottom_in, R.anim.fade_out);
    }

    public void onPolicyClick(View view){
        slidingRootNav.closeMenu(isTrue);
        startActivity( new Intent(mCActivity, PrivacyActivity.class) );
        overridePendingTransition(R.anim.anim_bottom_in, R.anim.fade_out);
    }

    public void onFAQClick(View view){
        slidingRootNav.closeMenu(isTrue);
        startActivity( new Intent(mCActivity, ActivityFAQs.class) );
        overridePendingTransition(R.anim.anim_bottom_in, R.anim.fade_out);
    }

    public void onSettingsClick(View view){
        slidingRootNav.closeMenu(isTrue);
        startActivity( new Intent(mCActivity, AppSettingsActivity.class) );
        overridePendingTransition(R.anim.anim_bottom_in, R.anim.fade_out);
    }

    public void onShareApp(View view) {
        slidingRootNav.closeMenu(isTrue);
        Intent intent = new Intent(Intent.ACTION_SEND);
        var tex = "Hey! I'm using this awesome app. You should try it too!\nI Invest in Call Minutes And Earn Daily Returns Through Nextel Connect ever since, its a live changer.\n\n".concat(BuildConfig.FRONTBASE_URL);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app");
        intent.putExtra(Intent.EXTRA_TEXT, tex);
        startActivity(Intent.createChooser(intent, getTitle()));
    }

    public void onRateApp(View view) {
        slidingRootNav.closeMenu(isTrue);
        new RatingHelper(mCActivity).showRatingDialog();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(CONData.APP_PREFS_EXT, MODE_PRIVATE);
        String isDark = prefs.getString("theme", "Light");

        if ("Dark".equals(isDark)) {
            navNightMode.setChecked(true);
            navNightMode.setText("Dark Mode");
        } else {
            navNightMode.setChecked(false);
            navNightMode.setText("Light Mode");
        }
    }

    public void onAirdrop(View view) {
        slidingRootNav.closeMenu(isTrue);
        Toast.makeText(mCActivity, "Feature coming soon. Keep anticipating", Toast.LENGTH_LONG).show();
    }

    public void onProfileInfoClick(View view){
        slidingRootNav.closeMenu(isTrue);

        if ( new SessionService(mCActivity).isLoggedIn() ) {
            Intent profileIntent = HomeActivity.Companion.newInstance(mCActivity);
            profileIntent.putExtra("pynith.apps.nextel.extra.WEB_PATH", "/dashboard/profile");
            startActivity(profileIntent);
            overridePendingTransition(R.anim.anim_pull_in_right, R.anim.anim_push_out_right);
        }else{
            Toast.makeText(mCActivity, "Please login first", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemSelected(int position) {

        if (position == POS_DASHBOARD) {
            slidingRootNav.closeMenu(isTrue);
            if( selected_id != POS_DASHBOARD ) {
                startActivity(new Intent(mCActivity, HomeActivity.class));
                overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
            }
            selected_id = 0;
        }

        if (position == POS_FEATURE) {
            slidingRootNav.closeMenu(isTrue);
            if( selected_id != POS_FEATURE ) {
                Intent featuresIntent = HomeActivity.Companion.newInstance(mCActivity);
                featuresIntent.putExtra("pynith.apps.nextel.extra.WEB_PATH", "/dashboard/vas");
                startActivity(featuresIntent);
                overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
            }
            selected_id = 1;
        }

        if (position == POS_PROFILE) {
            slidingRootNav.closeMenu(isTrue);
            if( selected_id != POS_PROFILE ) {
                Intent profileIntent = HomeActivity.Companion.newInstance(mCActivity);
                profileIntent.putExtra("pynith.apps.nextel.extra.WEB_PATH", "/dashboard/profile");
                startActivity(profileIntent);
                overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
            }
            selected_id = 2;
        }

        if (position == POS_SUPPORT) {
            slidingRootNav.closeMenu(isTrue);
            startActivity( new Intent(mCActivity, SupportActivity.class) );
            overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
        }

        if (position == POS_GAMES) {
            slidingRootNav.closeMenu(isTrue);
            startActivity( new Intent(mCActivity, AppGameActivity.class) );
            overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
        }

        if (position == POS_ABOUT) {
            slidingRootNav.closeMenu(isTrue);
            startActivity( new Intent(mCActivity, AboutActivity.class) );
            overridePendingTransition(R.anim.anim_pull_in_right, R.anim.fade_out);
        }

        if (position == POS_LOGOUT) {
            slidingRootNav.closeMenu(isTrue);
            App.Companion.logout(mCActivity);
        }

    }


}
