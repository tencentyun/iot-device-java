package com.tencent.iot.device.video.advanced.recorder.param;

public class RecordThreadParam {

    private RecordParam recordParam;
    private MicParam micParam;
    private AudioEncodeParam audioEncodeParam;
    private CameraParam cameraParam;
    private VideoEncodeParam videoEncodeParam;

    public VideoEncodeParam getVideoEncodeParam() {
        return videoEncodeParam;
    }

    public void setVideoEncodeParam(VideoEncodeParam videoEncodeParam) {
        this.videoEncodeParam = videoEncodeParam;
    }

    public void setCameraParam(CameraParam cameraParam) {
        this.cameraParam = cameraParam;
    }

    public void setAudioEncodeParam(AudioEncodeParam audioEncodeParam) {
        this.audioEncodeParam = audioEncodeParam;
    }

    public void setMicParam(MicParam micParam) {
        this.micParam = micParam;
    }

    public void setRecordParam(RecordParam recordParam) {
        this.recordParam = recordParam;
    }

    public AudioEncodeParam getAudioEncodeParam() {
        return audioEncodeParam;
    }

    public CameraParam getCameraParam() {
        return cameraParam;
    }

    public MicParam getMicParam() {
        return micParam;
    }

    public RecordParam getRecordParam() {
        return recordParam;
    }


}
