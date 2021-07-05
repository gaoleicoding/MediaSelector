package com.luck.pictureselector.activity;

import android.Manifest;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.luck.picture.selector.PictureMediaScannerConnection;
import com.luck.picture.selector.PictureSelector;
import com.luck.picture.selector.animators.AnimationType;
import com.luck.picture.selector.app.PictureAppMaster;
import com.luck.picture.selector.broadcast.BroadcastAction;
import com.luck.picture.selector.broadcast.BroadcastManager;
import com.luck.picture.selector.config.PictureConfig;
import com.luck.picture.selector.config.PictureMimeType;
import com.luck.picture.selector.decoration.GridSpacingItemDecoration;
import com.luck.picture.selector.dialog.PictureCustomDialog;
import com.luck.picture.selector.entity.LocalMedia;
import com.luck.picture.selector.entity.MediaExtraInfo;
import com.luck.picture.selector.listener.OnCallbackListener;
import com.luck.picture.selector.listener.OnCustomImagePreviewCallback;
import com.luck.picture.selector.listener.OnPermissionDialogOptionCallback;
import com.luck.picture.selector.listener.OnPermissionsObtainCallback;
import com.luck.picture.selector.listener.OnResultCallbackListener;
import com.luck.picture.selector.listener.OnVideoSelectedPlayCallback;
import com.luck.picture.selector.manager.PictureCacheManager;
import com.luck.picture.selector.permissions.PermissionChecker;
import com.luck.picture.selector.style.PictureSelectorUIStyle;
import com.luck.picture.selector.tools.MediaUtils;
import com.luck.picture.selector.tools.ScreenUtils;
import com.luck.picture.selector.tools.SdkVersionUtils;
import com.luck.picture.selector.tools.ToastUtils;
import com.luck.pictureselector.R;
import com.luck.pictureselector.adapter.FullyGridLayoutManager;
import com.luck.pictureselector.adapter.PicturePreviewGridAdapter;
import com.luck.pictureselector.engine.GlideEngine;
import com.luck.pictureselector.listener.DragListener;
import com.luck.pictureselector.utils.LogUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServiceEvaluateActivity extends AppCompatActivity {
    private final static String TAG = "EvaluateSubmitActivity";
    private PicturePreviewGridAdapter mAdapter;
    private final int maxSelectNum = 6;
    private final int chooseMode = PictureMimeType.ofAll();
    private PictureSelectorUIStyle mSelectorUIStyle;
    private ItemTouchHelper mItemTouchHelper;
    private DragListener mDragListener;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_evaluate);
        mSelectorUIStyle = PictureSelectorUIStyle.ofDefaultStyle();

        RecyclerView mRecyclerView = findViewById(R.id.recycler);

        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                3, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);

        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3,
                ScreenUtils.dip2px(this, 12), false));
        mAdapter = new PicturePreviewGridAdapter(getContext(), onAddPicClickListener);
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList("selectorList") != null) {
            mAdapter.setList(savedInstanceState.getParcelableArrayList("selectorList"));
        }

        mAdapter.setSelectMax(maxSelectNum);
        mRecyclerView.setAdapter(mAdapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (bundle != null) {
                        int i = bundle.getInt("index", 0);
                        sharedElements.clear();
                        names.clear();
                        View itemView = manager.findViewByPosition(i);
                        ImageView imageView = itemView.findViewById(R.id.evaluate_iv_pic);
                        //注意这里第二个参数，如果放置的是条目的item则动画不自然。放置对应的imageView则完美
                        sharedElements.put(mAdapter.getList().get(i).getRealPath(), imageView);
                        bundle = null;
                    }
                }
            });
        }

        mAdapter.setOnItemClickListener((v, position) -> {
            List<LocalMedia> selectList = mAdapter.getList();
            if (selectList.size() > 0) {
                LocalMedia media = selectList.get(position);
                String mimeType = media.getMimeType();
                int mediaType = PictureMimeType.getMimeType(mimeType);
                switch (mediaType) {
                    case PictureConfig.TYPE_VIDEO:
                        // 预览视频
                        PictureSelector.create(ServiceEvaluateActivity.this)
                                .themeStyle(R.style.picture_default_style)
//                                .setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                                .externalPictureVideo(TextUtils.isEmpty(media.getAndroidQToPath()) ? media.getPath() : media.getAndroidQToPath());
                        break;
                    case PictureConfig.TYPE_AUDIO:
                        // 预览音频
                        PictureSelector.create(ServiceEvaluateActivity.this)
                                .externalPictureAudio(PictureMimeType.isContent(media.getPath()) ? media.getAndroidQToPath() : media.getPath());
                        break;
                    default:
                        // 预览图片 可自定长按保存路径
//                        PictureWindowAnimationStyle animationStyle = new PictureWindowAnimationStyle();
//                        animationStyle.activityPreviewEnterAnimation = R.anim.picture_anim_up_in;
//                        animationStyle.activityPreviewExitAnimation = R.anim.picture_anim_down_out;
                        PictureSelector.create(ServiceEvaluateActivity.this)
                                .themeStyle(R.style.picture_default_style) // xml设置主题
//                                .setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                                //.setPictureWindowAnimationStyle(animationStyle)// 自定义页面启动动画
                                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// 设置相册Activity方向，不设置默认使用系统
                                .isNotPreviewDownload(true)// 预览图片长按是否可以下载
                                //.bindCustomPlayVideoCallback(new MyVideoSelectedPlayCallback(getContext()))// 自定义播放回调控制，用户可以使用自己的视频播放界面
                                .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
                                .openExternalPreview(position, v.findViewById(R.id.evaluate_iv_pic), selectList);
                        break;
                }
            }
        });

//        mAdapter.setItemLongClickListener((holder, position, v) -> {
        //如果item不是最后一个，则执行拖拽

//            int size = mAdapter.getData().size();
//            if (size != maxSelectNum) {
//                mItemTouchHelper.startDrag(holder);
//                return;
//            }
//            if (holder.getLayoutPosition() != size - 1) {
//                mItemTouchHelper.startDrag(holder);
//            }
//        });

        mDragListener = new DragListener() {
            @Override
            public void deleteState(boolean isDelete) {
//                if (isDelete) {
//                    tvDeleteText.setText(getString(R.string.app_let_go_drag_delete));
//                    tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_let_go_delete, 0, 0);
//                } else {
//                    tvDeleteText.setText(getString(R.string.app_drag_delete));
//                    tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.picture_icon_delete, 0, 0);
//                }

            }

            @Override
            public void dragState(boolean isStart) {
//                int visibility = tvDeleteText.getVisibility();
//                if (isStart) {
//                    if (visibility == View.GONE) {
//                        tvDeleteText.animate().alpha(1).setDuration(300).setInterpolator(new AccelerateInterpolator());
//                        tvDeleteText.setVisibility(View.VISIBLE);
//                    }
//                } else {
//                    if (visibility == View.VISIBLE) {
//                        tvDeleteText.animate().alpha(0).setDuration(300).setInterpolator(new AccelerateInterpolator());
//                        tvDeleteText.setVisibility(View.GONE);
//                    }
//                }
            }
        };

        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int itemViewType = viewHolder.getItemViewType();
                if (isCanDrag(itemViewType)) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
                return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP
                        | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                //得到item原来的position
                try {
                    int fromPosition = viewHolder.getAdapterPosition();
                    //得到目标position
                    int toPosition = target.getAdapterPosition();
                    int itemViewType = target.getItemViewType();
                    if (isCanDrag(itemViewType)) {
                        if (fromPosition < toPosition) {
                            for (int i = fromPosition; i < toPosition; i++) {
                                Collections.swap(mAdapter.getList(), i, i + 1);
                            }
                        } else {
                            for (int i = fromPosition; i > toPosition; i--) {
                                Collections.swap(mAdapter.getList(), i, i - 1);
                            }
                        }
                        mAdapter.notifyItemMoved(fromPosition, toPosition);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                int itemViewType = viewHolder.getItemViewType();
                if (isCanDrag(itemViewType)) {
                    if (null == mDragListener) {
                        return;
                    }
                   /* if (needScaleBig) {
                        //如果需要执行放大动画
                        viewHolder.itemView.animate().scaleXBy(0.1f).scaleYBy(0.1f).setDuration(100);
                        //执行完成放大动画,标记改掉
                        needScaleBig = false;
                        //默认不需要执行缩小动画，当执行完成放大 并且松手后才允许执行
                        needScaleSmall = false;
                    }
                    int sh = recyclerView.getHeight() + tvDeleteText.getHeight();
                    int ry = tvDeleteText.getBottom() - sh;
                    if (dY >= ry) {
                    拖到删除处
                        mDragListener.deleteState(true);
                        if (isUpward) {
                            //在删除处放手，则删除item
                            viewHolder.itemView.setVisibility(View.INVISIBLE);
                            mAdapter.delete(viewHolder.getAdapterPosition());
                            resetState();
                            return;
                        }
                    } else {//没有到删除处
                        if (View.INVISIBLE == viewHolder.itemView.getVisibility()) {
                            //如果viewHolder不可见，则表示用户放手，重置删除区域状态
                            mDragListener.dragState(false);
                        }
                        if (needScaleSmall) {//需要松手后才能执行
                            viewHolder.itemView.animate().scaleXBy(1f).scaleYBy(1f).setDuration(100);
                        }
                        mDragListener.deleteState(false);
                    }*/
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                int itemViewType = viewHolder != null ? viewHolder.getItemViewType() : PicturePreviewGridAdapter.ITEM_TYPE_ADD_PICTURE;
                if (isCanDrag(itemViewType)) {
                    if (ItemTouchHelper.ACTION_STATE_DRAG == actionState && mDragListener != null) {
                        mDragListener.dragState(true);
                    }
                    super.onSelectedChanged(viewHolder, actionState);
                }
            }

            @Override
            public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
                return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int itemViewType = viewHolder.getItemViewType();
                if (isCanDrag(itemViewType)) {
                    viewHolder.itemView.setAlpha(1.0f);
                    super.clearView(recyclerView, viewHolder);
                    mAdapter.notifyDataSetChanged();
                    resetState();
                }
            }
        });

        // 绑定拖拽事件
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        // 注册广播
        BroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver,
                BroadcastAction.ACTION_DELETE_PREVIEW_POSITION);
    }

    private boolean isCanDrag(int itemViewType) {
        return itemViewType != PicturePreviewGridAdapter.ITEM_TYPE_ADD_PICTURE && itemViewType != PicturePreviewGridAdapter.ITEM_TYPE_ADD_VIDEO;
    }

    /**
     * 重置
     */
    private void resetState() {
        if (mDragListener != null) {
            mDragListener.deleteState(false);
            mDragListener.dragState(false);
        }
    }

    /**
     * 清空缓存包括裁剪、压缩、AndroidQToPath所生成的文件，注意调用时机必须是处理完本身的业务逻辑后调用；非强制性
     */
    private void clearCache() {
        // 清空图片缓存，包括裁剪、压缩后的图片 注意:必须要在上传完成后调用 必须要获取权限
        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //PictureCacheManager.deleteCacheDirFile(this, PictureMimeType.ofImage());
            PictureCacheManager.deleteAllCacheDirRefreshFile(getContext());
        } else {
            PermissionChecker.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
        }
    }

    private final PicturePreviewGridAdapter.OnAddPicClickListener onAddPicClickListener = new PicturePreviewGridAdapter.OnAddPicClickListener() {
        @Override
        public void onAddPicClick(int type) {
            // boolean mode = cb_mode.isChecked();
            // 进入相册 以下是例子：不需要的api可以不写
            int language = -1;
            int animationMode = AnimationType.DEFAULT_ANIMATION;
            PictureSelector.create(ServiceEvaluateActivity.this)
                    .openGallery(chooseMode)// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                    .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
                    //.theme(themeId)// 主题样式设置 具体参考 values/styles   用法：R.style.picture.white.style v2.3.3后 建议使用setPictureStyle()动态方式
                    .setPictureUIStyle(mSelectorUIStyle)
                    //.setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                    //.setPictureCropStyle(mCropParameterStyle)// 动态自定义裁剪主题
                    //.setPictureWindowAnimationStyle(mWindowAnimationStyle)// 自定义相册启动退出动画
                    .isWeChatStyle(false)// 是否开启微信图片选择风格
                    .isUseCustomCamera(false)// 是否使用自定义相机
                    .setLanguage(language)// 设置语言，默认中文
                    .isPageStrategy(true)// 是否开启分页策略 & 每页多少条；默认开启
                    .setRecyclerAnimationMode(animationMode)// 列表动画效果
                    .isWithVideoImage(true)// 图片和视频是否可以同选,只在ofAll模式下有效
                    //.isSyncCover(true)// 是否强制从MediaStore里同步相册封面，如果相册封面没显示异常则没必要设置
                    //.isCameraAroundState(false) // 是否开启前置摄像头，默认false，如果使用系统拍照 可能部分机型会有兼容性问题
                    //.isCameraRotateImage(false) // 拍照图片旋转是否自动纠正
                    .isAutoRotating(true)// 压缩时自动纠正有旋转的图片
                    .isMaxSelectEnabledMask(true)// 选择数到了最大阀值列表是否启用蒙层效果
                    //.isAutomaticTitleRecyclerTop(false)// 连续点击标题栏RecyclerView是否自动回到顶部,默认true
                    //.loadCacheResourcesCallback(GlideCacheEngine.createCacheEngine())// 获取图片资源缓存，主要是解决华为10部分机型在拷贝文件过多时会出现卡的问题，这里可以判断只在会出现一直转圈问题机型上使用
                    //.setOutputCameraPath(createCustomCameraOutPath())// 自定义相机输出目录
                    //.setButtonFeatures(CustomCameraView.BUTTON_STATE_BOTH)// 设置自定义相机按钮状态
                    .setCaptureLoadingColor(ContextCompat.getColor(getContext(), R.color.app_color_blue))
                    .maxSelectNum(maxSelectNum)// 最大图片选择数量
                    .minSelectNum(1)// 最小选择数量
                    .maxVideoSelectNum(1) // 视频最大选择数量
                    //.minVideoSelectNum(1)// 视频最小选择数量
                    //.closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 关闭在AndroidQ下获取图片或视频宽高相反自动转换
                    .imageSpanCount(4)// 每行显示个数
                    //.queryFileSize() // 过滤最大资源,已废弃
                    //.filterMinFileSize(5)// 过滤最小资源，单位kb
                    //.filterMaxFileSize()// 过滤最大资源，单位kb
                    .isReturnEmpty(false)// 未选择数据时点击按钮是否可以返回
                    .closeAndroidQChangeWH(true)//如果图片有旋转角度则对换宽高,默认为true
                    .closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 如果视频有旋转角度则对换宽高,默认为false
                    .isAndroidQTransform(true)// 是否需要处理Android Q 拷贝至应用沙盒的操作，只针对compress(false); && .isEnableCrop(false);有效,默认处理
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// 设置相册Activity方向，不设置默认使用系统
                    .isOriginalImageControl(false)// 是否显示原图控制按钮，如果设置为true则用户可以自由选择是否使用原图，压缩、裁剪功能将会失效
                    //.isAutoScalePreviewImage(true)// 如果图片宽度不能充满屏幕则自动处理成充满模式
                    //.bindCustomPlayVideoCallback(new MyVideoSelectedPlayCallback(getContext()))// 自定义视频播放回调控制，用户可以使用自己的视频播放界面
                    //.bindCustomPreviewCallback(new MyCustomPreviewInterfaceListener())// 自定义图片预览回调接口
                    //.bindCustomCameraInterfaceListener(new MyCustomCameraInterfaceListener())// 提供给用户的一些额外的自定义操作回调
                    //.bindCustomPermissionsObtainListener(new MyPermissionsObtainCallback())// 自定义权限拦截
                    //.bindCustomChooseLimitListener(new MyChooseLimitCallback()) // 自定义选择限制条件Dialog
                    //.cameraFileName(System.currentTimeMillis() +".jpg")    // 重命名拍照文件名、如果是相册拍照则内部会自动拼上当前时间戳防止重复，注意这个只在使用相机时可以使用，如果使用相机又开启了压缩或裁剪 需要配合压缩和裁剪文件名api
                    //.renameCompressFile(System.currentTimeMillis() +".jpg")// 重命名压缩文件名、 如果是多张压缩则内部会自动拼上当前时间戳防止重复
                    //.renameCropFileName(System.currentTimeMillis() + ".jpg")// 重命名裁剪文件名、 如果是多张裁剪则内部会自动拼上当前时间戳防止重复
                    .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选
                    //.isSingleDirectReturn(cb_single_back.isChecked())// 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                    .isPreviewImage(true)// 是否可预览图片
                    .isPreviewVideo(true)// 是否可预览视频

                    //.querySpecifiedFormatSuffix(PictureMimeType.ofJPEG())// 查询指定后缀格式资源
                    //.queryMimeTypeConditions(PictureMimeType.ofWEBP())
                    .isEnablePreviewAudio(true) // 是否可播放音频
                    .isCamera(true)// 是否显示拍照按钮
                    //.isMultipleSkipCrop(false)// 多图裁剪时是否支持跳过，默认支持
                    //.isMultipleRecyclerAnimation(false)// 多图裁剪底部列表显示动画效果
                    .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                    //.imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg,Android Q使用PictureMimeType.PNG_Q
                    .isEnableCrop(false)// 是否裁剪
                    //.basicUCropConfig()//对外提供所有UCropOptions参数配制，但如果PictureSelector原本支持设置的还是会使用原有的设置
                    .isCompress(true)// 是否压缩
                    //.compressFocusAlpha(true)// 压缩时是否开启透明通道
                    //.compressEngine(ImageCompressEngine.createCompressEngine()) // 自定义压缩引擎
                    .compressQuality(80)// 图片压缩后输出质量 0~ 100
                    .synOrAsy(false)//同步true或异步false 压缩 默认同步
                    //.queryMaxFileSize(10)// 只查多少M以内的图片、视频、音频  单位M
                    //.compressSavePath(getPath())//压缩图片保存地址
                    //.sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效 注：已废弃
                    //.glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度 注：已废弃
//                    .withAspectRatio(aspect_ratio_x, aspect_ratio_y)// 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
                    .hideBottomControls(true)// 是否显示uCrop工具栏，默认不显示
                    .isGif(false)// 是否显示gif图片
                    //.isWebp(false)// 是否显示webp图片,默认显示
                    //.isBmp(false)//是否显示bmp图片,默认显示
                    .freeStyleCropEnabled(true)// 裁剪框是否可拖拽
                    .circleDimmedLayer(false)// 是否圆形裁剪
                    //.setCropDimmedColor(ContextCompat.getColor(getContext(), R.color.app_color_white))// 设置裁剪背景色值
                    //.setCircleDimmedBorderColor(ContextCompat.getColor(getApplicationContext(), R.color.app_color_white))// 设置圆形裁剪边框色值
                    //.setCircleStrokeWidth(3)// 设置圆形裁剪边框粗细
                    .showCropFrame(false)// 是否显示裁剪矩形边框 圆形裁剪时建议设为false
                    .showCropGrid(false)// 是否显示裁剪矩形网格 圆形裁剪时建议设为false
                    .isOpenClickSound(false)// 是否开启点击声音
                    .selectionData(mAdapter.getList())// 是否传入已选图片
                    //.isDragFrame(false)// 是否可拖动裁剪框(固定)
                    //.videoMinSecond(10)// 查询多少秒以内的视频
                    //.videoMaxSecond(15)// 查询多少秒以内的视频
                    .recordVideoSecond(15)//录制视频秒数 默认60s
                    .videoQuality(1)// 视频录制质量 0 or 1
                    //.isPreviewEggs(true)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                    //.cropCompressQuality(90)// 注：已废弃 改用cutOutQuality()
                    .cutOutQuality(90)// 裁剪输出质量 默认100
                    //.cutCompressFormat(Bitmap.CompressFormat.PNG.name())//裁剪图片输出Format格式，默认JPEG
                    .minimumCompressSize(100)// 小于多少kb的图片不压缩
                    //.cropWH()// 裁剪宽高比，设置如果大于图片本身宽高则无效
                    //.cropImageWideHigh()// 裁剪宽高比，设置如果大于图片本身宽高则无效
                    //.rotateEnabled(false) // 裁剪是否可旋转图片
                    //.scaleEnabled(false)// 裁剪是否可放大缩小图片

                    //.forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
                    .setAddType(type)
                    .forResult(new MyResultCallback(mAdapter));

        }

        @Override
        public void onCancelUploadVideo(String filePath) {

        }

        @Override
        public void onRetryUploadVideo(LocalMedia media) {

        }
    };

    /**
     * 创建自定义拍照输出目录
     */
    private String createCustomCameraOutPath() {
        File customFile;
        if (SdkVersionUtils.checkedAndroid_Q()) {
            // 在Android Q上不能直接使用外部存储目录；且沙盒内的资源是无法通过PictureSelector扫描出来的
            File externalFilesDir = getContext().getExternalFilesDir(chooseMode == PictureMimeType.ofVideo() ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES);
            customFile = new File(externalFilesDir.getAbsolutePath(), "PictureSelector");
            if (!customFile.exists()) {
                customFile.mkdirs();
            }
        } else {
            File rootFile = Environment.getExternalStorageDirectory();
            customFile = new File(rootFile.getAbsolutePath() + File.separator + "CustomPictureCamera");
            if (!customFile.exists()) {
                customFile.mkdirs();
            }
        }
        return customFile.getAbsolutePath() + File.separator;
    }

    /**
     * 返回结果回调
     */
    private static class MyResultCallback implements OnResultCallbackListener<LocalMedia> {
        private final WeakReference<PicturePreviewGridAdapter> mAdapterWeakReference;

        public MyResultCallback(PicturePreviewGridAdapter adapter) {
            super();
            this.mAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void onResult(List<LocalMedia> selectList) {
            for (LocalMedia media : selectList) {
                if (media.getWidth() == 0 || media.getHeight() == 0) {
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(media.getPath());
                        media.setWidth(imageExtraInfo.getWidth());
                        media.setHeight(imageExtraInfo.getHeight());
                    } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                        MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(PictureAppMaster.getInstance().getAppContext(), media.getPath());
                        media.setWidth(videoExtraInfo.getWidth());
                        media.setHeight(videoExtraInfo.getHeight());
                    }
                }

                //  可以通过PictureSelectorExternalUtils.getExifInterface();方法获取一些额外的资源信息，如旋转角度、经纬度等信息
            }
            Log.d(TAG, Collections.singletonList(selectList).toString());

            PicturePreviewGridAdapter adapter = mAdapterWeakReference.get();
            for (LocalMedia media : selectList) {
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    adapter.getList().add(0, media);
                    getCetificertAndUploadVideo(media);
                } else {
                    adapter.getList().add(media);

                    uploadImage(media);
                }
            }
            adapter.setList(adapter.getList());
            adapter.notifyDataSetChanged();

        }

        @Override
        public void onCancel() {
            Log.i(TAG, "PictureSelector Cancel");
        }
    }
     // 这里可以做上传图片的处理
    public static void uploadImage(LocalMedia media) {
        LogUtils.d(TAG, "uploadImage-getPath: " + media.getPath());
        LogUtils.d(TAG, "uploadImage-getRealPath: " + media.getRealPath());
        LogUtils.d(TAG, "uploadImage-getCompressPath: " + media.getCompressPath());
        String fPath = media.getCompressPath() == null ? media.getRealPath() : media.getCompressPath();
        File file = new File(fPath);
        LogUtils.d(TAG, "uploadImage-length: " + file.length() / 1024 + "k");
    }
    // 这里可以做上传视频的处理
    public static void getCetificertAndUploadVideo(LocalMedia media) {
        String fileName = media.getFileName();
        String realPath = media.getRealPath();
        String path = media.getPath();
        LogUtils.d(TAG, "uploadVideo-path: " + path);
        LogUtils.d(TAG, "uploadVideo-realPath: " + realPath);
        LogUtils.d(TAG, "uploadVideo-length: " + new File(realPath).length() / 1024 + "k");
    }

    /**
     * 自定义播放逻辑处理，用户可以自己实现播放界面
     */
    private static class MyVideoSelectedPlayCallback implements OnVideoSelectedPlayCallback<LocalMedia> {
        private final Context context;

        public MyVideoSelectedPlayCallback(Context context) {
            super();
            WeakReference<Context> mContextWeakReference = new WeakReference<>(context);
            this.context = mContextWeakReference.get();
        }

        @Override
        public void startPlayVideo(LocalMedia media) {
            if (context != null) {
                ToastUtils.s(context, media.getPath());
            }
        }
    }

    /**
     * 自定义权限管理回调
     */
    private static class MyPermissionsObtainCallback implements OnPermissionsObtainCallback {

        @Override
        public void onPermissionsIntercept(Context context, boolean isCamera, String[] permissions, String tips, OnPermissionDialogOptionCallback dialogOptionCallback) {
            PictureCustomDialog dialog = new PictureCustomDialog(context, R.layout.picture_wind_base_dialog);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
            Button btn_commit = dialog.findViewById(R.id.btn_commit);
            btn_commit.setText(context.getString(R.string.picture_go_setting));
            TextView tvTitle = dialog.findViewById(R.id.tvTitle);
            TextView tv_content = dialog.findViewById(R.id.tv_content);
            tvTitle.setText(context.getString(R.string.picture_prompt));
            tv_content.setText(tips);
            btn_cancel.setOnClickListener(v -> {
                dialog.dismiss();
                dialogOptionCallback.onCancel();
            });
            btn_commit.setOnClickListener(v -> {
                dialog.dismiss();
                dialogOptionCallback.onSetting();
                PermissionChecker.launchAppDetailsSettings(context);
            });
            dialog.show();
        }
    }

    /**
     * 自定义预览图片接口
     */
    private static class MyCustomPreviewInterfaceListener implements OnCustomImagePreviewCallback<LocalMedia> {

        @Override
        public void onCustomPreviewCallback(Context context, List<LocalMedia> previewData, int currentPosition) {
            // TODO context特指PictureSelectorActivity
            if (previewData != null && previewData.size() > 0) {
                LocalMedia currentLocalMedia = previewData.get(currentPosition);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE) {// 存储权限
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    PictureCacheManager.deleteCacheDirFile(getContext(), PictureMimeType.ofImage(), new OnCallbackListener<String>() {
                        @Override
                        public void onCall(String absolutePath) {
                            new PictureMediaScannerConnection(getContext(), absolutePath);
                            Log.i(TAG, "刷新图库:" + absolutePath);
                        }
                    });
                } else {
                    Toast.makeText(ServiceEvaluateActivity.this,
                            getString(R.string.picture_jurisdiction), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && mAdapter.getList() != null && mAdapter.getList().size() > 0) {
            outState.putParcelableArrayList("selectorList",
                    (ArrayList<? extends Parcelable>) mAdapter.getList());
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (BroadcastAction.ACTION_DELETE_PREVIEW_POSITION.equals(action)) {
                // 外部预览删除按钮回调
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    int position = extras.getInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION);
                    ToastUtils.s(getContext(), "delete image index:" + position);
                    mAdapter.remove(position);
                    mAdapter.notifyItemRemoved(position);
                }
            }
        }
    };

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        bundle = new Bundle(data.getExtras());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver, BroadcastAction.ACTION_DELETE_PREVIEW_POSITION);
        clearCache();
    }

    public Context getContext() {
        return this;
    }
}
