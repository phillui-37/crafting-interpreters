cmake_minimum_required(VERSION 3.25)
project(CLox LANGUAGES C)

## Set latest C standard
set(CMAKE_C_STANDARD 23)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

## Output directory
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/out)

## Source files
file(GLOB_RECURSE SOURCES "src/*.c")

## Executable target
add_executable(CLox ${SOURCES})

## Header directory
target_include_directories(CLox PRIVATE header)

## Compiler options
target_compile_options(CLox PRIVATE -Wall -O3)