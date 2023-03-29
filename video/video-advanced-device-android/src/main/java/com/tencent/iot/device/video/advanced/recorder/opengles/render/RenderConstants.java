package com.tencent.iot.device.video.advanced.recorder.opengles.render;

public interface RenderConstants {
    /**
     * 处理类型
     */
    interface Process {
        int CAMERA = 0; // 相机
        int VIDEO = 1; // 视频
        int IMAGE = 2; // 图像
        int TAKE_PHOTO = 3; // 拍照
        int RECORD_VIDEO = 4; // 录像
    }

    /**
     * 滤镜
     */
    interface Filter {
        int NORMAL = -1; // 原画
        int MEAN_BLUR = 0; // 均值模糊
        int GAUSSIAN_BLUR = 1; // 高斯模糊
        int GRAY = 2; // 灰度滤镜
        int PIP = 3; // 画中画
        int MOTION_BLUR = 4; // 运动模糊
        int SCALE = 5; // 缩放
        int SKEW = 6; // 扭曲
        int BLUE_LINE_CHALLENGE_H = 7; // 蓝线挑战（横向）
        int BLUE_LINE_CHALLENGE_V = 8; // 蓝线挑战（纵向）
        int RETAIN_FRAME = 9; // 保留帧
        int CONVEYOR_BELT_H = 10; // 传送带（横向）
        int CONVEYOR_BELT_V = 11; // 传送带（纵向）
        int TWO_PART = 12; // 两分屏
        int THREE_PART = 13; // 三分屏
        int FOUR_PART = 14; // 四分屏
        int NINE_PART = 15; // 九分屏
    }

    /**
     * 转场特效
     */
    interface Transition {
        int NORMAL = -1; // 普通
        int MIX = 0; // 混合
        int BLUR = 1; // 模糊
        int PUSH = 2; // 推镜
        int PULL = 3; // 拉镜
        int VORTEX = 4; // 旋涡
        int LEFT_MOVE = 5; // 左移
        int RIGHT_MOVE = 6; // 右移
        int TOP_MOVE = 7; // 上移
        int DOWN_MOVE = 8; // 下移
        int LEFT_TOP_MOVE = 9; // 左上移
        int RIGHT_TOP_MOVE = 10; // 右上移
        int LEFT_DOWN_MOVE = 11; // 左下移
        int RIGHT_DOWN_MOVE = 12; // 右下移
        int PAGE_UP = 13; // 翻页
        int CUT_1 = 14; // 分割一
        int CUT_2 = 15; // 分割二
        int CUT_3 = 16; // 分割三
        int CUT_4 = 17; // 分割四
        int FLIP_HORIZONTAL = 18; // 水平翻转
        int FLIP_VERTICAL = 19; // 垂直翻转
    }

    /**
     * 转场特效 2
     */
    interface Transition2 {
        int NORMAL = -1; // 普通
        int MOVE_UP = 0; // 移动向上
        int MOVE_DOWN = 1; // 移动向下
        int MOVE_LEFT = 2; // 移动向左
        int MOVE_RIGHT = 3; // 移动向右
        int MOVE_LEFT_UP = 4; // 移动向左上
        int MOVE_RIGHT_UP = 5; // 移动向右上
        int MOVE_LEFT_DOWN = 6; // 移动向左下
        int MOVE_RIGHT_DOWN = 7; // 移动向右下
        int WIPE_LEFT = 8; // 抹掉向左
        int WIPE_RIGHT = 9; // 抹掉向右
        int WIPE_UP = 10; // 抹掉向上
        int WIPE_DOWN = 11; // 抹掉向下
        int WIPE_LEFT_UP = 12; // 抹掉向左上
        int WIPE_RIGHT_DOWN = 13; // 抹掉向右下
        int WIPE_LEFT_DOWN = 14; // 抹掉向左下
        int WIPE_RIGHT_UP = 15; // 抹掉向右上
        int WIPE_CENTER = 16; // 抹掉中心
        int WIPE_CIRCLE = 17; // 抹掉圆形
    }
}
