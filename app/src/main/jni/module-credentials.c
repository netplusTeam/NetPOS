#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getRrnUrl(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "https://getrrn.netpluspay.com");
}