package io.r_a_d.radio2

enum class Actions
{
    PLAY,
    STOP,
    PAUSE,
    VOLUME,
    KILL,
    NOTIFY,
    PLAY_OR_FALLBACK
}

enum class ActionOnError {
    RESET,
    NOTIFY
}