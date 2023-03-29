package com.tencent.iot.device.video.advanced.recorder.opengles.render.manager;

import android.content.Context;

import com.tencent.iot.device.video.advanced.recorder.opengles.render.base.BaseRender;
import com.tencent.iot.device.video.advanced.recorder.opengles.render.base.bean.base.BaseRenderBean;
import com.tencent.iot.device.video.advanced.recorder.opengles.render.filter.BaseFilter;
import com.tencent.iot.device.video.advanced.recorder.utils.FilterUtils;


public class RenderProcess {
    private final Context context;

    private BaseFilter filter;

    public RenderProcess(Context context) {
        this.context = context.getApplicationContext();
    }

    public void init() {
        release();
    }

    public void release() {
        releaseRender(filter);
        filter = null;
    }

    public void onCreate() {
        onCreateRender(filter);
    }

    public void onChange() {
        onChangeRender(filter);
    }

    public int onDraw(int textureId, int width, int height) {
        int fboTextureId = onDrawRender(filter, textureId, width, height);
        return fboTextureId;
    }

    private void onCreateRender(BaseRender render) {
        if (render == null) {
            return;
        }
        render.setCreate(false);
    }

    private void onChangeRender(BaseRender render) {
        if (render == null) {
            return;
        }
        render.setChange(false);
    }

    private int onDrawRender(BaseRender render, int textureId, int width, int height) {
        if (render == null) {
            return textureId;
        }
        render.onCreate();
        render.onChange(width, height);
        render.onDraw(textureId);
        return render.getFboTextureId();
    }

    public void setFilter(BaseRenderBean bean) {
        if (bean == null) {
            filter = null;
            return;
        }
        filter = FilterUtils.getFilter(context, bean);
    }

    private void releaseRender(BaseRender render) {
        if (render == null) {
            return;
        }
        render.onRelease();
    }
}
