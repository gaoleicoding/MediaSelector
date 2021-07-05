package com.luck.pictureselector.listener;


public interface DragListener {
    /**
     * 是否将 item拖动到删除处，根据状态改变颜色
     */
    void deleteState(boolean isDelete);

    /**
     * 是否于拖拽状态
     */
    void dragState(boolean isStart);
}
