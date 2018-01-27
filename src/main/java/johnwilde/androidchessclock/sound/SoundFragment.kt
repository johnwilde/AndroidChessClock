package johnwilde.androidchessclock.sound

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hannesdorfmann.mosby3.mvi.MviFragment
import johnwilde.androidchessclock.ChessApplication
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.R
import timber.log.Timber
import java.io.IOException

class SoundFragment : MviFragment<SoundView, SoundViewPresenter>(), SoundView {
    lateinit var clockManager : ClockManager
    lateinit var preferences : PreferencesUtil

    // for sounding buzzer
    private var mBellId: Int = 0
    private var mClickId: Int = 0
    private var mSoundPool: SoundPool? = null

    override fun createPresenter(): SoundViewPresenter {
        return SoundViewPresenter(clockManager)
    }

    override fun onAttach(context: Context?) {
        val dependencyInjection = ChessApplication.getDependencyInjection(context!!)
        preferences = dependencyInjection.preferenceUtil
        clockManager = dependencyInjection.clockManager
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("SoundFragment create")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.empty, container, false)
    }

    override fun onResume() {
        super.onResume()
        acquireMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun render(viewState: SoundViewState) {
        Timber.d("%s", viewState)
        // Don't replay sounds when fragment is being recreated (the BehaviorSubject sends the
        // viewstate, but this doesn't apply to sounds that have already been played)
        if (!isRestoringViewState) {
            when (viewState) {
                is Buzzer -> {
                    if (preferences.playBuzzerAtEnd) playBell()
                }
                is Click -> {
                    if (preferences.playSoundOnButtonTap) playClick()
                }
            }
        }
    }

    private fun acquireMediaPlayer() {
        mSoundPool = SoundPool(1, AudioManager.STREAM_MUSIC, 100)
        mBellId = getMediaPlayer(R.raw.bell)
        mClickId = getMediaPlayer(R.raw.click)
    }

    private fun getMediaPlayer(media: Int): Int {
        var id : Int = -1
        try {
            val afd = resources.openRawResourceFd(media)
            id = mSoundPool?.load(afd, 1) ?: -1
            afd.close()
        } catch (ex: IOException) {
            // fall through
        } catch (ex: IllegalArgumentException) {
            // fall through
        } catch (ex: SecurityException) {
            // fall through
        }

        return id
    }

    private fun releaseMediaPlayer() {
        if (mSoundPool != null) {
            mSoundPool?.release()
            mSoundPool = null
            mClickId = -1
            mBellId = -1
        }
    }

    private fun playBell() {
        playSound(mBellId)
    }

    private fun playClick() {
        playSound(mClickId)
    }

    private fun playSound(soundID: Int) {
        if (mSoundPool == null) {
            return
        }
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        val curVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val leftVolume = curVolume / maxVolume
        val rightVolume = curVolume / maxVolume
        val priority = 1
        val no_loop = 0
        val normal_playback_rate = 1f
        mSoundPool?.play(soundID, leftVolume, rightVolume, priority, no_loop, normal_playback_rate)
    }
}
