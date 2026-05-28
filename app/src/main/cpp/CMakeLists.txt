cmake_minimum_required(VERSION 3.22)
project("llama_jni" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_subdirectory(llama.cpp)

add_library(llama_jni SHARED llama_jni.cpp)

target_include_directories(llama_jni PRIVATE llama.cpp)

target_link_libraries(llama_jni PRIVATE llama)
