package johnwilde.androidchessclock.sound

sealed class SoundViewState
class Buzzer : SoundViewState()
class Click : SoundViewState()
