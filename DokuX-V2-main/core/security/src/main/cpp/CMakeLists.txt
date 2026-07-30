cmake_minimum_required(VERSION 3.22.1)
project("docsecurity")

add_library(docsecurity SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(docsecurity ${log-lib})
