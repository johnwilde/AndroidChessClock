package johnwilde.androidchessclock.logic

interface BaseTimer {
    var msToGo : Long
    fun moveStart()
    fun moveEnd()
    fun pause()
    fun resume()
    fun setNewTime(ms : Long)
}