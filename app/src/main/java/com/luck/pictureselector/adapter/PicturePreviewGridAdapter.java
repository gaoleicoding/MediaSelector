package com.luck.pictureselector.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.selector.config.PictureMimeType;
import com.luck.picture.selector.entity.LocalMedia;
import com.luck.picture.selector.entity.UploadStatus;
import com.luck.picture.selector.listener.OnItemClickListener;
import com.luck.picture.selector.tools.ScreenUtils;
import com.luck.pictureselector.R;
import com.luck.pictureselector.listener.OnItemLongClickListener;
import com.luck.pictureselector.utils.LogUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class PicturePreviewGridAdapter extends RecyclerView.Adapter<PicturePreviewGridAdapter.ViewHolder> {
    public static final String TAG = "PicturePreviewGridAdapter";
    public static final int ITEM_TYPE_PREVIEW = 0;
    public static final int ITEM_TYPE_ADD_PICTURE = 1;
    public static final int ITEM_TYPE_ADD_VIDEO = 2;
    private final LayoutInflater mInflater;
    private List<LocalMedia> list = new ArrayList<>();
    private int selectMax = 6;
    private boolean hasSelectVideo;
    private final OnAddPicClickListener mOnAddPicClickListener;

    public interface OnAddPicClickListener {
        void onAddPicClick(int type);

        void onCancelUploadVideo(String filePath);

        void onRetryUploadVideo(LocalMedia media);
    }

    public List<LocalMedia> getList() {
        return list == null ? new ArrayList<>() : list;
    }

    public void delete(int position) {
        try {

            if (position != RecyclerView.NO_POSITION && list.size() > position) {
                list.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, list.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PicturePreviewGridAdapter(Context context, OnAddPicClickListener mOnAddPicClickListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mOnAddPicClickListener = mOnAddPicClickListener;
    }

    public void setSelectMax(int selectMax) {
        this.selectMax = selectMax;
    }

    public void setList(List<LocalMedia> list) {
        if (list.size() > 0) {
            LocalMedia media = list.get(0);
            hasSelectVideo = PictureMimeType.isHasVideo(media.getMimeType());
        } else {
            hasSelectVideo = false;
        }
        this.list = list;
    }

    public void remove(int position) {
        if (list != null && position < list.size()) {
            list.remove(position);
        }
    }

    @Override
    public int getItemCount() {

        int count;
        if (list.size() < selectMax) {
            if (hasSelectVideo) {
                count = list.size() + 1;
            } else {
                if (list.size() == selectMax - 1) {
                    count = list.size() + 1;
                } else {
                    count = list.size() + 2;
                }
            }
        } else {
            count = list.size();
        }
        LogUtils.d(TAG, "getItemCount: " + count);
        return count;
    }


    @Override
    public int getItemViewType(int position) {
        int type;
        if (isShowAddItem(position)) {
            type = ITEM_TYPE_ADD_PICTURE;
        } else if (isShowAddVideoItem(position)) {
            type = ITEM_TYPE_ADD_VIDEO;
        } else {
            type = ITEM_TYPE_PREVIEW;
        }
        LogUtils.d(TAG, "getItemViewType: " + type);
        return type;
    }

    /**
     * 创建ViewHolder
     */
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.evaluate_submit_pic_item, viewGroup, false);
        return new ViewHolder(view);
    }

    private boolean isShowAddItem(int position) {
        int size = list.size();
        boolean isMaxPic = (size == (selectMax - 1) && !hasSelectVideo);
        return position == size && !isMaxPic;
    }

    private boolean isShowAddVideoItem(int position) {
        int size = list.size();
        boolean isMaxPic = (size == (selectMax - 1) && !hasSelectVideo);
        if (isMaxPic) {
            return position == size;
        } else {
            return position == size + 1 && !hasSelectVideo;
        }
    }

    /**
     * 设置值
     */
    @Override
    public void onBindViewHolder(@NotNull final ViewHolder viewHolder, final int position) {
        //少于MaxSize张，显示继续添加的图标
        if (getItemViewType(position) == ITEM_TYPE_ADD_PICTURE) {
            viewHolder.mImg.setImageResource(R.drawable.evaluate_add_pic_bg);
            viewHolder.mIvType.setImageResource(R.drawable.evaluate_add_pic);
            viewHolder.mTvType.setText(R.string.evaluate_add_pic);
            viewHolder.mTvType.setVisibility(View.VISIBLE);
            viewHolder.itemView.setOnClickListener(v -> mOnAddPicClickListener.onAddPicClick(ITEM_TYPE_ADD_PICTURE));
            viewHolder.mIvDel.setVisibility(View.INVISIBLE);

        } else if (getItemViewType(position) == ITEM_TYPE_ADD_VIDEO) {
            viewHolder.mImg.setImageResource(R.drawable.evaluate_add_pic_bg);
            viewHolder.mIvType.setImageResource(R.drawable.evaluate_add_video);
            viewHolder.mTvType.setText(R.string.evaluate_add_video);
            viewHolder.mTvType.setVisibility(View.VISIBLE);
            viewHolder.itemView.setOnClickListener(v -> mOnAddPicClickListener.onAddPicClick(ITEM_TYPE_ADD_VIDEO));
            viewHolder.mIvDel.setVisibility(View.INVISIBLE);
        } else {
            LocalMedia media = list.get(position);
            viewHolder.mIvDel.setVisibility(View.VISIBLE);
            viewHolder.mTvType.setVisibility(View.GONE);
            viewHolder.mIvDel.setOnClickListener(view -> {
                // 如果正在上传，则不能删除
//                if (media.uploadStatus == UploadStatus.SENDING) {
//                    ToastUtils.show(view.getContext().getString(R.string.evaluate_uploading_no_del));
//                    return;
//                }

                list.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, list.size());
                setList(list);
                getItemCount();
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    hasSelectVideo = false;
                    mOnAddPicClickListener.onCancelUploadVideo(media.getFileName());
                }
            });

            viewHolder.mTvUploadFailure.setOnClickListener(view -> {
                viewHolder.mUploadProgress.setVisibility(View.VISIBLE);
                viewHolder.mTvUploadFailure.setVisibility(View.GONE);
                LocalMedia media2 = list.get(position);
                media2.uploadStatus = UploadStatus.SENDING;
                mOnAddPicClickListener.onRetryUploadVideo(media2);

            });

            int chooseModel = media.getChooseModel();
            String path;
            if (media.isCut() && !media.isCompressed()) {
                // 裁剪过
                path = media.getCutPath();
            } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
                // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                path = media.getCompressPath();
            } else {
                // 原图
                path = media.getPath();
            }

//            LogUtils.i(TAG, "原图地址::" + media.getPath());
//            LogUtils.i(TAG, "压缩q前文件大小::" + new File(media.getRealPath()).length() / 1024 + "k");
//            if (media.isCut()) {
//                LogUtils.i(TAG, "裁剪地址::" + media.getCutPath());
//            }
//            if (media.isCompressed()) {
//                LogUtils.i(TAG, "压缩地址::" + media.getCompressPath());
//                LogUtils.i(TAG, "压缩后文件大小::" + new File(media.getCompressPath()).length() / 1024 + "k");
//            }
//            if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
//                LogUtils.i(TAG, "Android Q特有地址::" + media.getAndroidQToPath());
//            }
//            if (media.isOriginal()) {
//                LogUtils.i(TAG, "是否开启原图功能::" + true);
//                LogUtils.i(TAG, "开启原图功能后地址::" + media.getOriginalPath());
//            }
            if (PictureMimeType.isHasImage(media.getMimeType())) {
                viewHolder.mIvType.setVisibility(View.GONE);
            } else {
                viewHolder.mIvType.setVisibility(View.VISIBLE);
                viewHolder.mIvType.setImageResource(R.drawable.evaluate_play_video);
            }
            if (media.uploadStatus == UploadStatus.SENDING) {
                viewHolder.mUploadProgress.setVisibility(View.VISIBLE);
                viewHolder.mTvUploadFailure.setVisibility(View.GONE);
            } else if (media.uploadStatus == UploadStatus.SEND_SUCCESS) {
                viewHolder.mUploadProgress.setVisibility(View.GONE);
                viewHolder.mTvUploadFailure.setVisibility(View.GONE);
            } else if (media.uploadStatus == UploadStatus.SEND_FAIL) {
                viewHolder.mUploadProgress.setVisibility(View.GONE);
                viewHolder.mTvUploadFailure.setVisibility(View.VISIBLE);
            }

            if (chooseModel == PictureMimeType.ofAudio()) {
                viewHolder.mImg.setImageResource(R.drawable.picture_audio_placeholder);
            } else {
                RequestOptions myOptions = new RequestOptions()
                        .transform(new CenterCrop(), new RoundedCorners(ScreenUtils.dip2px(viewHolder.itemView.getContext(), 5)));
                Glide.with(viewHolder.itemView.getContext())
                        .load(PictureMimeType.isContent(path) && !media.isCut() && !media.isCompressed() ? Uri.parse(path) : path)
                        .apply(myOptions)
                        .placeholder(R.drawable.evaluate_add_pic_bg)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(viewHolder.mImg);
            }
            //itemView 的点击事件
            if (mItemClickListener != null) {
                viewHolder.itemView.setOnClickListener(v -> {
                    int adapterPosition = viewHolder.getAbsoluteAdapterPosition();
                    mItemClickListener.onItemClick(v, adapterPosition);
                });
            }

            if (mItemLongClickListener != null) {
                viewHolder.itemView.setOnLongClickListener(v -> {
                    int adapterPosition = viewHolder.getAbsoluteAdapterPosition();
                    mItemLongClickListener.onItemLongClick(viewHolder, adapterPosition, v);
                    return true;
                });
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView mImg;
        ImageView mIvDel;
        ImageView mIvType;
        TextView mTvType;
        TextView mTvUploadFailure;
        ProgressBar mUploadProgress;

        public ViewHolder(View view) {
            super(view);
            mImg = view.findViewById(R.id.evaluate_iv_pic);
            mIvDel = view.findViewById(R.id.evaluate_iv_del);
            mIvType = view.findViewById(R.id.evaluate_iv_type);
            mTvType = view.findViewById(R.id.evaluate_tv_type);
            mTvUploadFailure = view.findViewById(R.id.evaluate_tv_upload_failure);
            mUploadProgress = view.findViewById(R.id.evaluate_upload_progress);
        }
    }

    private OnItemClickListener mItemClickListener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.mItemClickListener = l;
    }

    private OnItemLongClickListener mItemLongClickListener;

    public void setItemLongClickListener(OnItemLongClickListener l) {
        this.mItemLongClickListener = l;
    }
}
