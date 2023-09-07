LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := module-credentials
LOCAL_SRC_FILES := module-credentials.c
include $(BUILD_SHARED_LIBRARY)