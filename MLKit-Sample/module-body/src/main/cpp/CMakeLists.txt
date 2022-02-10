# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)


find_package(bytehook REQUIRED CONFIG)

# libhack.so
add_library(detour SHARED detour.c)
target_compile_features(detour PUBLIC c_std_11)
target_compile_options(detour PUBLIC -Weverything -Werror)
target_link_libraries(detour bytehook::bytehook log)
