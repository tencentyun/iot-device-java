package com.tencent.iot.explorer.device.face.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.utils.ImageConverter;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.face.business.heavystep.AlignmentStep;
import com.tencent.iot.explorer.device.face.business.heavystep.CompareStep;
import com.tencent.iot.explorer.device.face.business.heavystep.QualityProStep;
import com.tencent.iot.explorer.device.face.business.heavystep.RetrievalStep;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.lightstep.FilterStep;
import com.tencent.iot.explorer.device.face.business.lightstep.PickBestStep;
import com.tencent.iot.explorer.device.face.business.lightstep.PreprocessStep;
import com.tencent.iot.explorer.device.face.business.lightstep.TrackStep;
import com.tencent.iot.explorer.device.face.widgets.FaceDrawView.DrawableFace;
import com.tencent.youtu.YTFaceAlignment.FaceShape;
import com.tencent.youtu.YTFaceRetrieval.RetrievedItem;
import com.tencent.youtu.YTFaceTracker.TrackedFace;
import com.tencent.youtu.YTImage;
import com.tencent.youtu.YTUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/** View 相关的事情都在这里面 */
public abstract class AbsActivityViewController {

    protected View mRootView;
    private final RadioGroup mSwitch;
    private final TextView mFindFaceText;
    private final TextView mLogText;

    protected AbsActivityViewController(Context context) {
        mRootView = LayoutInflater.from(context).inflate(R.layout.activity_view_controller, null);
        mFindFaceText = mRootView.findViewById(R.id.findFaceText);
        mLogText = mRootView.findViewById(R.id.logTxt);

        mSwitch = mRootView.findViewById(R.id.modeRadioGroup);
        mSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);

        mRootView.post(new Runnable() {
            @Override
            public void run() {
                View preview = onCreateCameraPreview();//onCreateCameraPreview() 需要在 AbsActivityViewController 构造方法以后执行, 因为onCreateCameraPreview()依赖AbsActivityViewController子类构造方法的产物
                replaceView(mRootView.findViewById(R.id.cameraPreview), preview);
            }
        });
    }

    public AbsActivityViewController addButton(String buttonText, final OnClickListener onClick) {
        Button button = new Button(mRootView.getContext());
        button.setText(buttonText);
        button.setOnClickListener(onClick);
        ((ViewGroup) mRootView.findViewById(R.id.button_container)).addView(button);
        return this;
    }

    public AbsActivityViewController addRadioButton(String buttonText, Runnable onCheckedRunnable) {
        RadioButton radioButton = new RadioButton(mRootView.getContext());
        radioButton.setText(buttonText);
        radioButton.setTag(onCheckedRunnable);
        radioButton.setId(View.generateViewId());
        mSwitch.addView(radioButton);
        return this;
    }

    public void setCompareImage(final Bitmap compareImg) {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                ((ImageView) mRootView.findViewById(R.id.compareImg)).setImageBitmap(compareImg);
            }
        });
    }

    public void setLogText(final String msg) {
        mLogText.post(new Runnable() {
            @Override
            public void run() {
                mLogText.setVisibility(View.VISIBLE);
                mLogText.setText(msg);
            }
        });
    }

    public void appendLogText(final String msg) {
        mLogText.post(new Runnable() {
            @Override
            public void run() {
                mLogText.setVisibility(View.VISIBLE);
                mLogText.append(msg + "\n");
            }
        });
    }

    private final OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            ((Runnable) group.findViewById(checkedId).getTag()).run();
        }
    };

    /**
     * 替换布局中的 Camera Preview
     * @return 新的 Camera Preview
     */
    protected abstract View onCreateCameraPreview();

    @SuppressLint("NewApi")
    protected void replaceView(View oldView, View newView) {
        if (newView == null) {
            return;
        }

        int id = oldView.getId();
        if (id == View.NO_ID) {
            id = View.generateViewId();
        }
        LayoutParams lp = oldView.getLayoutParams();
        ViewGroup parent = (ViewGroup) oldView.getParent();
        int index = parent.indexOfChild(oldView);
        parent.removeView(oldView);
        newView.setId(id);
        parent.addView(newView, index, lp);
    }

    public View getRootView() {
        return mRootView;
    }

    /**
     * 设置某个单选按钮的选中状态, 注意: 仅修改UI状态, 不触发业务逻辑
     * @param buttonName 单选按钮的名字
     */
    public void checkRadioButton(final String buttonName) {
        mSwitch.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<View> outViews = new ArrayList<>();
                mSwitch.findViewsWithText(outViews, buttonName, View.FIND_VIEWS_WITH_TEXT);
                if (outViews.size() == 0) {
                    throw new IllegalArgumentException("No button with name found: " + buttonName);
                }
                if (outViews.size() > 1) {
                    throw new IllegalArgumentException("More than one button with name found: " + buttonName);
                }
                mSwitch.setOnCheckedChangeListener(null);//设置状态前先置空监听器, 避免死循环
                mSwitch.check(outViews.get(0).getId());
                mSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
            }
        });
    }

    public void drawLightThreadStuff(StuffBox stuffBox) {
        Collection<TrackedFace> allFaces;
        allFaces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
        if (allFaces.size() == 0) {
            allFaces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
        }
        Collection<TrackedFace> allFacesCopy = new ArrayList<>(allFaces);

        List<DrawableFace> drawableFaces = new ArrayList<>();
        
        for (TrackedFace pickOkFace : stuffBox.find(PickBestStep.OUT_PICK_OK_FACES)) {
            drawableFaces.add(new DrawableFace(pickOkFace.faceRect, pickOkFace.xy5Points, faceToString(pickOkFace, "重点目标"), Color.GREEN));
            allFacesCopy.remove(pickOkFace);
        }
        for (Entry<TrackedFace, String> pickFailedFace : (stuffBox.find(PickBestStep.OUT_PICK_FAILED_FACES)).entrySet()) {
            TrackedFace face = pickFailedFace.getKey();
            drawableFaces.add(new DrawableFace(face.faceRect, face.xy5Points, faceToString(face, pickFailedFace.getValue()), Color.RED));
            allFacesCopy.remove(face);
        }
        for (Entry<TrackedFace, String> filterFailedFace : stuffBox.find(FilterStep.OUT_FILTER_FAIL_FACES).entrySet()) {
            TrackedFace face = filterFailedFace.getKey();
            drawableFaces.add(new DrawableFace(face.faceRect, face.xy5Points, faceToString(face, filterFailedFace.getValue()), Color.RED));
            allFacesCopy.remove(face);
        }
        for (TrackedFace face : allFacesCopy) {
            drawableFaces.add(new DrawableFace(face.faceRect, face.xy5Points, faceToString(face, ""), Color.GRAY));
        }
        
        //有些机器上, 绘制人脸框的 SurfaceView 可能有兼容性问题, 所以这里也用文字显示一下, 方便排查
        final String ms = "检测到人脸数量: " + allFaces.size();
        mFindFaceText.post(new Runnable() {
            @Override
            public void run() {
                mFindFaceText.setText(ms);
            }
        });

        Frame convertedColorFrame = stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP).colorFrame;
        onDrawColorFaces(drawableFaces, convertedColorFrame.width, convertedColorFrame.height);

        Frame convertedIrFrame = stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP).irFrame;
        if (convertedIrFrame != null) {//有红外帧, 绘制相关的数据
            //绘制与彩图人脸匹配的红外人脸
            List<DrawableFace> drawableIrFaces = new ArrayList<>();
            Collection<TrackedFace> matchedColorIrFaces = stuffBox.find(TrackStep.OUT_MATCHED_COLOR_IR_FACE).values();
            if (matchedColorIrFaces.size() == 0) {
                matchedColorIrFaces = stuffBox.find(TrackStep.OUT_MATCHED_COLOR_IR_FACE).values();
            }
            for (TrackedFace face : matchedColorIrFaces) {
                drawableIrFaces.add(new DrawableFace(face.faceRect, face.xy5Points, faceToString(face, "重点目标"), Color.GREEN));
            }

            //绘制与彩图人脸匹配失败的红外人脸
            Set<Entry<TrackedFace, String>> notMatchedFaces = stuffBox.find(TrackStep.OUT_MATCH_FAILED_COLOR_IR_FACE).entrySet();
            if (notMatchedFaces.size() == 0) {
                notMatchedFaces= stuffBox.find(TrackStep.OUT_MATCH_FAILED_COLOR_IR_FACE).entrySet();
            }
            for (Entry<TrackedFace, String> entry : notMatchedFaces) {
                TrackedFace face = entry.getKey();
                String failMsg = entry.getValue();
                drawableIrFaces.add(new DrawableFace(face.faceRect, face.xy5Points, faceToString(face, failMsg), Color.RED));
            }
            //提交绘制
            onDrawIrFaces(drawableIrFaces, convertedIrFrame.width, convertedIrFrame.height);
        }
        else {
            // 如果没有红外帧，仍需要调用一下该函数，否则若上一帧包含红外帧，人脸框不会消失
            onDrawIrFaces(new ArrayList<DrawableFace>(), 10, 10);
        }
    }

    protected abstract void onDrawColorFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight);
    protected abstract void onDrawIrFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight);
    protected abstract void onDrawDepthFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight);

    /**
     * TODO 性能优化
     */
    private static String faceToString(TrackedFace face, String extraMsg) {
        final StringBuffer sb = new StringBuffer();
        sb.append("traceId=").append(face.traceId).append("\n");
        sb.append("frameId=").append(face.frameId).append("\n");
        sb.append("consecutive=").append(face.consecutive).append("\n");
        sb.append("pitch=").append(String.format("%.2f", face.pitch)).append("\n");
        sb.append("yaw=").append(String.format("%.2f", face.yaw)).append("\n");
        sb.append("roll=").append(String.format("%.2f", face.roll)).append("\n");
        sb.append(extraMsg);
        return sb.toString();
    }

    private static float[] mergeFloats(float[]... arrays) {
        int finalLength = 0;
        for (float[] array : arrays) {
            finalLength += array.length;
        }

        float[] dest = null;
        int destPos = 0;

        for (float[] array : arrays) {
            if (dest == null) {
                dest = Arrays.copyOf(array, finalLength);
                destPos = array.length;
            } else {
                System.arraycopy(array, 0, dest, destPos, array.length);
                destPos += array.length;
            }
        }
        return dest;
    }

    public void drawHeavyThreadStuff(StuffBox stuffBox) {
        Frame colorFrame = stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP).colorFrame;

        // 绘制人脸90个关键点的画笔
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(5);

        // 人脸90个关键点
        float[] allPoints = null;

        // UI 数据
        Collection<TrackedFace> colorFaces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
        final List<FaceResult> faceResults = new ArrayList<>(colorFaces.size());
        // 每个人脸详细结果
        for (TrackedFace face : colorFaces) {
            FaceResult faceResult = new FaceResult();
            faceResults.add(faceResult);

            // 裁出人脸小图
            YTImage ytImage = YTUtils.cropRGB888(colorFrame.data, colorFrame.width, colorFrame.height, face.faceRect);
            Bitmap faceBitmap = ImageConverter.rgbToBitmap(ytImage.data, ytImage.width, ytImage.height);

            // 在人脸小图上绘制90个点
            FaceShape faceShape = stuffBox.find(AlignmentStep.OUT_FACE_SHAPE).get(face);
            if (faceShape != null && faceBitmap != null) {
                allPoints = mergeFloats(
                        faceShape.faceProfile,
                        faceShape.leftEyebrow,
                        faceShape.rightEyebrow,
                        faceShape.leftEye,
                        faceShape.rightEye,
                        faceShape.nose,
                        faceShape.mouth,
                        faceShape.pupil
                );
                float[] mappedPoints = new float[allPoints.length];
                Matrix matrix = new Matrix();
                matrix.postTranslate(-face.faceRect.left, -face.faceRect.top);//90个点本来是以帧左上角为原点的, 这里改成以人脸抠图左上角为原点
                matrix.mapPoints(mappedPoints, allPoints);
                Canvas canvas = new Canvas(faceBitmap);
                canvas.drawPoints(mappedPoints, paint);
            }

            faceResult.face = faceBitmap;

            //设置数据字段
            if (stuffBox.find(AlignmentStep.OUT_ALIGNMENT_OK_FACES).contains(face)) {
                faceResult.alignment = "alignmentOk";
                faceResult.alignment_color = Color.GREEN;
            } else if (stuffBox.find(AlignmentStep.OUT_ALIGNMENT_FAILED_FACES).containsKey(face)) {
                faceResult.alignment = "alignmentFail, " + stuffBox.find(AlignmentStep.OUT_ALIGNMENT_FAILED_FACES).get(face);
                faceResult.alignment_color = Color.RED;
            } else {
                faceResult.alignment = "alignment NONE";
                faceResult.alignment_color = Color.GRAY;
            }

            if (stuffBox.find(QualityProStep.OUT_QUALITY_PRO_OK).contains(face)) {
                faceResult.quality_pro = "qualityProOk";
                faceResult.quality_pro_color = Color.GREEN;
            } else if (stuffBox.find(QualityProStep.OUT_QUALITY_PRO_FAILED).containsKey(face)) {
                faceResult.quality_pro = "qualityProFail, " + stuffBox.find(QualityProStep.OUT_QUALITY_PRO_FAILED).get(face);
                faceResult.quality_pro_color = Color.RED;
            } else {
                faceResult.quality_pro = "qualityPro NONE";
                faceResult.quality_pro_color = Color.GRAY;
            }

            if (stuffBox.find(RetrievalStep.OUT_RETRIEVE_RESULTS).containsKey(face)) {
                StringBuilder sb = new StringBuilder();
                sb.append("retrievalOK");
                for (RetrievedItem i : stuffBox.find(RetrievalStep.OUT_RETRIEVE_RESULTS).get(face)) {
                    sb.append("\n");
                    sb.append(String.format("%s, sco=%.1f,sim=%.3f", i.featureId, i.score, i.sim));
                }
                faceResult.retrieval = sb.toString();
                faceResult.retrieval_color = Color.GREEN;
            } else {
                faceResult.retrieval = "retrieval NONE";
                faceResult.retrieval_color = Color.GRAY;
            }

            if (stuffBox.find(CompareStep.OUT_COMPARE_SCORE).containsKey(face)) {
                faceResult.compare = "compare sco=" + stuffBox.find(CompareStep.OUT_COMPARE_SCORE).get(face);
                faceResult.compare_color = Color.GREEN;
            } else {
                faceResult.compare = "compare NONE";
                faceResult.compare_color = Color.GRAY;
            }
        }

        // 彩色帧全图
        final Bitmap colorFrameBitmap = ImageConverter.rgbToBitmap(colorFrame.data, colorFrame.width, colorFrame.height);
        if (colorFrameBitmap != null && allPoints != null) {
            Canvas canvas = new Canvas(colorFrameBitmap);
            canvas.drawPoints(allPoints, paint);
        }

        // 切换到主线程绘制
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                //显示帧
                ((ImageView) mRootView.findViewById(R.id.frame)).setImageBitmap(colorFrameBitmap);

                //容器, 用于显示该帧的人脸识别结果
                ViewGroup container = mRootView.findViewById(R.id.mHeavyBusinessResultContainerView);
                //清空容器
                container.removeAllViews();
                //往容器添加内容
                LayoutInflater inflater = LayoutInflater.from(mRootView.getContext());
                for (FaceResult r : faceResults) {
                    View item = inflater.inflate(R.layout.heavy_working_stuff, container, false);

                    ((ImageView) item.findViewById(R.id.img)).setImageBitmap(r.face);

                    TextView alignment = (TextView) item.findViewById(R.id.alignment);
                    alignment.setText(r.alignment);
                    alignment.setTextColor(r.alignment_color);

                    TextView quality_pro = (TextView) item.findViewById(R.id.quality_pro);
                    quality_pro.setText(r.quality_pro);
                    quality_pro.setTextColor(r.quality_pro_color);

                    TextView color_live = (TextView) item.findViewById(R.id.color_live);
                    color_live.setText(r.color_live);
                    color_live.setTextColor(r.color_live_color);

                    TextView ir_live = (TextView) item.findViewById(R.id.ir_live);
                    ir_live.setText(r.ir_live);
                    ir_live.setTextColor(r.ir_live_color);

                    TextView depth_live = (TextView) item.findViewById(R.id.depth_live);
                    depth_live.setText(r.depth_live);
                    depth_live.setTextColor(r.depth_live_color);

                    TextView retrieval = (TextView) item.findViewById(R.id.retrieval);
                    retrieval.setText(r.retrieval);
                    retrieval.setTextColor(r.retrieval_color);
                    
                    TextView compare = (TextView) item.findViewById(R.id.compare);
                    compare.setText(r.compare);
                    compare.setTextColor(r.compare_color);

                    container.addView(item);
                }
            }
        });

    }

    public static class FaceResult {

        Bitmap face;

        String alignment;
        int alignment_color;

        String quality_pro;
        int quality_pro_color;

        String color_live;
        int color_live_color;

        String ir_live;
        int ir_live_color;

        String depth_live;
        int depth_live_color;

        String retrieval;
        int retrieval_color;

        String compare;
        int compare_color;
    }

}
