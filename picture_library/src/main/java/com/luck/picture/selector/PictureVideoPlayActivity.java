package com.luck.picture.selector;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.luck.picture.selector.config.PictureConfig;
import com.luck.picture.selector.config.PictureMimeType;
import com.luck.picture.selector.config.PictureSelectionConfig;
import com.luck.picture.selector.entity.LocalMedia;
import com.luck.picture.selector.tools.SdkVersionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PictureVideoPlayActivity extends PictureBaseActivity implements
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, View.OnClickListener {
    private String videoPath;
    private ImageView ibLeftBack;
    private VideoView mVideoView;
    private ImageView mIvPlay;
    private TextView mTvCountDown;
    private LinearLayout mPlayView;
    private int mPositionWhenPaused = -1;
    private SeekBar seekBar;
    int duration = 0;
    int countDownTime = 3;
    final int PLAY_MSG = 2;
    final int COUNTDOWN_MSG = 3;
    final int VIDEO_PLAY = 4;
    final int VIDEO_REPLAY = 5;
    int UPGRADE_SEEKBAR_INTERVAL = 1000;
    int UPGRADE_REPLAY_COUNTDOWN = 1000;
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case PLAY_MSG:
                    // 获得当前播放时间和当前视频的长度
                    int currentPosition = mVideoView.getCurrentPosition();
                    if (duration > 0) {
                        // 设置进度条的主要进度，表示当前的播放时间
                        seekBar.setProgress(currentPosition);
                        handler.sendEmptyMessageDelayed(PLAY_MSG, UPGRADE_SEEKBAR_INTERVAL);
                    }
                    break;

                case COUNTDOWN_MSG:
                    if (countDownTime == 0) {
                        playVideo();
                        removeMessages(COUNTDOWN_MSG);
                        countDownTime = 3;
                        mPlayView.setVisibility(View.GONE);
                    }
                    mTvCountDown.setText(getString(R.string.video_replay_countdown, countDownTime));
                    countDownTime--;
                    handler.sendEmptyMessageDelayed(COUNTDOWN_MSG, UPGRADE_REPLAY_COUNTDOWN);
                    break;
            }
        }
    };

    @Override
    public boolean isImmersive() {
        return false;
    }

    @Override
    public boolean isRequestedOrientation() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
        seekBar = findViewById(R.id.seek_bar);

        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

    }

    @Override
    public int getResourceId() {
        return R.layout.picture_activity_video_play;
    }

    @Override
    protected void initPictureSelectorStyle() {
        if (PictureSelectionConfig.style != null) {
//            if (PictureSelectionConfig.style.pictureLeftBackIcon != 0) {
//                ibLeftBack.setImageResource(PictureSelectionConfig.style.pictureLeftBackIcon);
//            }
        }
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        videoPath = getIntent().getStringExtra(PictureConfig.EXTRA_VIDEO_PATH);

        if (TextUtils.isEmpty(videoPath)) {
            LocalMedia media = getIntent().getParcelableExtra(PictureConfig.EXTRA_MEDIA_KEY);
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                finish();
                return;
            }
            videoPath = media.getPath();
        }
        if (TextUtils.isEmpty(videoPath)) {
            exit();
            return;
        }
        ibLeftBack = findViewById(R.id.pictureLeftBack);
        mIvPlay = findViewById(R.id.iv_play);
        mVideoView = findViewById(R.id.video_view);
        mTvCountDown = findViewById(R.id.tv_countdown);
        mPlayView = findViewById(R.id.ll_play_video);
        MediaController mediaController = new MediaController(this);
        mediaController.setVisibility(View.GONE);        //隐藏进度条
        mVideoView.setMediaController(mediaController);
        mVideoView.setOnClickListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(this);
        ibLeftBack.setOnClickListener(this);
        mIvPlay.setOnClickListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();
        // Play Video
        if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(videoPath)) {
            mVideoView.setVideoURI(Uri.parse(videoPath));
        } else {
            mVideoView.setVideoPath(videoPath);
        }
        mVideoView.start();

    }

    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            //判断是否是由用户拖拽发生的变化
            if (fromUser) {
                handler.removeMessages(PLAY_MSG);
                handler.removeMessages(COUNTDOWN_MSG);
                // 设置当前播放的位置
                mVideoView.seekTo(progress);
                handler.sendEmptyMessageDelayed(PLAY_MSG, UPGRADE_SEEKBAR_INTERVAL);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        mPositionWhenPaused = mVideoView.getCurrentPosition();
        mVideoView.stopPlayback();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPositionWhenPaused >= 0) {
            mVideoView.seekTo(mPositionWhenPaused);
            mPositionWhenPaused = -1;
        }
    }

    // 发生错误被回调
    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        return false;
    }

    // 播放完成被回调
    @Override
    public void onCompletion(MediaPlayer mp) {
        seekBar.setProgress(duration);
        pauseVideo(VIDEO_REPLAY);
    }

    // 准备完成被回调
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnInfoListener((mp1, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // video started
                mVideoView.setBackgroundColor(Color.TRANSPARENT);
                duration = mVideoView.getDuration();
                seekBar.setProgress(0);
                seekBar.setMax(duration);
                handler.sendEmptyMessage(PLAY_MSG);
                return true;
            }
            return false;
        });

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pictureLeftBack) {
            onBackPressed();
        } else if (id == R.id.iv_play) {
            playVideo();
        } else if (id == R.id.video_view) {
            pauseVideo(VIDEO_PLAY);
        } else if (id == R.id.tv_confirm) {
            List<LocalMedia> result = new ArrayList<>();
            result.add(getIntent().getParcelableExtra(PictureConfig.EXTRA_MEDIA_KEY));
            setResult(RESULT_OK, new Intent()
                    .putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                            (ArrayList<? extends Parcelable>) result));
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        if (PictureSelectionConfig.windowAnimationStyle != null
                && PictureSelectionConfig.windowAnimationStyle.activityPreviewExitAnimation != 0) {
            finish();
            overridePendingTransition(0, PictureSelectionConfig.windowAnimationStyle.activityPreviewExitAnimation);
        } else {
            exit();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new ContextWrapper(newBase) {
            @Override
            public Object getSystemService(String name) {
                if (Context.AUDIO_SERVICE.equals(name)) {
                    return getApplicationContext().getSystemService(name);
                }
                return super.getSystemService(name);
            }
        });
    }

    protected void playVideo() {
        mVideoView.start();
        mPlayView.setVisibility(View.GONE);
        handler.sendEmptyMessage(PLAY_MSG);
    }

    protected void pauseVideo(int type) {
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);
        mVideoView.requestFocus();
        mVideoView.setClickable(true);
        if (type == VIDEO_PLAY) {
            mIvPlay.setImageResource(R.drawable.picture_icon_video_play);
            mTvCountDown.setVisibility(View.GONE);
            handler.removeMessages(PLAY_MSG);
            handler.removeMessages(COUNTDOWN_MSG);
        }
        if (type == VIDEO_REPLAY) {
            mIvPlay.setImageResource(R.drawable.evaluate_replay_video);
            mTvCountDown.setVisibility(View.VISIBLE);
            handler.removeMessages(COUNTDOWN_MSG);
            handler.removeMessages(PLAY_MSG);
            handler.sendEmptyMessageDelayed(COUNTDOWN_MSG, UPGRADE_REPLAY_COUNTDOWN);
        }

    }

    protected String time(long millionSeconds) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millionSeconds);
        return simpleDateFormat.format(c.getTime());
    }
}
