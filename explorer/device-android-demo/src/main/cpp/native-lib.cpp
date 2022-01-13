#include <jni.h>
#include <string>
#include <stdio.h>
#include <android/log.h>
#include <errno.h>
#include <string.h>
#include <pthread.h>

extern "C" int iot_video_main(int argc, char **argv);

char sg_dev_path[256];
char sg_flv_path[256];
char sg_cs_aac_path[256];
char sg_cs_video_path[256];
char sg_cs_recv_talk_path[256];
char sg_cse_srcipt_path[256];
char sg_cse_test_pic_path[256];

static void *thd_fn(void *arg) {
    pthread_detach(pthread_self());
    char *args[8] = {NULL, sg_dev_path, sg_flv_path, sg_cs_recv_talk_path, sg_cs_aac_path, sg_cs_video_path,sg_cse_srcipt_path,sg_cse_test_pic_path};
    iot_video_main(8, args);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ivdemo_MainActivity_nativeDemo(
        JNIEnv* env,
        jobject /* this */,
        jstring dir) {
    const char *dir_path = env->GetStringUTFChars(dir, 0);
    std::string hello = "IoT Video";
    snprintf(sg_dev_path, 256, "%s/device.json", dir_path);
    snprintf(sg_flv_path, 256, "%s/p2p_test_file.flv", dir_path);
    snprintf(sg_cs_recv_talk_path, 256, "%s/talk_recv.flv", dir_path);
    snprintf(sg_cs_aac_path, 256, "%s/audio_sample44100_stereo_96kbps.aac", dir_path);
    snprintf(sg_cs_video_path, 256, "%s/video_size640x360_gop50_fps25.h264", dir_path);
    snprintf(sg_cse_srcipt_path, 256, "%s/event_test_script.txt", dir_path);
    snprintf(sg_cse_test_pic_path, 256, "%s/pic/", dir_path);

    pthread_t pid;
    pthread_create(&pid, NULL, thd_fn, NULL);
    return env->NewStringUTF(hello.c_str());
}