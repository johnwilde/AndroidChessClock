package johnwilde.androidchessclock.main

interface Partial<VS> {
    fun reduce(previousState: VS): VS
}