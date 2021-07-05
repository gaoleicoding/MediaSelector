package com.luck.picture.selector.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.svideo.snap.crop.AliyunVideoCropActivity;
import com.aliyun.svideosdk.common.struct.common.AliyunSnapVideoParam;
import com.aliyun.svideosdk.common.struct.common.CropKey;
import com.aliyun.svideosdk.common.struct.common.VideoDisplayMode;
import com.aliyun.svideosdk.common.struct.common.VideoQuality;
import com.aliyun.svideosdk.common.struct.encoder.VideoCodecs;
import com.luck.picture.selector.PictureSelectorActivity;
import com.luck.picture.selector.R;
import com.luck.picture.selector.config.PictureConfig;
import com.luck.picture.selector.config.PictureMimeType;
import com.luck.picture.selector.config.PictureSelectionConfig;
import com.luck.picture.selector.dialog.PictureCustomDialog;
import com.luck.picture.selector.entity.LocalMedia;
import com.luck.picture.selector.listener.OnPhotoSelectChangedListener;
import com.luck.picture.selector.tools.AnimUtils;
import com.luck.picture.selector.tools.AttrsUtils;
import com.luck.picture.selector.tools.DateUtils;
import com.luck.picture.selector.tools.MediaUtils;
import com.luck.picture.selector.tools.StringUtils;
import com.luck.picture.selector.tools.ToastUtils;
import com.luck.picture.selector.tools.ValueOf;
import com.luck.picture.selector.tools.VoiceUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.aliyun.svideo.snap.crop.AliyunVideoCropActivity.VIDEO_CROP_REQUEST_CODE;

;


/**
 * @author：luck
 * @date：2016-12-30 12:02
 * @describe：PictureImageGridAdapter
 */
public class PictureImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final PictureSelectorActivity context;
    private boolean showCamera;
    private OnPhotoSelectChangedListener<LocalMedia> imageSelectChangedListener;
    private List<LocalMedia> datas = new ArrayList<>();
    private final List<LocalMedia> selectData = new ArrayList<>();
    private final PictureSelectionConfig config;
    public final int maxVideoDuration = 3 * 60 * 1000;
    public final int maxCropDuration = 16000;
    private int formerSelectVideoSize = 0;
    private int formerSelectPicSize = 0;

    public PictureImageGridAdapter(PictureSelectorActivity context, PictureSelectionConfig config) {
        this.context = context;
        this.config = config;
        this.showCamera = config.isCamera;
    }

    public void setShowCamera(boolean showCamera) {
        this.showCamera = showCamera;
    }

    public boolean isShowCamera() {
        return showCamera;
    }

    /**
     * 全量刷新
     *
     * @param data
     */
    public void bindData(List<LocalMedia> data) {
        this.datas = data == null ? new ArrayList<>() : data;
        this.notifyDataSetChanged();
    }

    public void bindSelectData(List<LocalMedia> images) {
        // 这里重新构构造一个新集合，不然会产生已选集合一变，结果集合也会添加的问题
        List<LocalMedia> selection = new ArrayList<>();
        int size = images.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = images.get(i);
            selection.add(media);
        }

        for (LocalMedia media : selection) {
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                formerSelectVideoSize++;
            } else {
                formerSelectPicSize++;
            }
        }
        if (!config.isSingleDirectReturn) {
            subSelectPosition();
            if (imageSelectChangedListener != null) {
                imageSelectChangedListener.onChange(selectData);
            }
        }
    }

    public List<LocalMedia> getSelectedData() {
        return selectData == null ? new ArrayList<>() : selectData;
    }

    public int getSelectedSize() {
        return selectData == null ? 0 : selectData.size();
    }

    public List<LocalMedia> getDatas() {
        return datas == null ? new ArrayList<>() : datas;
    }

    public boolean isDataEmpty() {
        return datas == null || datas.size() == 0;
    }

    public void clear() {
        if (getSize() > 0) {
            datas.clear();
        }
    }

    public int getSize() {
        return datas == null ? 0 : datas.size();
    }

    public LocalMedia getItem(int position) {
        return getSize() > 0 ? datas.get(position) : null;
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera && position == 0) {
            return PictureConfig.TYPE_CAMERA;
        } else {
            return PictureConfig.TYPE_PICTURE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == PictureConfig.TYPE_CAMERA) {
            View view = LayoutInflater.from(context).inflate(R.layout.picture_item_camera, parent, false);
            return new CameraViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.picture_image_grid_item, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == PictureConfig.TYPE_CAMERA) {
            CameraViewHolder headerHolder = (CameraViewHolder) holder;
            headerHolder.itemView.setOnClickListener(v -> {
                int videoSize = formerSelectVideoSize;
                int picSize = formerSelectPicSize;
                int selectCount = selectData.size();
                for (int i = 0; i < selectCount; i++) {
                    LocalMedia media = selectData.get(i);
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        picSize++;
                    } else {
                        videoSize++;
                    }
                }
                if (config.chooseMode == PictureMimeType.ofImage() && picSize == config.maxSelectNum - config.maxVideoSelectNum) {
                    showPromptDialog(context.getString(R.string.picture_message_max_num, config.maxSelectNum - config.maxVideoSelectNum - formerSelectPicSize + ""));
                    return;
                }
                if (config.chooseMode == PictureMimeType.ofVideo() && videoSize == config.maxVideoSelectNum) {
                    showPromptDialog(context.getString(R.string.picture_message_video_max_num, config.maxVideoSelectNum + ""));
                    return;
                }
//                PictureSelectionConfig selectionConfig = PictureSelectionConfig.getCleanInstance();
//                LocalMedia media = new LocalMedia();
//                if (selectionConfig.mAddFileType == PictureMimeType.ofImage()) {
//                    media.setMimeType(PictureMimeType.getMimeType(PictureConfig.TYPE_IMAGE));
//                } else if (selectionConfig.mAddFileType == PictureMimeType.ofVideo()) {
//                    media.setMimeType(PictureMimeType.getMimeType(PictureConfig.TYPE_VIDEO));
//                }
//                if (judgeNoMatch(media, false)) return;
                if (imageSelectChangedListener != null) {
                    imageSelectChangedListener.onTakePhoto();
                }
            });
        } else {
            final ViewHolder contentHolder = (ViewHolder) holder;
            final LocalMedia image = datas.get(showCamera ? position - 1 : position);
            if (image.isSelected()) {
                boolean isChecked = contentHolder.tvCheck.isSelected();
                selectImage(contentHolder, !isChecked);
            }
            image.position = contentHolder.getAbsoluteAdapterPosition();
            final String path = image.getPath();
            final String mimeType = image.getMimeType();
            if (config.checkNumMode) {
                notifyCheckChanged(contentHolder, image);
            }
            if (config.isSingleDirectReturn) {
                contentHolder.tvCheck.setVisibility(View.GONE);
                contentHolder.btnCheck.setVisibility(View.GONE);
            } else {
                selectImage(contentHolder, isSelected(image));
                contentHolder.tvCheck.setVisibility(View.VISIBLE);
                contentHolder.btnCheck.setVisibility(View.VISIBLE);
                // 启用了蒙层效果
                if (config.isMaxSelectEnabledMask) {
                    dispatchHandleMask(contentHolder, image);
                }
            }
            contentHolder.tvIsGif.setVisibility(PictureMimeType.isGif(mimeType) ? View.VISIBLE : View.GONE);
            if (PictureMimeType.isHasImage(image.getMimeType())) {
                if (image.loadLongImageStatus == PictureConfig.NORMAL) {
                    image.isLongImage = MediaUtils.isLongImg(image);
                    image.loadLongImageStatus = PictureConfig.LOADED;
                }
                contentHolder.tvLongChart.setVisibility(image.isLongImage ? View.VISIBLE : View.GONE);
            } else {
                image.loadLongImageStatus = PictureConfig.NORMAL;
                contentHolder.tvLongChart.setVisibility(View.GONE);
            }
            boolean isHasVideo = PictureMimeType.isHasVideo(mimeType);
            if (isHasVideo || PictureMimeType.isHasAudio(mimeType)) {
                contentHolder.tvDuration.setVisibility(View.VISIBLE);
                contentHolder.tvDuration.setText(DateUtils.formatDurationTime(image.getDuration()));
                if (PictureSelectionConfig.uiStyle != null) {
                    if (isHasVideo) {
                        if (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable != 0) {
                            contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                    (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable,
                                            0, 0, 0);
                        } else {
                            contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                    (R.drawable.picture_icon_video, 0, 0, 0);
                        }
                    } else {
                        if (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable != 0) {
                            contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                    (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable,
                                            0, 0, 0);
                        } else {
                            contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                    (R.drawable.picture_icon_audio, 0, 0, 0);
                        }
                    }
                } else {
                    contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (isHasVideo ? R.drawable.picture_icon_video : R.drawable.picture_icon_audio,
                                    0, 0, 0);
                }
            } else {
                contentHolder.tvDuration.setVisibility(View.GONE);
            }
            if (config.chooseMode == PictureMimeType.ofAudio()) {
                contentHolder.ivPicture.setImageResource(R.drawable.picture_audio_placeholder);
            } else {
                if (PictureSelectionConfig.imageEngine != null) {
                    PictureSelectionConfig.imageEngine.loadGridImage(context, path, contentHolder.ivPicture);
                }
            }

            if (config.enablePreview || config.enPreviewVideo || config.enablePreviewAudio) {
                contentHolder.btnCheck.setOnClickListener(v -> {
                    if (config.isMaxSelectEnabledMask) {
                        if (config.isWithVideoImage) {
                            int selectedCount = getSelectedSize();
                            int videoSize = 0;
                            for (int i = 0; i < selectedCount; i++) {
                                LocalMedia media = selectData.get(i);
                                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                    videoSize++;
                                }
                            }
                            String errorMsg;
                            boolean isNotOption;
                            if (PictureMimeType.isHasVideo(image.getMimeType())) {
                                isNotOption = !contentHolder.tvCheck.isSelected() && videoSize >= config.maxVideoSelectNum;
                                errorMsg = StringUtils.getMsg(context, image.getMimeType(), config.maxVideoSelectNum);
                            } else {
                                isNotOption = !contentHolder.tvCheck.isSelected() && selectedCount >= config.maxSelectNum;
                                errorMsg = StringUtils.getMsg(context, image.getMimeType(), config.maxSelectNum);
                            }
                            if (isNotOption) {
                                showPromptDialog(errorMsg);
                                return;
                            }
                        } else {
                            if (!contentHolder.tvCheck.isSelected() && getSelectedSize() + formerSelectVideoSize >= config.maxSelectNum) {
                                String msg = StringUtils.getMsg(context, image.getMimeType(), config.maxSelectNum);
                                showPromptDialog(msg);
                                return;
                            }
                        }
                    }
                    // If the original path does not exist or the path does exist but the file does not exist
                    String newPath = image.getRealPath();
                    if (!TextUtils.isEmpty(newPath) && !new File(newPath).exists()) {
                        ToastUtils.s(context, PictureMimeType.s(context, mimeType));
                        return;
                    }
                    changeCheckboxState(contentHolder, image);
                });
            }
            contentHolder.contentView.setOnClickListener(v -> {
                if (config.isMaxSelectEnabledMask) {
                    if (image.isMaxSelectEnabledMask()) {
                        return;
                    }
                }
                // If the original path does not exist or the path does exist but the file does not exist
                String newPath = image.getRealPath();
                if (!TextUtils.isEmpty(newPath) && !new File(newPath).exists()) {
                    ToastUtils.s(context, PictureMimeType.s(context, mimeType));
                    return;
                }
                int index = showCamera ? position - 1 : position;
                if (index == -1) {
                    return;
                }
                boolean eqResult =
                        PictureMimeType.isHasImage(mimeType) && config.enablePreview
                                || config.isSingleDirectReturn
                                || PictureMimeType.isHasVideo(mimeType) && (config.enPreviewVideo
                                || config.selectionMode == PictureConfig.SINGLE)
                                || PictureMimeType.isHasAudio(mimeType) && (config.enablePreviewAudio
                                || config.selectionMode == PictureConfig.SINGLE);
                if (eqResult) {
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        if (config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                            // The video is less than the minimum specified length
                            showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000));
                            return;
                        }
                        if (config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                            // The length of the video exceeds the specified length
                            showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000));
                            return;
                        }
                    }
                    imageSelectChangedListener.onPictureClick(image, index);
                } else {
                    changeCheckboxState(contentHolder, image);
                }
            });
        }
    }

    /**
     * Handle mask effects
     *
     * @param contentHolder
     * @param item
     */
    private void dispatchHandleMask(ViewHolder contentHolder, LocalMedia item) {
        if (config.isWithVideoImage && config.maxVideoSelectNum > 0) {
            if (config.chooseMode == PictureMimeType.ofImage()) {
                if (getSelectedSize() + formerSelectPicSize >= config.maxSelectNum - config.maxVideoSelectNum) {
                    boolean isSelected = contentHolder.tvCheck.isSelected();
                    ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(isSelected ?
                                    ContextCompat.getColor(context, R.color.picture_color_80) :
                                    ContextCompat.getColor(context, R.color.picture_color_half_white),
                            BlendModeCompat.SRC_ATOP);
                    contentHolder.ivPicture.setColorFilter(colorFilter);
                    item.setMaxSelectEnabledMask(!isSelected);
                }
            } else {
                if (getSelectedSize() + formerSelectVideoSize >= config.maxVideoSelectNum) {
                    boolean isSelected = contentHolder.tvCheck.isSelected();
                    ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(isSelected ?
                                    ContextCompat.getColor(context, R.color.picture_color_80) :
                                    ContextCompat.getColor(context, R.color.picture_color_half_white),
                            BlendModeCompat.SRC_ATOP);
                    contentHolder.ivPicture.setColorFilter(colorFilter);
                    item.setMaxSelectEnabledMask(!isSelected);
                }
            }
        } else {
            LocalMedia media = selectData.size() > 0 ? selectData.get(0) : null;
            if (media != null) {
                boolean isSelected = contentHolder.tvCheck.isSelected();
                if (config.chooseMode == PictureMimeType.ofAll()) {
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        // All videos are not optional
                        if (!isSelected && !PictureMimeType.isHasImage(item.getMimeType())) {
                            ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ContextCompat.getColor
                                    (context, PictureMimeType.isHasVideo(item.getMimeType()) ? R.color.picture_color_half_white : R.color.picture_color_20), BlendModeCompat.SRC_ATOP);
                            contentHolder.ivPicture.setColorFilter(colorFilter);
                        }
                        item.setMaxSelectEnabledMask(PictureMimeType.isHasVideo(item.getMimeType()));
                    } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                        // All images are not optional
                        if (!isSelected && !PictureMimeType.isHasVideo(item.getMimeType())) {
                            ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ContextCompat.getColor
                                    (context, PictureMimeType.isHasImage(item.getMimeType()) ? R.color.picture_color_half_white : R.color.picture_color_20), BlendModeCompat.SRC_ATOP);
                            contentHolder.ivPicture.setColorFilter(colorFilter);
                        }
                        item.setMaxSelectEnabledMask(PictureMimeType.isHasImage(item.getMimeType()));
                    }
                } else {
                    if (config.chooseMode == PictureMimeType.ofVideo() && config.maxVideoSelectNum > 0) {
                        if (!isSelected && getSelectedSize() + formerSelectVideoSize == config.maxVideoSelectNum) {
                            ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ContextCompat.getColor
                                    (context, R.color.picture_color_half_white), BlendModeCompat.SRC_ATOP);
                            contentHolder.ivPicture.setColorFilter(colorFilter);
                        }
                        item.setMaxSelectEnabledMask(!isSelected && getSelectedSize() + formerSelectVideoSize == config.maxVideoSelectNum);
                    } else {
                        if (!isSelected && getSelectedSize() + formerSelectVideoSize == config.maxSelectNum) {
                            ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ContextCompat.getColor
                                    (context, R.color.picture_color_half_white), BlendModeCompat.SRC_ATOP);
                            contentHolder.ivPicture.setColorFilter(colorFilter);
                        }
                        item.setMaxSelectEnabledMask(!isSelected && getSelectedSize() + formerSelectVideoSize == config.maxSelectNum);
                    }
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return showCamera ? datas.size() + 1 : datas.size();
    }

    public class CameraViewHolder extends RecyclerView.ViewHolder {
        TextView tvCamera;

        public CameraViewHolder(View itemView) {
            super(itemView);
            tvCamera = itemView.findViewById(R.id.tvCamera);
            if (PictureSelectionConfig.uiStyle != null) {
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_camera_backgroundColor != 0) {
                    itemView.setBackgroundColor(PictureSelectionConfig.uiStyle.picture_adapter_item_camera_backgroundColor);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textSize != 0) {
                    tvCamera.setTextSize(PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textSize);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textColor != 0) {
                    tvCamera.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textColor);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_camera_text != 0) {
                    tvCamera.setText(itemView.getContext().getString(PictureSelectionConfig.uiStyle.picture_adapter_item_camera_text));
                } else {
                    tvCamera.setText(config.chooseMode == PictureMimeType.ofAudio() ? context.getString(R.string.picture_tape)
                            : context.getString(R.string.picture_take_picture));
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textTopDrawable != 0) {
                    tvCamera.setCompoundDrawablesWithIntrinsicBounds(0, PictureSelectionConfig.uiStyle.picture_adapter_item_camera_textTopDrawable, 0, 0);
                }
            } else {
                tvCamera.setText(config.chooseMode == PictureMimeType.ofAudio() ? context.getString(R.string.picture_tape)
                        : context.getString(R.string.picture_take_picture));
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPicture;
        TextView tvCheck;
        TextView tvDuration, tvIsGif, tvLongChart;
        View contentView;
        View btnCheck;

        public ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            ivPicture = itemView.findViewById(R.id.ivPicture);
            tvCheck = itemView.findViewById(R.id.tvCheck);
            btnCheck = itemView.findViewById(R.id.btnCheck);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvIsGif = itemView.findViewById(R.id.tv_isGif);
            tvLongChart = itemView.findViewById(R.id.tv_long_chart);
            if (PictureSelectionConfig.uiStyle != null) {
                if (PictureSelectionConfig.uiStyle.picture_check_style != 0) {
                    tvCheck.setBackgroundResource(PictureSelectionConfig.uiStyle.picture_check_style);
                }
                if (PictureSelectionConfig.uiStyle.picture_check_textSize != 0) {
                    tvCheck.setTextSize(PictureSelectionConfig.uiStyle.picture_check_textSize);
                }
                if (PictureSelectionConfig.uiStyle.picture_check_textColor != 0) {
                    tvCheck.setTextColor(PictureSelectionConfig.uiStyle.picture_check_textColor);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textSize > 0) {
                    tvDuration.setTextSize(PictureSelectionConfig.uiStyle.picture_adapter_item_textSize);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textColor != 0) {
                    tvDuration.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_textColor);
                }

                if (PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text != 0) {
                    tvIsGif.setText(itemView.getContext().getString(PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text));
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_show) {
                    tvIsGif.setVisibility(View.VISIBLE);
                } else {
                    tvIsGif.setVisibility(View.GONE);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background != 0) {
                    tvIsGif.setBackgroundResource(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor != 0) {
                    tvIsGif.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize != 0) {
                    tvIsGif.setTextSize(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize);
                }
            } else if (PictureSelectionConfig.style != null) {
                if (PictureSelectionConfig.style.pictureCheckedStyle != 0) {
                    tvCheck.setBackgroundResource(PictureSelectionConfig.style.pictureCheckedStyle);
                }
            } else {
                Drawable checkedStyleDrawable = AttrsUtils.getTypeValueDrawable(itemView.getContext(), R.attr.picture_checked_style, R.drawable.picture_checkbox_selector);
                tvCheck.setBackground(checkedStyleDrawable);
            }
        }
    }

    public boolean isSelected(LocalMedia image) {
        int size = selectData.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = selectData.get(i);
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                continue;
            }
            if (media.getPath()
                    .equals(image.getPath())
                    || media.getId() == image.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update button status
     */
    private void notifyCheckChanged(ViewHolder viewHolder, LocalMedia imageBean) {
        viewHolder.tvCheck.setText("");
        int size = selectData.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = selectData.get(i);
            if (media.getPath().equals(imageBean.getPath())
                    || media.getId() == imageBean.getId()) {
                imageBean.setNum(media.getNum());
                media.setPosition(imageBean.getPosition());
                viewHolder.tvCheck.setText(ValueOf.toString(imageBean.getNum()));
            }
        }
    }


    /**
     * Update the selected status of the image
     *
     * @param contentHolder
     * @param media
     */

    @SuppressLint("StringFormatMatches")
    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia media) {

        checkSelect(contentHolder, media);
    }

    private void checkSelect(ViewHolder contentHolder, LocalMedia localMedia) {
        boolean isChecked = contentHolder.tvCheck.isSelected();

        if (judgeNoMatch(localMedia, isChecked)) return;

        if (isChecked) {
            int selectCount = selectData.size();
            for (int i = 0; i < selectCount; i++) {
                LocalMedia media = selectData.get(i);
                if (media == null || TextUtils.isEmpty(media.getPath())) {
                    continue;
                }
                if (media.getPath().equals(localMedia.getPath())
                        || media.getId() == localMedia.getId()) {
                    selectData.remove(media);
                    subSelectPosition();
                    AnimUtils.disZoom(contentHolder.ivPicture, config.zoomAnim);
                    break;
                }
            }
        } else {
            // The radio
            if (config.selectionMode == PictureConfig.SINGLE) {
                singleRadioMediaImage();
            }
            if (PictureMimeType.isHasVideo(localMedia.getMimeType())) {
                selectData.add(0, localMedia);
            } else {
                selectData.add(localMedia);
            }
            VoiceUtils.getInstance().play();
            AnimUtils.zoom(contentHolder.ivPicture, config.zoomAnim);
            contentHolder.tvCheck.startAnimation(AnimationUtils.loadAnimation(context, R.anim.picture_anim_modal_in));
        }

        boolean isRefreshAll = true;
        if (config.isMaxSelectEnabledMask) {

            if (config.chooseMode == PictureMimeType.ofVideo() && config.maxVideoSelectNum > 0) {
                if (!isChecked && getSelectedSize() + formerSelectVideoSize >= config.maxVideoSelectNum) {
                    // add
                    isRefreshAll = true;
                }

            } else {
                if (!isChecked && getSelectedSize() + formerSelectPicSize >= config.maxSelectNum - config.maxVideoSelectNum) {
                    // add
                    isRefreshAll = true;
                }

            }
//            if (config.chooseMode == PictureMimeType.ofAll()) {
//                // ofAll
//                if (config.isWithVideoImage && config.maxVideoSelectNum > 0) {
//                    if (getSelectedSize() + formerSelectVideoSize >= config.maxSelectNum) {
//                        isRefreshAll = true;
//                    }
//                    if (isChecked) {
//                        // delete
//                        if (getSelectedSize() + formerSelectVideoSize == config.maxSelectNum - 1) {
//                            isRefreshAll = true;
//                        }
//                    }
//                } else {
//                    if (!isChecked && getSelectedSize() + formerSelectVideoSize == 1) {
//                        // add
//                        isRefreshAll = true;
//                    }
//                    if (isChecked && getSelectedSize() + formerSelectVideoSize == 0) {
//                        // delete
//                        isRefreshAll = true;
//                    }
//                }
//            } else {
//                // ofImage or ofVideo or ofAudio
//                if (config.chooseMode == PictureMimeType.ofVideo() && config.maxVideoSelectNum > 0) {
//                    if (!isChecked && getSelectedSize() + formerSelectVideoSize == config.maxVideoSelectNum) {
//                        // add
//                        isRefreshAll = true;
//                    }
//                    if (isChecked && getSelectedSize() + formerSelectVideoSize == config.maxVideoSelectNum - 1) {
//                        // delete
//                        isRefreshAll = true;
//                    }
//                } else {
//                    if (!isChecked && getSelectedSize() + formerSelectPicSize == config.maxSelectNum - config.maxVideoSelectNum) {
//                        // add
//                        isRefreshAll = true;
//                    }
//                    if (isChecked && getSelectedSize() + formerSelectPicSize == config.maxSelectNum - 1) {
//                        // delete
//                        isRefreshAll = true;
//                    }
//                }
//            }
        }
        selectImage(contentHolder, !isChecked);
        if (isRefreshAll) {
            notifyDataSetChanged();
        } else {
            notifyItemChanged(contentHolder.getAdapterPosition());
        }

        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectData);
        }
    }

    private boolean judgeNoMatch(LocalMedia localMedia, boolean isChecked) {
        int selectCount = selectData.size();
        int videoSize = formerSelectVideoSize;
        int picSize = formerSelectPicSize;
        for (int i = 0; i < selectCount; i++) {
            LocalMedia media = selectData.get(i);
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                videoSize++;
            } else {
                picSize++;
            }
        }

        if (PictureMimeType.isHasVideo(localMedia.getMimeType())) {
            // isWithVideoImage mode

            long duration = localMedia.getDuration();
            if (duration > maxVideoDuration) {
                ToastUtils.s(context, context.getString(R.string.alivc_crop_video_over_length));
                return true;
            }

            if (videoSize >= config.maxVideoSelectNum && !isChecked) {
                showPromptDialog(StringUtils.getMsg(context, localMedia.getMimeType(), config.maxVideoSelectNum));
                return true;
            }

            if (!isChecked && config.videoMinSecond > 0 && localMedia.getDuration() < config.videoMinSecond) {
                showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000));
                return true;
            }

            if (!isChecked && config.videoMaxSecond > 0 && localMedia.getDuration() > config.videoMaxSecond) {
                showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000));
                return true;
            }

            if (PictureMimeType.isHasVideo(localMedia.getMimeType()) && !isChecked) {
                // 如果选择的视频长度大于15s，去裁剪
                if (localMedia.getDuration() >= maxCropDuration) {
                    context.selectCropVideo(localMedia);
                    startVideoCrop(localMedia.getRealPath());
                    return true;
                }
            }
        } else {
            if (picSize == config.maxSelectNum - config.maxVideoSelectNum && !isChecked) {
                showPromptDialog(context.getString(R.string.picture_message_max_num, config.maxSelectNum - config.maxVideoSelectNum - formerSelectPicSize + ""));
                return true;
            }

        }
        return false;
    }

    private void startVideoCrop(String mediaPath) {
        Intent intent = new Intent(context, AliyunVideoCropActivity.class);
        intent.putExtra(CropKey.VIDEO_PATH, mediaPath);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RESOLUTION, AliyunSnapVideoParam.RESOLUTION_480P);
        intent.putExtra(AliyunSnapVideoParam.CROP_MODE, VideoDisplayMode.FILL);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_QUALITY, VideoQuality.HD);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_GOP, 5);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_FRAMERATE, 25);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RATIO, CropKey.RATIO_MODE_9_16);
        intent.putExtra(AliyunSnapVideoParam.MIN_CROP_DURATION, 3000);
        intent.putExtra(CropKey.ACTION, CropKey.ACTION_TRANSCODE);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_CODEC, VideoCodecs.H264_HARDWARE);
        intent.putExtra(AliyunSnapVideoParam.CROP_USE_GPU, false);
        context.startActivityForResult(intent, VIDEO_CROP_REQUEST_CODE);
    }

    /**
     * Radio mode
     */
    private void singleRadioMediaImage() {
        if (selectData != null
                && selectData.size() > 0) {
            LocalMedia media = selectData.get(0);
            notifyItemChanged(media.position);
            selectData.clear();
        }
    }

    /**
     * Update the selection order
     */
    private void subSelectPosition() {
        if (config.checkNumMode) {
            int size = selectData.size();
            for (int index = 0; index < size; index++) {
                LocalMedia media = selectData.get(index);
                media.setNum(index + 1);
                notifyItemChanged(media.position);
            }
        }
    }

    /**
     * Select the image and animate it
     *
     * @param holder
     * @param isChecked
     */
    public void selectImage(ViewHolder holder, boolean isChecked) {
        holder.tvCheck.setSelected(isChecked);
        ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(isChecked ?
                        ContextCompat.getColor(context, R.color.picture_color_80) :
                        ContextCompat.getColor(context, R.color.picture_color_20),
                BlendModeCompat.SRC_ATOP);
        holder.ivPicture.setColorFilter(colorFilter);
    }

    /**
     * Tips
     */
    private void showPromptDialog(String content) {
        if (PictureSelectionConfig.onChooseLimitCallback != null) {
            PictureSelectionConfig.onChooseLimitCallback.onChooseLimit(context, content);
        } else {
            PictureCustomDialog dialog = new PictureCustomDialog(context, R.layout.picture_prompt_dialog);
            TextView btnOk = dialog.findViewById(R.id.btnOk);
            TextView tvContent = dialog.findViewById(R.id.tv_content);
            tvContent.setText(content);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }


    /**
     * Binding listener
     *
     * @param imageSelectChangedListener
     */
    public void setOnPhotoSelectChangedListener(OnPhotoSelectChangedListener
                                                        imageSelectChangedListener) {
        this.imageSelectChangedListener = imageSelectChangedListener;
    }
}
