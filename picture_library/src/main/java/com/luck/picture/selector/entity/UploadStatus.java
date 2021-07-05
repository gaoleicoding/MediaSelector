package com.luck.picture.selector.entity;

public enum UploadStatus {

    //缺省值
    DEFAULT(0),
    //上传中
    SENDING(1),
    //上传失败
    SEND_FAIL(2),
    //上传成功
    SEND_SUCCESS(3);

    private final int value;

    UploadStatus(int value) {
        this.value = value;
    }

    public static UploadStatus fromInt(int v) {
        switch (v) {
            case 1:
                return SENDING;
            case 2:
                return SEND_FAIL;
            case 3:
                return SEND_SUCCESS;
            default:
                return null;
        }
    }

    public int value() {
        return this.value;
    }
}
