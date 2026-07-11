cmake_minimum_required(VERSION 3.22.1)
project(blackjack)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ./include holds card.h / deck.h / hand.h / blackjack_game.h
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_library(
    blackjack
    SHARED
    blackjack_jni.cpp
)

find_library(log-lib log)

target_link_libraries(
    blackjack
    ${log-lib}
)
