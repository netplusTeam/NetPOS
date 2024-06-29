#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getRrnUrl(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "https://getrrn.netpluspay.com");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getMerchantId(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "MID635ff140365c5");

}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getPayByTransferBearerToken(JNIEnv *env, jobject thiz) {
    return (*env) ->NewStringUTF(env, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdG9ybUlkIjoiYzIzMDY1MTctNmE5Mi0xMWVhLTk1N2MtZjIzYzkyOWIwMDU3IiwiYmFua05hbWUiOiIxQjBFNjhGRC03Njc2LTRGMkMtODgzRC0zOTMxQzM1NjQxOTAiLCJ0ZXJtaW5hbElkIjoiMjEwMUpBMjYiLCJ1c2VyX3R5cGUiOiJyZWd1bGFyIiwibWVyY2hhbnRJZCI6bnVsbCwiYnVzaW5lc3NOYW1lIjoib2xhbWlkZUB3ZWJtYWxsLm5nIiwidXNlcm5hbWUiOiJvbGFtaWRlQHdlYm1hbGwubmciLCJidXNpbmVzc19hZGRyZXNzIjoiTWFydXdhLCBMZWtraSByb2FkLCBMYWdvcyBzdGF0ZSIsInBob25lX251bWJlciI6IjA4MDMxNTU4MDE3IiwibmV0cGx1c1BheU1pZCI6bnVsbCwicGFydG5lcklkIjpudWxsLCJkb21haW5zIjpbIm5ldHBvcyJdLCJyb2xlcyI6WyJhZG1pbiJdLCJpc3MiOiJzdG9ybTphY2NvdW50cyIsInN1YiI6InVzZXIiLCJpYXQiOjE2OTUwNTI1MDYsImV4cCI6MTcyNjU4ODUwNn0.a7e3H8-Gmwi9vW5MntUGHqeJhuMF8oIrsR9q92xqo-Y");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getPayByTransferBaseUrl(JNIEnv *env, jobject thiz) {
    return (*env) ->NewStringUTF(env, "http://api.zenith-pbt.netpluspay.com/api/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getComplaintsBaseUrl(JNIEnv *env, jobject thiz) {
    return (*env) ->NewStringUTF(env, "https://netpos.netpluspay.com/api/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getFCMBMerchantsAccountBaseUrl(JNIEnv *env,
                                                                          jobject thiz) {
    return (*env) -> NewStringUTF(env, "https://pbt.fcmb.netpluspay.com/api/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getProvidusMerchantsAccountBaseUrl(JNIEnv *env,
                                                                              jobject thiz) {
    return (*env) -> NewStringUTF(env, "https://pbt.providus.netpluspay.com/api/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getCheckoutBaseUrl(JNIEnv *env, jobject thiz) {
    return (*env) -> NewStringUTF(env, "https://paytally.netpluspay.com/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getWebViewBaseUrl(JNIEnv *env, jobject thiz) {
    return (*env) -> NewStringUTF(env, "https://api.netpluspay.com/transactions/requery/");
}

JNIEXPORT jstring JNICALL
Java_com_woleapp_netpos_util_UtilityParams_getCheckoutMerchantId(JNIEnv *env, jobject thiz) {
    return (*env) -> NewStringUTF(env, "MID63dbdc67badab");
}