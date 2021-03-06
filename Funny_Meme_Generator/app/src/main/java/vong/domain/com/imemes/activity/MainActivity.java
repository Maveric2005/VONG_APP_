package vong.domain.com.imemes.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import com.onesignal.OneSignal;
import vong.domain.com.imemes.App;
import com.generator.maker.meme.BuildConfig;
import com.generator.maker.meme.R;
import vong.domain.com.imemes.data.MemeCategory;
import vong.domain.com.imemes.data.MemeLibConfig;
import vong.domain.com.imemes.data.MemeOriginAssets;
import vong.domain.com.imemes.data.MemeOriginFavorite;
import vong.domain.com.imemes.data.MemeOriginInterface;
import vong.domain.com.imemes.data.MemeOriginStorage;
import vong.domain.com.imemes.ui.GridDecoration;
import vong.domain.com.imemes.ui.GridRecycleAdapter;
import vong.domain.com.imemes.util.Helpers;
import com.startapp.android.publish.adsCommon.StartAppAd;
import com.startapp.android.publish.adsCommon.StartAppSDK;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener {

    public static final int REQUEST_LOAD_GALLERY_IMAGE = 50;
    public static final int REQUEST_TAKE_CAMERA_PICTURE = 51;
    public static final String IMAGE_PATH = "imagePath";
    private static boolean isShowingFullscreenImage = false;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.main__activity__navview)
    NavigationView navigationView;

    @BindView(R.id.main__tabs)
    TabLayout tabLayout;

    @BindView(R.id.main__activity__recycler_view)
    RecyclerView recyclerMemeList;

    App app;
    private MemeCategory mMemeCategory = null;
    private String cameraPictureFilepath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        // Call syncHashedEmail anywhere in your app if you have the user's email.
        // This improves the effectiveness of OneSignal's "best-time" notification scheduling feature.
        // OneSignal.syncHashedEmail(userEmail);

        setContentView(R.layout.main__activity);
        StartAppSDK.init(this, "206714554", false);


        // Bind UI
        app = (App) getApplication();
        ButterKnife.bind(this);

        // Setup toolbar
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.main__navdrawer__open, R.string.main__navdrawer__close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        tabLayout.setOnTabSelectedListener(this);

        // Setup Floating Action Button
        int gridColumns = Helpers.isInPortraitMode(this)
                ? app.settings.getGridColumnCountPortrait()
                : app.settings.getGridColumnCountLandscape();

        recyclerMemeList.setHasFixedSize(true);
        RecyclerView.LayoutManager recyclerGridLayout = new GridLayoutManager(this, gridColumns);
        recyclerMemeList.setLayoutManager(recyclerGridLayout);
        recyclerMemeList.addItemDecoration(new GridDecoration(10));

        mMemeCategory = app.getMemeCategory(MemeLibConfig.MEME_CATEGORIES.ALL[app.settings.getLastSelectedCategory()]);

        for (String cat : getResources().getStringArray(R.array.meme_categories)) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(cat);
            tabLayout.addTab(tab);
        }

        selectTab(app.settings.getLastSelectedCategory());

        //
        // Actions based on build type or version
        //
        navigationView.getMenu().findItem(R.id.action_donate_bitcoin).setVisible(!BuildConfig.IS_GPLAY_BUILD);



        if (BuildConfig.IS_TEST_BUILD) {
            ((ImageView) navigationView.getHeaderView(0).findViewById(R.id.main__activity__navheader__image)).setImageResource(R.drawable.ic_launcher_test);
        }


    }

    @SuppressWarnings("ConstantConditions")
    private void selectTab(int pos) {
        pos = pos >= 0 ? pos : tabLayout.getTabCount() - 1;
        pos = pos < tabLayout.getTabCount() ? pos : 0;
        tabLayout.getTabAt(pos).select();
    }

    @Override
    protected void onResume() {
        if (isShowingFullscreenImage) {
            isShowingFullscreenImage = false;
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        StartAppAd.onBackPressed(this);
        super.onBackPressed();
    }

    public void btnOpenActivity (View view){
        Intent nextActivity = new Intent(this, NextActivity.class);
        startActivity(nextActivity);
        StartAppAd.showAd(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean handleBarClick(MenuItem item) {
        MemeOriginInterface memeOriginObject = null;

        switch (item.getItemId()) {
            case R.id.action_settings: {
                Helpers.animateToActivity(this, SettingsActivity.class, SettingsActivity.ACTIVITY_ID, false);
                return true;
            }
            case R.id.action_exit: {
                finish();
                return true;
            }
            case R.id.action_recommend: {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.main__ready_to_memetastic, getString(R.string.page_additional_contact_text)));
                startActivity(Intent.createChooser(i, getString(R.string.main__share_meme)));
                return true;
            }
            case R.id.action_donate_bitcoin: {
                Helpers.openWebpageWithExternalBrowser(this, getString(R.string.page_additional_contact_text));
                return true;
            }
            case R.id.action_homepage_github: {
                Helpers.openWebpageWithExternalBrowser(this, getString(R.string.app_www_source));
                return true;
            }
            case R.id.action_picture_from_gallery: {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Helpers.animateToActivity(this, i, REQUEST_LOAD_GALLERY_IMAGE);
                return true;
            }
            case R.id.action_picture_from_camera: {
                showCameraDialog();
                return true;
            }

            case R.id.action_mode_create: {
                memeOriginObject = new MemeOriginAssets(mMemeCategory, getAssets());
                toolbar.setTitle(R.string.app_name);
                break;
            }
            case R.id.action_mode_favs: {
                memeOriginObject = new MemeOriginFavorite(app.settings.getFavoriteMemes(), getAssets());
                toolbar.setTitle(R.string.main__mode__favs);
                break;
            }
            case R.id.action_mode_saved: {
                File filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
                filePath.mkdirs();
                memeOriginObject = new MemeOriginStorage(filePath, getString(R.string.dot_thumbnails));
                toolbar.setTitle(R.string.main__mode__saved);
                break;
            }
        }

        // Change mode
        if (memeOriginObject != null) {
            tabLayout.setVisibility(item.getItemId() == R.id.action_mode_create ? View.VISIBLE : View.GONE);
            drawer.closeDrawers();
            GridRecycleAdapter recyclerMemeAdapter = new GridRecycleAdapter(memeOriginObject, this);
            recyclerMemeList.setAdapter(recyclerMemeAdapter);
            return true;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOAD_GALLERY_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();

                    // String picturePath contains the path of selected Image
                    onImageTemplateWasChosen(picturePath, false);
                }
            } else {
                Helpers.showSnackBar(this, R.string.main__error_no_picture_selected);
            }
        }

        if (requestCode == REQUEST_TAKE_CAMERA_PICTURE) {
            if (resultCode == RESULT_OK) {
                onImageTemplateWasChosen(cameraPictureFilepath, false);
            } else {
                Helpers.showSnackBar(this, R.string.main__error_no_picture_selected);
            }
        }
    }

    /**
     * Show the camera picker via intent
     * Source: http://developer.android.com/training/camera/photobasics.html
     */
    public void showCameraDialog() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                // Create an image file name
                String imageFileName = getString(R.string.app_name) + "_" + System.currentTimeMillis();
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM), "Camera");
                photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);

                // Save a file: path for use with ACTION_VIEW intents
                cameraPictureFilepath = photoFile.getAbsolutePath();

            } catch (IOException ex) {
                Helpers.showSnackBar(this, R.string.main__error_camera_cannot_start);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri uri = FileProvider.getUriForFile(this, getString(R.string.app_fileprovider), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                } else {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }
                startActivityForResult(takePictureIntent, REQUEST_TAKE_CAMERA_PICTURE);
            }
        }

    }

    public void onImageTemplateWasChosen(String filePath, boolean bIsAsset) {
        final Intent intent = new Intent(this, MemeCreateActivity.class);
        intent.putExtra(MemeCreateActivity.EXTRA_IMAGE_PATH, filePath);
        intent.putExtra(MemeCreateActivity.ASSET_IMAGE, bIsAsset);
        Helpers.animateToActivity(this, intent, MemeCreateActivity.RESULT_MEME_EDITING_FINISHED);
    }

    public void openImageViewActivityWithImage(String imagePath) {
        isShowingFullscreenImage = true;

        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra(IMAGE_PATH, imagePath);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Helpers.animateToActivity(this, intent, 0);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int tabPos = tab.getPosition();
        if (tabPos >= 0 && tabPos < MemeLibConfig.MEME_CATEGORIES.ALL.length) {
            mMemeCategory = app.getMemeCategory(MemeLibConfig.MEME_CATEGORIES.ALL[tabPos]);
            MemeOriginInterface memeOriginObject = new MemeOriginAssets(mMemeCategory, getAssets());
            GridRecycleAdapter recyclerMemeAdapter = new GridRecycleAdapter(memeOriginObject, this);
            recyclerMemeList.setAdapter(recyclerMemeAdapter);
            app.settings.setLastSelectedCategory(MemeLibConfig.getIndexOfCategory(mMemeCategory.getCategoryName()));
        }
    }

    private final RectF point = new RectF(0, 0, 0, 0);
    private static final int SWIPE_MIN_DX = 150;
    private static final int SWIPE_MAX_DY = 90;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            point.set(event.getX(), event.getY(), 0, 0);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            point.set(point.left, point.top, event.getX(), event.getY());
            if (Math.abs(point.width()) > SWIPE_MIN_DX && Math.abs(point.height()) < SWIPE_MAX_DY) {

                selectTab(tabLayout.getSelectedTabPosition()
                        + (point.width() > 0 ? -1 : +1)    // R->L : L<-R
                );
            }
        }
        return super.dispatchTouchEvent(event);
    }


    //########################
    //## Single line overrides
    //########################
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main__menu, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return handleBarClick(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleBarClick(item);
    }


}
