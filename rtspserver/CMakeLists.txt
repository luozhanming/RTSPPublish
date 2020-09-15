cmake_minimum_required(VERSION 3.4.2)

add_definitions(-DNO_OPENSSL)
add_definitions(-DOS_LINUX)

set(LIVE555_PATH ${CMAKE_SOURCE_DIR}/src/main/jni/live555)
set(RTSP_PATH ${CMAKE_SOURCE_DIR}/src/main/jni/librtsp)
set(RTP_PATH ${CMAKE_SOURCE_DIR}/src/main/jni/librtp)
set(SDK_PATH ${CMAKE_SOURCE_DIR}/src/main/jni/libsdk)

include_directories(${LIVE555_PATH}/BasicUsageEnvironment/include/)
include_directories(${LIVE555_PATH}/groupsock/include/)
include_directories(${LIVE555_PATH}/liveMedia/include/)
include_directories(${LIVE555_PATH}/UsageEnvironment/include/)
include_directories(${LIVE555_PATH}/liveMedia/)
#include_directories(${CMAKE_SOURCE_DIR}/src/main/jni/)
#include_directories(${RTSP_PATH}/client/)
#include_directories(${RTSP_PATH}/include/)
#include_directories(${RTSP_PATH}/server/)
#include_directories(${RTP_PATH}/include/)
#include_directories(${RTP_PATH}/payload/)
#include_directories(${SDK_PATH}/http/include/)
#include_directories(${SDK_PATH}/http/source/)
#include_directories(${SDK_PATH}/include/)
#include_directories(${SDK_PATH}/include/cpm/)
#include_directories(${SDK_PATH}/include/libct/)
#include_directories(${SDK_PATH}/include/port/)
#include_directories(${SDK_PATH}/include/sys/)
#include_directories(${SDK_PATH}/source/port/)
#include_directories(${SDK_PATH}/aio/include/)
#include_directories(${SDK_PATH}/deprecated/)


#include_directories(${RTSP_PATH}/include/sdk/)
#include_directories(${RTSP_PATH}/include/aio/)
#include_directories(${RTSP_PATH}/sdk/port/)
#include_directories(${RTSP_PATH}/include/http/)
#include_directories(${RTSP_PATH}/http/)


file(GLOB_RECURSE BasicUsageEnvironment ${LIVE555_PATH}/BasicUsageEnvironment/*.cpp)
file(GLOB_RECURSE groupsock ${LIVE555_PATH}/groupsock/*.cpp ${LIVE555_PATH}/groupsock/*.c)
file(GLOB_RECURSE liveMedia ${LIVE555_PATH}/liveMedia/*.cpp)
file(GLOB_RECURSE UsageEnvironment ${LIVE555_PATH}/UsageEnvironment/*.cpp)
file(GLOB_RECURSE liveMedia2 ${LIVE555_PATH}/liveMedia/*.c)
#file(GLOB_RECURSE librtsp ${RTSP_PATH}/*.c)
#file(GLOB_RECURSE librtp ${RTP_PATH}/*.c)
#file(GLOB_RECURSE libsdk ${SDK_PATH}/*.c)

add_library(live555
            SHARED
            ${UsageEnvironment}
            ${BasicUsageEnvironment}
            ${groupsock}
            ${liveMedia}
            ${liveMedia2}
        )

#add_library(rtsp
#        SHARED
#        ${librtsp}
#        ${librtp}
#        ${libsdk}
#        )


add_library(native
        SHARED
        ${CMAKE_SOURCE_DIR}/src/main/jni/native_lib.cpp
        )

target_link_libraries(
        native
        live555
        android
)
