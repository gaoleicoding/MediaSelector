package com.luck.picture.selector.listener;

import android.content.Context;

import com.luck.picture.selector.config.PictureSelectionConfig;

/**
 * @author：luck
 * @date：2020/4/27 3:24 PM
 * @describe：OnCustomCameraInterfaceListener
 */
public interface OnCustomCameraInterfaceListener {
    /**
     * Camera Menu
     *
     * @param context
     * @param config
     * @param type
     */
    void onCameraClick(Context context, PictureSelectionConfig config, int type);
}
