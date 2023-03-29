package com.tencent.iot.device.video.advanced.recorder.opengles.render.base;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import com.tencent.iot.device.video.advanced.recorder.utils.OpenGLESUtils;

public class BaseOesRender extends BaseRender {

    private int oesTextureId; // oes 纹理 id
    private int uMatrixLocation; // 顶点变换矩阵位置
    private int uOesMatrixLocation; // 纹理变换矩阵位置
    private int oesW = -1; // oes尺寸
    private int oesH = -1; // oes尺寸
    private float[] mMVPMatrix = new float[16]; // 顶点变换矩阵
    private float[] mOesMatrix = { // 纹理变换矩阵
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1 };
    private boolean isReadyToDraw = false; // 是否准备好绘制
    private SurfaceTexture surfaceTexture;
    private OnSurfaceTextureListener onSurfaceTextureListener;

    public BaseOesRender(Context context) {
        super(context, "render/base/oes/vertex.frag", "render/base/oes/frag.frag");
        setBindFbo(true);
        oesTextureId = OpenGLESUtils.getOesTexture();
    }

    @Override
    public void onInitCoordinateBuffer() {
        setCoordinateBuffer(OpenGLESUtils.getSquareCoordinateBuffer());
    }

    @Override
    public boolean onReadyToDraw() {
        if (!isReadyToDraw) {
            if (onSurfaceTextureListener != null) {
                if (surfaceTexture != null) {
                    surfaceTexture.release();
                    surfaceTexture = null;
                }
                surfaceTexture = new SurfaceTexture(oesTextureId);
                onSurfaceTextureListener.onSurfaceTexture(surfaceTexture);
                isReadyToDraw = true;
            } else if (surfaceTexture != null) {
                surfaceTexture.attachToGLContext(oesTextureId);
                isReadyToDraw = true;
            } else {
                return false;
            }
        }
        return oesW != -1 && oesH != -1;
    }

    @Override
    public void onDrawPre() {
        super.onDrawPre();
        mMVPMatrix = OpenGLESUtils.getMatrix(getWidth(), getHeight(), oesW, oesH);

        surfaceTexture.updateTexImage();

        float[] oesMatrix = new float[16];
        surfaceTexture.getTransformMatrix(oesMatrix);
        if (!OpenGLESUtils.isIdentityM(oesMatrix)) {
            mOesMatrix = oesMatrix;
        }
    }

    @Override
    public void onInitLocation() {
        super.onInitLocation();
        uMatrixLocation = GLES20.glGetUniformLocation(getProgram(), "uMatrix");
        uOesMatrixLocation = GLES20.glGetUniformLocation(getProgram(), "uOesMatrix");
    }

    @Override
    public void onActiveTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(getSamplerLocation(), 0);
    }

    @Override
    public void onSetOtherData() {
        super.onSetOtherData();
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(uOesMatrixLocation, 1, false, mOesMatrix, 0);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        onDeleteTexture(oesTextureId);
    }

    /**
     * 绘制
     */
    public void onDrawSelf() {
        super.onDraw(oesTextureId);
    }

    /**
     * 设置 oes 尺寸
     */
    public void setOesSize(int width, int height) {
        oesW = width;
        oesH = height;
    }

    /**
     * 设置 SurfaceTexture
     */
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
        isReadyToDraw = false;
    }

    /**
     * 设置 SurfaceTexture 回调
     */
    public void setOnSurfaceTextureListener(OnSurfaceTextureListener onSurfaceTextureListener) {
        this.onSurfaceTextureListener = onSurfaceTextureListener;
        isReadyToDraw = false;
    }
}
