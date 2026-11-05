cmake_minimum_required(VERSION 3.22.1)
project(tefzygisk CXX)

add_library(tefzygisk SHARED
        src/tef_zygisk.cpp
)

target_compile_features(tefzygisk PRIVATE cxx_std_17)
target_compile_options(tefzygisk PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(tefzygisk PRIVATE log dl android)
