package com.luck.picture.selector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.SharedElementCallback;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.transition.Explode;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.luck.picture.selector.broadcast.BroadcastAction;
import com.luck.picture.selector.broadcast.BroadcastManager;
import com.luck.picture.selector.config.PictureConfig;
import com.luck.picture.selector.config.PictureMimeType;
import com.luck.picture.selector.config.PictureSelectionConfig;
import com.luck.picture.selector.dialog.PictureCustomDialog;
import com.luck.picture.selector.entity.LocalMedia;
import com.luck.picture.selector.listener.OnImageCompleteCallback;
import com.luck.picture.selector.permissions.PermissionChecker;
import com.luck.picture.selector.photoview.PhotoView;
import com.luck.picture.selector.thread.PictureThreadUtils;
import com.luck.picture.selector.tools.AttrsUtils;
import com.luck.picture.selector.tools.DateUtils;
import com.luck.picture.selector.tools.JumpUtils;
import com.luck.picture.selector.tools.MediaUtils;
import com.luck.picture.selector.tools.PictureFileUtils;
import com.luck.picture.selector.tools.ScreenUtils;
import com.luck.picture.selector.tools.SdkVersionUtils;
import com.luck.picture.selector.tools.ToastUtils;
import com.luck.picture.selector.tools.ValueOf;
import com.luck.picture.selector.widget.PreviewViewPager;
import com.luck.picture.selector.widget.longimage.ImageSource;
import com.luck.picture.selector.widget.longimage.ImageViewState;
import com.luck.picture.selector.widget.longimage.SubsamplingScaleImageView;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okio.BufferedSource;
import okio.Okio;

/**
 * @author???luck
 * @data???2017/01/18 ??????1:00
 * @??????: ????????????
 */
public class PictureExternalPreviewActivity extends PictureBaseActivity implements View.OnClickListener {
    private int mScreenWidth, mScreenHeight;
    private ImageButton ibLeftBack;
    private TextView tvTitle;
    private PreviewViewPager viewPager;
    private final List<LocalMedia> images = new ArrayList<>();
    private SimpleFragmentAdapter adapter;
    private String downloadPath;
    private String mMimeType;
    private ImageButton ibDelete;
    private View mTitleBar;
    /**
     * ??????????????????????????????????????????
     */
    private int enterPosition = 0, currentPosition;

    @Override
    public int getResourceId() {
        return R.layout.picture_activity_external_preview;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        //????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            supportPostponeEnterTransition();//???????????? ?????????fragment??????????????????????????????start
        }
        mTitleBar = findViewById(R.id.titleBar);
        tvTitle = findViewById(R.id.picture_title);
        ibLeftBack = findViewById(R.id.left_back);
        ibDelete = findViewById(R.id.ib_delete);
        viewPager = findViewById(R.id.preview_pager);
        enterPosition = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        currentPosition = enterPosition;
        mScreenWidth = ScreenUtils.getScreenWidth(getContext());
        mScreenHeight = ScreenUtils.getScreenHeight(getContext());
        List<LocalMedia> mediaList = getIntent().getParcelableArrayListExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        if (mediaList != null && mediaList.size() > 0) {
            images.addAll(mediaList);
        }
        ibLeftBack.setOnClickListener(this);
        ibDelete.setOnClickListener(this);
        ibDelete.setVisibility(PictureSelectionConfig.style != null ? PictureSelectionConfig.style.pictureExternalPreviewGonePreviewDelete
                ? View.VISIBLE : View.GONE : View.GONE);
        initViewPageAdapterData();
    }

    /**
     * ????????????
     */
    @Override
    public void initPictureSelectorStyle() {
        if (PictureSelectionConfig.style != null) {
            if (PictureSelectionConfig.style.pictureTitleTextColor != 0) {
                tvTitle.setTextColor(PictureSelectionConfig.style.pictureTitleTextColor);
            }
            if (PictureSelectionConfig.style.pictureTitleTextSize != 0) {
                tvTitle.setTextSize(PictureSelectionConfig.style.pictureTitleTextSize);
            }
            if (PictureSelectionConfig.style.pictureLeftBackIcon != 0) {
                ibLeftBack.setImageResource(PictureSelectionConfig.style.pictureLeftBackIcon);
            }
            if (PictureSelectionConfig.style.pictureExternalPreviewDeleteStyle != 0) {
                ibDelete.setImageResource(PictureSelectionConfig.style.pictureExternalPreviewDeleteStyle);
            }
//            if (PictureSelectionConfig.style.pictureTitleBarBackgroundColor != 0) {
//                mTitleBar.setBackgroundColor(colorPrimary);
//            }
        } else {
            int previewBgColor = AttrsUtils.getTypeValueColor(getContext(), R.attr.picture_ac_preview_title_bg);
//            if (previewBgColor != 0) {
//                mTitleBar.setBackgroundColor(previewBgColor);
//            } else {
//                mTitleBar.setBackgroundColor(colorPrimary);
//            }
        }
    }

    private void initViewPageAdapterData() {
        tvTitle.setText(getString(R.string.picture_preview_image_num,
                currentPosition + 1, images.size()));
        adapter = new SimpleFragmentAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int index) {
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        index + 1, images.size()));
                currentPosition = index;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //???????????????????????????  ????????????????????????????????????????????????  ????????????????????????????????????????????????????????????
            //??????????????? ?????????????????????????????????activity
            //?????????sharedElements??????put view,??????view??????transitionname????????????
            setEnterSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    String path = images.get(viewPager.getCurrentItem()).getRealPath();
                    View view =viewPager.findViewWithTag(viewPager.getCurrentItem());
                    sharedElements.clear();
                    sharedElements.put(path, view);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.left_back) {
            onBackPressed();
        } else if (id == R.id.ib_delete) {
            if (images.size() > 0) {
                int currentItem = viewPager.getCurrentItem();
                images.remove(currentItem);
                adapter.removeCacheView(currentItem);
                // ????????????????????????
                Bundle bundle = new Bundle();
                bundle.putInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION, currentItem);
                BroadcastManager.getInstance(getContext())
                        .action(BroadcastAction.ACTION_DELETE_PREVIEW_POSITION)
                        .extras(bundle).broadcast();
                if (images.size() == 0) {
                    onBackPressed();
                    return;
                }
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        currentPosition + 1, images.size()));
                currentPosition = currentItem;
                adapter.notifyDataSetChanged();
            }
        }
    }

    public class SimpleFragmentAdapter extends PagerAdapter {

        /**
         * ????????????????????????
         */
        private static final int MAX_CACHE_SIZE = 20;
        /**
         * ??????view
         */
        private SparseArray<View> mCacheView;

        private void clear() {
            if (null != mCacheView) {
                mCacheView.clear();
                mCacheView = null;
            }
        }

        public void removeCacheView(int position) {
            if (mCacheView != null && position < mCacheView.size()) {
                mCacheView.removeAt(position);
            }
        }

        public SimpleFragmentAdapter() {
            super();
            this.mCacheView = new SparseArray<>();
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            (container).removeView((View) object);
            if (mCacheView.size() > MAX_CACHE_SIZE) {
                mCacheView.remove(position);
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View contentView = mCacheView.get(position);

            if (contentView == null) {
                contentView = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.picture_image_preview, container, false);
                mCacheView.put(position, contentView);
            }
            contentView.setTag(position);
            // ???????????????
            final PhotoView photoView = contentView.findViewById(R.id.preview_image);
            // ????????????
            final SubsamplingScaleImageView longImageView = contentView.findViewById(R.id.longImg);
            // ??????????????????
            ImageView ivPlay = contentView.findViewById(R.id.iv_play);
            LocalMedia media = images.get(position);
            if (config.isAutoScalePreviewImage) {
                float width = Math.min(media.getWidth(), media.getHeight());
                float height = Math.max(media.getHeight(), media.getWidth());
                if (width > 0 && height > 0) {
                    // ??????????????????????????????????????????????????????
                    int displayHeight = (int) Math.ceil(width * height / width);
                    //????????????????????????????????? ??????????????????????????????
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) photoView.getLayoutParams();
                    layoutParams.width = mScreenWidth;
                    layoutParams.height = displayHeight < mScreenHeight ? displayHeight + mScreenHeight : displayHeight;
                    layoutParams.gravity = Gravity.CENTER;
                }
            }
            final String path;
            if (media.isCut() && !media.isCompressed()) {
                // ?????????
                path = media.getCutPath();
            } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
                // ?????????,???????????????????????????,??????????????????????????????
                path = media.getCompressPath();
            } else if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
                // AndroidQ??????path
                path = media.getAndroidQToPath();
            } else {
                // ??????
                path = media.getPath();
            }
            boolean isHttp = PictureMimeType.isHasHttp(path);
            String mimeType = isHttp && TextUtils.isEmpty(media.getMimeType()) ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
            boolean isHasVideo = PictureMimeType.isHasVideo(mimeType);
            ivPlay.setVisibility(isHasVideo ? View.VISIBLE : View.GONE);
            boolean isGif = PictureMimeType.isGif(mimeType);
            boolean eqLongImg = MediaUtils.isLongImg(media);
            photoView.setVisibility(eqLongImg && !isGif ? View.GONE : View.VISIBLE);
            longImageView.setVisibility(eqLongImg && !isGif ? View.VISIBLE : View.GONE);
            // ????????????gif?????????gif???
            if (isGif && !media.isCompressed()) {
                if (PictureSelectionConfig.imageEngine != null) {
                    PictureSelectionConfig.imageEngine.loadAsGifImage
                            (getContext(), path, photoView);
                }
            } else {
                if (PictureSelectionConfig.imageEngine != null) {
                    if (isHttp) {
                        // ????????????
                        PictureSelectionConfig.imageEngine.loadImage(contentView.getContext(), path,
                                photoView, longImageView, new OnImageCompleteCallback() {
                                    @Override
                                    public void onShowLoading() {
                                        showPleaseDialog();
                                    }

                                    @Override
                                    public void onHideLoading() {
                                        dismissDialog();
                                    }
                                });
                    } else {
                        if (eqLongImg) {
                            displayLongPic(PictureMimeType.isContent(path)
                                    ? Uri.parse(path) : Uri.fromFile(new File(path)), longImageView);
                        } else {
                            PictureSelectionConfig.imageEngine.loadImage(contentView.getContext(), path, photoView);
                        }
                    }
                }
            }
            photoView.setOnViewTapListener((view, x, y) -> {
                onBackPressed();
            });
            longImageView.setOnClickListener(v -> {
                onBackPressed();
            });
            if (!isHasVideo) {
                longImageView.setOnLongClickListener(v -> {
                    if (config.isNotPreviewDownload) {
                        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            downloadPath = path;
                            String currentMimeType = PictureMimeType.isHasHttp(path) && TextUtils.isEmpty(media.getMimeType()) ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
                            mMimeType = PictureMimeType.isJPG(currentMimeType) ? PictureMimeType.MIME_TYPE_JPEG : currentMimeType;
                            showDownLoadDialog();
                        } else {
                            PermissionChecker.requestPermissions(PictureExternalPreviewActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
                        }
                    }
                    return true;
                });
            }
            if (!isHasVideo) {
                photoView.setOnLongClickListener(v -> {
                    if (config.isNotPreviewDownload) {
                        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            downloadPath = path;
                            String currentMimeType = PictureMimeType.isHasHttp(path) && TextUtils.isEmpty(media.getMimeType()) ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
                            mMimeType = PictureMimeType.isJPG(currentMimeType) ? PictureMimeType.MIME_TYPE_JPEG : currentMimeType;
                            showDownLoadDialog();
                        } else {
                            PermissionChecker.requestPermissions(PictureExternalPreviewActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
                        }
                    }
                    return true;
                });
            }
            ivPlay.setOnClickListener(v -> {
                if (PictureSelectionConfig.customVideoPlayCallback != null) {
                    PictureSelectionConfig.customVideoPlayCallback.startPlayVideo(media);
                } else {
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString(PictureConfig.EXTRA_VIDEO_PATH, path);
                    intent.putExtras(bundle);
                    JumpUtils.startPictureVideoPlayActivity(container.getContext(), bundle, PictureConfig.PREVIEW_VIDEO_CODE);
                }
            });
//            container.removeAllViews();
            container.addView(contentView, 0);
            return contentView;
        }

    }

    /**
     * ????????????
     *
     * @param uri
     * @param longImg
     */
    private void displayLongPic(Uri uri, SubsamplingScaleImageView longImg) {
        longImg.setQuickScaleEnabled(true);
        longImg.setZoomEnabled(true);
        longImg.setDoubleTapZoomDuration(100);
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
        longImg.setImage(ImageSource.uri(uri), new ImageViewState(0, new PointF(0, 0), 0));
    }

    /**
     * ??????????????????
     */
    private void showDownLoadDialog() {
        if (!isFinishing() && !TextUtils.isEmpty(downloadPath)) {
            final PictureCustomDialog dialog =
                    new PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog);
            Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
            Button btn_commit = dialog.findViewById(R.id.btn_commit);
            TextView tvTitle = dialog.findViewById(R.id.tvTitle);
            TextView tv_content = dialog.findViewById(R.id.tv_content);
            tvTitle.setText(getString(R.string.picture_prompt));
            tv_content.setText(getString(R.string.picture_prompt_content));
            btn_cancel.setOnClickListener(v -> {
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            });
            btn_commit.setOnClickListener(view -> {
                boolean isHttp = PictureMimeType.isHasHttp(downloadPath);
                showPleaseDialog();
                if (isHttp) {
                    PictureThreadUtils.executeBySingle(new PictureThreadUtils.SimpleTask<String>() {
                        @Override
                        public String doInBackground() {
                            return showLoadingImage(downloadPath);
                        }

                        @Override
                        public void onSuccess(String result) {
                            onSuccessful(result);
                        }
                    });
                } else {
                    // ?????????????????????
                    try {
                        if (PictureMimeType.isContent(downloadPath)) {
                            savePictureAlbumAndroidQ(PictureMimeType.isContent(downloadPath) ? Uri.parse(downloadPath) : Uri.fromFile(new File(downloadPath)));
                        } else {
                            // ??????????????????????????????
                            savePictureAlbum();
                        }
                    } catch (Exception e) {
                        ToastUtils.s(getContext(), getString(R.string.picture_save_error) + "\n" + e.getMessage());
                        dismissDialog();
                        e.printStackTrace();
                    }
                }
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    /**
     * ???????????????????????????
     *
     * @throws Exception
     */
    private void savePictureAlbum() throws Exception {
        String suffix = PictureMimeType.getLastImgSuffix(mMimeType);
        String state = Environment.getExternalStorageState();
        File rootDir = state.equals(Environment.MEDIA_MOUNTED)
                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                : getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (rootDir != null && !rootDir.exists() && rootDir.mkdirs()) {
        }
        File folderDir = new File(SdkVersionUtils.checkedAndroid_Q() || !state.equals(Environment.MEDIA_MOUNTED)
                ? rootDir.getAbsolutePath() : rootDir.getAbsolutePath() + File.separator + PictureMimeType.CAMERA + File.separator);
        if (!folderDir.exists() && folderDir.mkdirs()) {
        }
        String fileName = DateUtils.getCreateFileName("IMG_") + suffix;
        File file = new File(folderDir, fileName);
        PictureFileUtils.copyFile(downloadPath, file.getAbsolutePath());
        onSuccessful(file.getAbsolutePath());
    }

    /**
     * ??????????????????
     *
     * @param result
     */
    private void onSuccessful(String result) {
        dismissDialog();
        if (!TextUtils.isEmpty(result)) {
            try {
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    File file = new File(result);
                    MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), null);
                    new PictureMediaScannerConnection(getContext(), file.getAbsolutePath(), () -> {
                    });
                }
                ToastUtils.s(getContext(), getString(R.string.picture_save_success) + "\n" + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error));
        }
    }

    /**
     * ???????????????picture ?????????Android Q???????????????????????????????????????????????????????????????SAF??????
     *
     * @param inputUri
     */
    private void savePictureAlbumAndroidQ(Uri inputUri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"));
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()));
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType);
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null) {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error));
            return;
        }
        PictureThreadUtils.executeBySingle(new PictureThreadUtils.SimpleTask<String>() {

            @Override
            public String doInBackground() {
                BufferedSource buffer = null;
                try {
                    InputStream inputStream = PictureContentResolver.getContentResolverOpenInputStream(getContext(), inputUri);
                    buffer = Okio.buffer(Okio.source(Objects.requireNonNull(inputStream)));

                    OutputStream outputStream = PictureContentResolver.getContentResolverOpenOutputStream(getContext(), uri);
                    boolean bufferCopy = PictureFileUtils.bufferCopy(buffer, outputStream);
                    if (bufferCopy) {
                        return PictureFileUtils.getPath(getContext(), uri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (buffer != null && buffer.isOpen()) {
                        PictureFileUtils.close(buffer);
                    }
                }
                return "";
            }

            @Override
            public void onSuccess(String result) {
                PictureThreadUtils.cancel(PictureThreadUtils.getSinglePool());
                onSuccessful(result);
            }
        });
    }


    /**
     * ??????Q????????????uri
     *
     * @return
     */
    private Uri createOutImageUri() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"));
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()));
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType);
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);

        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    // ???????????????????????????
    public String showLoadingImage(String urlPath) {
        Uri outImageUri = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedSource inBuffer = null;
        try {
            if (SdkVersionUtils.checkedAndroid_Q()) {
                outImageUri = createOutImageUri();
            } else {
                String suffix = PictureMimeType.getLastImgSuffix(mMimeType);
                String state = Environment.getExternalStorageState();
                File rootDir =
                        state.equals(Environment.MEDIA_MOUNTED)
                                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                : getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (rootDir != null) {
                    if (!rootDir.exists()) {
                        rootDir.mkdirs();
                    }
                    File folderDir = new File(!state.equals(Environment.MEDIA_MOUNTED)
                            ? rootDir.getAbsolutePath() : rootDir.getAbsolutePath() + File.separator + PictureMimeType.CAMERA + File.separator);
                    if (!folderDir.exists()) {
                        folderDir.mkdirs();
                    }
                    String fileName = DateUtils.getCreateFileName("IMG_") + suffix;
                    File file = new File(folderDir, fileName);
                    outImageUri = Uri.fromFile(file);
                }
            }
            if (outImageUri != null) {
                outputStream = PictureContentResolver.getContentResolverOpenOutputStream(getContext(), outImageUri);
                URL u = new URL(urlPath);
                inputStream = u.openStream();
                inBuffer = Okio.buffer(Okio.source(inputStream));
                boolean bufferCopy = PictureFileUtils.bufferCopy(inBuffer, outputStream);
                if (bufferCopy) {
                    return PictureFileUtils.getPath(this, outImageUri);
                }
            }
        } catch (Exception e) {
            if (outImageUri != null && SdkVersionUtils.checkedAndroid_Q()) {
                getContentResolver().delete(outImageUri, null, null);
            }
        } finally {
            PictureFileUtils.close(inputStream);
            PictureFileUtils.close(outputStream);
            PictureFileUtils.close(inBuffer);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void supportFinishAfterTransition() {
        Intent data = new Intent();
        data.putExtra("index", viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        super.supportFinishAfterTransition();
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        data.putExtra("index", viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        super.supportFinishAfterTransition();
    }

    private void exitAnimation() {
//        overridePendingTransition(R.anim.picture_anim_fade_in, PictureSelectionConfig.windowAnimationStyle.activityPreviewExitAnimation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.clear();
        }
        PictureSelectionConfig.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE) {// ????????????
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    showDownLoadDialog();
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction));
                }
            }
        }
    }
}
