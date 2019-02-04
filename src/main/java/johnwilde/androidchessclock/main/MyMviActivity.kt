package johnwilde.androidchessclock.main

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.hannesdorfmann.mosby3.ActivityMviDelegate
import com.hannesdorfmann.mosby3.ActivityMviDelegateImpl
import com.hannesdorfmann.mosby3.MviDelegateCallback
import com.hannesdorfmann.mosby3.mvi.MviPresenter
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView

/**
 *
 *
 * This abstract class can be used to extend from to implement an Model-View-Intent pattern with
 * this activity as View and a [MviPresenter] to coordinate the View and the underlying
 * model (business logic).
 *
 *
 *
 * Per default [ActivityMviDelegateImpl] is used which means the View is attached to the
 * presenter in [ ][Activity.onStart]. You better initialize all your UI components before that, typically in
 * [Activity.onCreate].
 * The view is detached from presenter in [ ][Activity.onStop]
 *
 *
 * @author Hannes Dorfmann
 * @since 3.0.0
 */
abstract class MyMviActivity<V : MvpView, P : MviPresenter<V, *>> : AppCompatActivity(), MvpView, MviDelegateCallback<V, P> {

    private var isRestoringViewState = false
    val mvpDelegate by lazy {
        ActivityMviDelegateImpl(this, this) as ActivityMviDelegate<V, P>
    }
//    private lateinit var mvpDelegate: ActivityMviDelegate<V, P>
//    get() {
//        mvpDelegate = ActivityMviDelegateImpl(this, this) as ActivityMviDelegate<V, P>
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mvpDelegate.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mvpDelegate.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mvpDelegate.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        mvpDelegate.onPause()
    }

    override fun onResume() {
        super.onResume()
        mvpDelegate.onResume()
    }

    override fun onStart() {
        super.onStart()
        mvpDelegate.onStart()
    }

    override fun onStop() {
        super.onStop()
        mvpDelegate.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        mvpDelegate.onRestart()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        mvpDelegate.onContentChanged()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mvpDelegate.onPostCreate(savedInstanceState)
    }

    /**
     * Instantiate a presenter instance
     *
     * @return The [MvpPresenter] for this viewState
     */
    @NonNull
    abstract override fun createPresenter(): P

    /**
     * Get the mvp delegate. This is internally used for creating presenter, attaching and detaching
     * viewState from presenter.
     *
     *
     * **Please note that only one instance of mvp delegate should be used per Activity
     * instance**.
     *
     *
     *
     *
     * Only override this method if you really know what you are doing.
     *
     *
     * @return [ActivityMviDelegate]
     */
//    @NonNull
//    fun mvpDelegate: ActivityMviDelegate<V, P> {
//        if (mvpDelegate == null) {
//            mvpDelegate = ActivityMviDelegateImpl(this, this) as ActivityMviDelegate<V, P>
//        }
//
//        return mvpDelegate
//    }

    @NonNull
    override fun getMvpView(): V {
        try {
            return this as V
        } catch (e: ClassCastException) {
            val msg = "Couldn't cast the View to the corresponding View interface. Most likely you forgot to add \"Activity implements YourMviViewInterface\"."
            Log.e(this.toString(), msg)
            throw RuntimeException(msg, e)
        }

    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return mvpDelegate.onRetainCustomNonConfigurationInstance()
    }

    override fun setRestoringViewState(restoringViewState: Boolean) {
        this.isRestoringViewState = restoringViewState
    }

    protected fun isRestoringViewState(): Boolean {
        return isRestoringViewState
    }
}
