/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.murati.audiobook.ui;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.MusicService;
import com.murati.audiobook.OfflineBookService;
import com.murati.audiobook.R;
import com.murati.audiobook.utils.FavoritesHelper;
import com.murati.audiobook.utils.FeedbackHelper;
import com.murati.audiobook.utils.LanguageHelper;
import com.murati.audiobook.utils.LogHelper;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.murati.audiobook.utils.MediaIDHelper;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 *
 * The requirements for a subclass is to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link androidx.appcompat.widget.Toolbar} with id 'toolbar',
 * a {@link DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(ActionBarCastActivity.class);

    private static final int DELAY_MILLIS = 1000;

    private CastContext mCastContext;
    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private boolean mToolbarInitialized;

    private int mItemToOpenWhenDrawerCloses = -1;

    private CastStateListener mCastStateListener = new CastStateListener() {
        @Override
        public void onCastStateChanged(int newState) {
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mMediaRouteMenuItem.isVisible()) {
                            LogHelper.d(TAG, "Cast Icon is visible");
                            showFtu();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }
    };

    private final DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerClosed(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerClosed(drawerView);
            if (mItemToOpenWhenDrawerCloses >= 0) {
                Bundle bundle = ActivityOptions.makeCustomAnimation(
                    ActionBarCastActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();

                Intent intent = null;
                switch (mItemToOpenWhenDrawerCloses) {
                    case R.id.navigation_allmusic:
                        intent = new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, MediaIDHelper.MEDIA_ID_ROOT);
                        break;
                    case R.id.navigation_playlists:
                        intent = new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, MediaIDHelper.MEDIA_ID_BY_QUEUE);
                        break;

                    case R.id.navigation_favorites:
                        intent = new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, MediaIDHelper.MEDIA_ID_BY_FAVORITES);
                        break;

                    case R.id.navigation_downloads:

                        if (!OfflineBookService.isPermissionGranted(ActionBarCastActivity.this)) {
                            Toast.makeText(getBaseContext(), R.string.notification_storage_permission_required, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        intent = new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, MediaIDHelper.MEDIA_ID_BY_DOWNLOADS);
                        break;

                    case R.id.navigation_settings:
                        intent = new Intent(ActionBarCastActivity.this, SettingsActivity.class);
                        break;

                    case R.id.navigation_feedback:
                        FeedbackHelper.showLikeDialog(ActionBarCastActivity.this);
                        break;
                    case R.id.navigation_about:
                        intent = new Intent(ActionBarCastActivity.this, AboutActivity.class);
                        break;
                    case R.id.navigation_quit:
                        //TODO: review notification bar cleanup

                        stopService(new Intent(ActionBarCastActivity.this, MusicService.class));
                        finish();

                        System.exit(0);
                        break;
                }
                if (intent != null) {
                    startActivity(intent, bundle);
                    finish();
                }
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerStateChanged(newState);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerOpened(drawerView);
            if (getSupportActionBar() != null) getSupportActionBar()
                    .setTitle(R.string.APP_NAME);
        }
    };

    private final FragmentManager.OnBackStackChangedListener mBackStackChangedListener =
        new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateDrawerToggle();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        int playServicesAvailable =
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (playServicesAvailable == ConnectionResult.SUCCESS) {
            mCastContext = CastContext.getSharedInstance(this);
        }

        LanguageHelper.enforceHungarianIfNeeded(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                "the end of your onCreate method");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCastContext != null) {
            mCastContext.addCastStateListener(mCastStateListener);
        }

        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);

        //TODO: make a language selector
        LanguageHelper.enforceHungarianIfNeeded(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCastContext != null) {
            mCastContext.removeCastStateListener(mCastStateListener);
        }
        getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        // Cast Menuitem
        if (mCastContext != null) {
            mMediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                    menu, R.id.media_route_menu_item);
        }
        return true;
    }

    private static final String FRAGMENT_TAG = "uamp_list_container";
    public String getMediaId() {
        //TODO: cast by main activity
        MediaBrowserFragment fragment = (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private void deleteEbook() {
        if (!OfflineBookService.isPermissionGranted(this)) {
            Toast.makeText(getBaseContext(), R.string.notification_storage_permission_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getBaseContext(), R.string.action_delete, Toast.LENGTH_SHORT).show();

        //TODO: async delete
        OfflineBookService.removeOfflineBook(getMediaId());

        //TODO: refresh control state
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //Try to get mediaId
        String mediaId = null;
        try {
            if (this.getClass() == MusicPlayerActivity.class) {
                MusicPlayerActivity musicPlayerActivity = (MusicPlayerActivity) this;
                mediaId = musicPlayerActivity.getMediaId();
            }
            else if (this.getClass() == FullScreenPlayerActivity.class) {
                FullScreenPlayerActivity fullPlayer = (FullScreenPlayerActivity) this;
                mediaId = fullPlayer.getMediaId();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Unable to fetch mediaId");
        }


        //Favorites Toggle
        if (item != null && mediaId != null && item.getItemId() == R.id.option_favorite ) {
            item.setIcon(
                FavoritesHelper.toggleFavoriteWithText(mediaId,ActionBarCastActivity.this));
            return true;
        }

        //Download button
        //TODO: if not downloaded yet
        if (item != null && mediaId != null && item.getItemId() == R.id.option_download) {
            return OfflineBookService.downloadWithActivity(mediaId,ActionBarCastActivity.this);
        }

        //Delete button
        if (item != null && mediaId != null && item.getItemId() == R.id.option_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_delete_question)
                .setTitle(R.string.action_delete)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm_delete,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActionBarCastActivity.this.deleteEbook();
                        }
                    }
                )
                .setNegativeButton(R.string.confirm_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }

        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        String mediaId = null;
        if (this.getClass() == MusicPlayerActivity.class) {
            MusicPlayerActivity musicPlayerActivity = (MusicPlayerActivity)this;
            mediaId = musicPlayerActivity.getMediaId();
            if (mediaId != null && mediaId.startsWith(MediaIDHelper.MEDIA_ID_BY_SEARCH)) {
                Intent home = new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class);
                home.setAction(Intent.ACTION_VIEW);
                home.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, MediaIDHelper.MEDIA_ID_ROOT);
                startActivity(home);
                return;
            }
        }

        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                "'toolbar'");
        }
        mToolbar.inflateMenu(R.menu.main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView " +
                        "with id 'nav_view'");
            }

            // Create an ActionBarDrawerToggle that will handle opening/closing of the drawer:
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
            mDrawerLayout.setDrawerListener(mDrawerListener);
            populateDrawerItems(navigationView);
            setSupportActionBar(mToolbar);
            updateDrawerToggle();
        } else {
            setSupportActionBar(mToolbar);
        }

        mToolbarInitialized = true;
    }

    private void populateDrawerItems(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mItemToOpenWhenDrawerCloses = menuItem.getItemId();
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
        if (MusicPlayerActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_allmusic);
        } else if (SettingsActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_playlists);
        }
    }

    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }
        boolean isRoot = getFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private void showFtu() {
        Menu menu = mToolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view != null && view instanceof MediaRouteButton) {
            IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(this, mMediaRouteMenuItem)
                    .setTitleText(R.string.touch_to_cast)
                    .setSingleTime()
                    .build();
            overlay.show();
        }
    }
}
