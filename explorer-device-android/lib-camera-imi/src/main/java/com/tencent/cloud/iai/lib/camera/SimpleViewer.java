package com.tencent.cloud.iai.lib.camera;

import com.tencent.cameralib.beans.FrameStreamBean;
import com.tencent.cameralib.utils.NativeMethods;

import java.nio.ByteBuffer;

public class SimpleViewer {

    private GLPanel mDepthGLPanel = null;
    private GLPanel mIRGLPanel = null;
    private GLPanel mColorGLPanel = null;

    private FrameStreamBean mStreamBean;
    byte [] irDataCache = null;
    byte [] depthDataCache = null;

    public SimpleViewer() {
    }

    public void setStreamBean(FrameStreamBean frameStreamBean){
        mStreamBean = frameStreamBean;
        drawStreamBean();

    }
    public void setDepthGLPanel(GLPanel GLPanel) {
        this.mDepthGLPanel = GLPanel;
    }

    public void setIRGLPanel(GLPanel GLPanel) {
        this.mIRGLPanel = GLPanel;
    }

    public void setColorGLPanel(GLPanel GLPanel) {
        this.mColorGLPanel = GLPanel;
    }

    private void drawStreamBean(){
        if (mStreamBean!=null){
            switch (mStreamBean.type){
                case 0 :
                    drawColor(mStreamBean);
                    break;
                case 1:
                    drawDepth(mStreamBean);
                    break;
                case 2:
                    drawIr(mStreamBean);
                    break;
                default:
                    break;
            }
        }
    }


//    private byte[] color = new byte[3];

    private void drawDepth(FrameStreamBean streamBean) {
        if (mDepthGLPanel==null) {
            return;
        }
        
        if (depthDataCache == null || depthDataCache.length!=streamBean.data.length)
            depthDataCache = new byte[streamBean.data.length];
        System.arraycopy(streamBean.data,0,depthDataCache,0,streamBean.data.length);
        byte [] rgb888 = NativeMethods.convert16bit2RGB888Depth255(depthDataCache, streamBean.width, streamBean.height);
        ByteBuffer byteBuffer = ByteBuffer.wrap(rgb888);
        mDepthGLPanel.paint(null, byteBuffer, streamBean.width, streamBean.height);

//        ByteBuffer byteBuffer = Utils.depth2RGB888(ByteBuffer.wrap(depthDataCache),streamBean.width,streamBean.height, true, false);
//        mDepthGLPanel.paint(null, byteBuffer, streamBean.width, streamBean.height);

    }

    private void drawIr(FrameStreamBean streamBean) {
        if (mIRGLPanel == null) {
            return;
        }
        if (irDataCache == null || irDataCache.length!=streamBean.data.length)
            irDataCache = new byte[streamBean.data.length];
        System.arraycopy(streamBean.data,0,irDataCache,0,streamBean.data.length);
        byte [] rgb888 = NativeMethods.convert16bit2RGB888(irDataCache, streamBean.width, streamBean.height, 1.5f);
        ByteBuffer byteBuffer = ByteBuffer.wrap(rgb888);
        mIRGLPanel.paint(null, byteBuffer, streamBean.width, streamBean.height);
    }
    private void drawColor(FrameStreamBean streamBean) {
        if (mColorGLPanel == null) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(streamBean.data);
        mColorGLPanel.paint(null, byteBuffer, streamBean.width, streamBean.height);
    }

    public void onPause(){
        if(mDepthGLPanel != null){
            mDepthGLPanel.onPause();
        }

        if(mIRGLPanel != null){
            mIRGLPanel.onPause();
        }

        if(mColorGLPanel != null){
            mColorGLPanel.onPause();
        }
    }

    public void onResume(){
        if(mDepthGLPanel != null){
            mDepthGLPanel.onResume();
        }

        if(mIRGLPanel != null){
            mIRGLPanel.onResume();
        }

        if(mColorGLPanel != null){
            mColorGLPanel.onResume();
        }
    }

}
