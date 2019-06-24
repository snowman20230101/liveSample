cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(src/main/cpp/librtmp)

#引入c/c++编译源文件
file(GLOB source_file src/main/cpp/*.cpp)

#生成自己的动态库
add_library(
        ffmpeg
        SHARED
        ${source_file})

#引入头文件
include_directories(src/main/cpp/include)

#设置静态库路径
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__STDC_CONSTANT_MACROS -L${CMAKE_SOURCE_DIR}/src/main/cpp/libs/${ANDROID_ABI}")

#连接动态库
target_link_libraries( ffmpeg
        # 引入ffmpeg静态库
        avformat avcodec avfilter avutil swresample swscale
        rtmp
        x264
        log z)