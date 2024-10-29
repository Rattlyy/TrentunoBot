package it.rattly.trentuno.games

sealed class WaitingStatus(val shouldWait: Boolean) {
    data object WAITING : WaitingStatus(true)
    data object CONTINUE : WaitingStatus(false)
}

sealed class TrentunoWaitingStatus(wait: Boolean) : WaitingStatus(wait) {
    data object PESCATO : TrentunoWaitingStatus(true)
}