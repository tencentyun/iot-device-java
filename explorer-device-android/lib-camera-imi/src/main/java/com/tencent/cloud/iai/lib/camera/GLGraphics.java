package com.tencent.cloud.iai.lib.camera;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLGraphics {

    private static float[] squareVertices =
            { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };

    private static float[] coordVertices =
            { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f,0.0f, 1.0f, 0.0f };

    private static final String VERTEX_SHADER = "attribute vec4 vPosition;\n"
            + "attribute vec2 a_texCoord;\n" + "varying vec2 tc;\n"
            + "void main() {\n" + "gl_Position = vPosition;\n"
            + "tc = a_texCoord;\n" + "}\n";

    private static final String FRAGMENT_SHADER = "precision mediump float;\n"
            + "uniform sampler2D tex_y;\n" + "varying vec2 tc;\n"
            + "void main() {\n" + "gl_FragColor = texture2D(tex_y,tc);\n"
            + "}\n";

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private ByteBuffer coordbuffer;
    private ByteBuffer verticebuffer;

    private int mVertexCount = -1;
    private int mProgram = 0;
    private int mTexture = 0;
    private int mIndex = 0;
    private int mPositionHandle = -1, mCoordHandle = -1;
    private int yhandle = -1;
    private int ytid = -1;
    private int mGraphWidth = -1;
    private int mGraphHeight = -1;

    private boolean isProgBuilt = false;

    public GLGraphics() {
        mTexture = GLES20.GL_TEXTURE0;

        createBuffers();
    }

    private void createBuffers() {
        verticebuffer = ByteBuffer.allocateDirect(squareVertices.length * 4);
        verticebuffer.order(ByteOrder.nativeOrder());
        verticebuffer.asFloatBuffer().put(squareVertices);
        verticebuffer.position(0);

        coordbuffer = ByteBuffer.allocateDirect(coordVertices.length * 4);
        coordbuffer.order(ByteOrder.nativeOrder());
        coordbuffer.asFloatBuffer().put(coordVertices);
        coordbuffer.position(0);
    }


    public void setVertexData(float[] vertices) {
        if (vertices != null) {
            mVertexCount = vertices.length / 3;
            ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            mVertexBuffer = vbb.asFloatBuffer();
            mVertexBuffer.put(vertices);
            mVertexBuffer.position(0);

            float colors[] = new float[mVertexCount * 4];
            for (int i = 0; i < colors.length; i++) {
                if (i % 4 == 3) {
                    colors[i] = 0;
                } else {
                    colors[i] = 1;
                }
            }

            ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
            cbb.order(ByteOrder.nativeOrder());
            mColorBuffer = cbb.asFloatBuffer();
            mColorBuffer.put(colors);
            mColorBuffer.position(0);
        }
    }

    @SuppressLint("NewApi")
    public void draw() {
        GLES20.glUseProgram(mProgram);
        ShaderUtil.checkGlError("glUseProgram");
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false,
                8, verticebuffer);
        ShaderUtil.checkGlError("glVertexAttribPointer mPositionHandle");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mCoordHandle, 2, GLES20.GL_FLOAT, false, 8,
                coordbuffer);
        ShaderUtil.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(mCoordHandle);

        // bind textures
        GLES20.glActiveTexture(mTexture);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ytid);
        GLES20.glUniform1i(yhandle, mIndex);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mCoordHandle);
    }


    public boolean isProgramBuilt() {
        return isProgBuilt;
    }

    @SuppressLint("NewApi")
    public void buildProgram() {
        if (mProgram <= 0) {
            mProgram = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        }
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        ShaderUtil.checkGlError("glGetAttribLocation vPosition");
        if (mPositionHandle == -1) {
            throw new RuntimeException(
                    "Could not get attribute location for vPosition");
        }
        mCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        ShaderUtil.checkGlError("glGetAttribLocation a_texCoord");
        if (mCoordHandle == -1) {
            throw new RuntimeException(
                    "Could not get attribute location for a_texCoord");
        }
        yhandle = GLES20.glGetUniformLocation(mProgram, "tex_y");
        ShaderUtil.checkGlError("glGetUniformLocation tex_y");
        if (yhandle == -1) {
            throw new RuntimeException(
                    "Could not get uniform location for tex_y");
        }

        isProgBuilt = true;
    }

    @SuppressLint("NewApi")
    public void buildTextures(Buffer rgbBuffer, int width, int height) {
        boolean videoSizeChanged = (width != mGraphWidth || height != mGraphHeight);
        if (videoSizeChanged) {
            mGraphWidth = width;
            mGraphHeight = height;
        }

        if (ytid < 0 || videoSizeChanged) {
            if (ytid >= 0) {
                GLES20.glDeleteTextures(1, new int[] { ytid }, 0);
                ShaderUtil.checkGlError("glDeleteTextures");
            }
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            ShaderUtil.checkGlError("glGenTextures");
            ytid = textures[0];
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ytid);
        ShaderUtil.checkGlError("glBindTexture");

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, mGraphWidth,
                mGraphHeight, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE,
                rgbBuffer);
        ShaderUtil.checkGlError("glTexImage2D");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

    }
}
