package com.back.catchmate.application.chat.service;

public enum ReadSequenceMode {
    V1_DIRTY_CHECK,
    V2_DIRECT_UPDATE,
    V3_JAVA_BUFFERED,
    V4_LUA_BUFFERED,
}
