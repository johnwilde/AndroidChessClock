package johnwilde.androidchessclock.main

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.sectionHeader
import co.zsmb.materialdrawerkt.draweritems.switchable.switchItem
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.model.SwitchDrawerItem
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.prefs.PreferencesUtil
import javax.inject.Inject

class MainDrawerBuilder @Inject constructor(var preferenceUtil: PreferencesUtil)
    : SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val subject: PublishSubject<Any> = PublishSubject.create()
    }
    lateinit var myDrawer: Drawer
    fun onDrawerOpened(activity: MainActivity) {
        myDrawer = activity.myDrawer
        subject.onNext(1)
        register(activity)
    }
    private fun onDrawerClosed(activity: MainActivity) {
        unregister(activity)
    }
    private fun unregister(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun register(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        updateDescription(1, preferenceUtil.formattedHourglass())
        updateDescription(2, preferenceUtil.formattedBasicTime())
        updateDescription(3, preferenceUtil.formattedTournament())
    }

    private fun updateDescription(identifier: Long, description: String) {
        val drawerItem = myDrawer.getDrawerItem(identifier)
        if (drawerItem is SwitchDrawerItem) {
            drawerItem.withDescription(description)
            myDrawer.updateItem(drawerItem)
        }
    }

    private fun toggleOtherItems(activity: MainActivity, identifier: Long) {
        val set = mutableSetOf<Long>(1, 2, 3)
        set.remove(identifier)
        for (i in set) {
            val item = myDrawer.getDrawerItem(i) as SwitchDrawerItem
            item.withChecked(false)
            activity.myDrawer.updateItem(item)
        }
        activity.clockManager.reset()
    }

    fun buildDrawer(activity: MainActivity): Drawer {
        return activity.drawer {
            closeOnClick = false
            showOnFirstLaunch = true
            headerViewRes = R.layout.header
            sectionHeader(R.string.time_control)
            switchItem(R.string.basic_time_preference_description) {
                iicon = MaterialDesignIconic.Icon.gmi_timer
                description = preferenceUtil.formattedBasicTime()
                identifier = 2
                selectable = false
                checked = preferenceUtil.timeControlType == PreferencesUtil.TimeControlType.BASIC
                onSwitchChanged { _, _, isEnabled ->
                    if (isEnabled) {
                        preferenceUtil.timeControlType = PreferencesUtil.TimeControlType.BASIC
                        toggleOtherItems(activity, identifier)
                    }
                }
                onBindView { drawerItem, _ ->
                    val switchDrawerItem = (drawerItem as SwitchDrawerItem)
                    switchDrawerItem.withOnDrawerItemClickListener { _, _, _ ->
                        showBasicTimePicker(activity)
                        false
                    }
                }
            }
            switchItem(R.string.hourglass_time_preference_description) {
                iicon = MaterialDesignIconic.Icon.gmi_hourglass_outline
                description = preferenceUtil.formattedHourglass()
                identifier = 1
                selectable = false
                checked = preferenceUtil.timeControlType == PreferencesUtil.TimeControlType.HOURGLASS
                onSwitchChanged { _, _, isEnabled ->
                    if (isEnabled) {
                        preferenceUtil.timeControlType = PreferencesUtil.TimeControlType.HOURGLASS
                        toggleOtherItems(activity, identifier)
                    }
                }
                onBindView { drawerItem, _ ->
                    val switchDrawerItem = (drawerItem as SwitchDrawerItem)
                    switchDrawerItem.withOnDrawerItemClickListener { _, _, _ ->
                        showHourglassTimePicker(activity)
                        false
                    }
                }
            }
            switchItem(R.string.advanced_time_preference_description) {
                iicon = MaterialDesignIconic.Icon.gmi_timer
                description = preferenceUtil.formattedTournament()
                identifier = 3
                selectable = false
                checked = preferenceUtil.timeControlType == PreferencesUtil.TimeControlType.TOURNAMENT
                onSwitchChanged { _, _, isEnabled ->
                    if (isEnabled) {
                        preferenceUtil.timeControlType = PreferencesUtil.TimeControlType.TOURNAMENT
                        toggleOtherItems(activity, identifier)
                    }
                }
                onBindView { drawerItem, _ ->
                    val switchDrawerItem = (drawerItem as SwitchDrawerItem)
                    switchDrawerItem.withOnDrawerItemClickListener { _, _, _ ->
                        showTournamentTimePicker(activity)
                        false
                    }
                }
            }
            sectionHeader(R.string.sound)
            switchItem(R.string.buzzer) {
                iicon = MaterialDesignIconic.Icon.gmi_notifications_active
                selectable = false
                checked = preferenceUtil.playBuzzerAtEnd
                onToggled { preferenceUtil.playBuzzerAtEnd = preferenceUtil.playBuzzerAtEnd.not() }
            }

            switchItem(R.string.button_click) {
                iicon = MaterialDesignIconic.Icon.gmi_audio
                selectable = false
                checked = preferenceUtil.playSoundOnButtonTap
                onToggled { preferenceUtil.playSoundOnButtonTap = preferenceUtil.playSoundOnButtonTap.not() }
            }
            sectionHeader(R.string.display)
            switchItem(R.string.negative_time) {
                iicon = MaterialDesignIconic.Icon.gmi_neg_1
                selectable = false
                checked = preferenceUtil.allowNegativeTime
                onToggled { preferenceUtil.allowNegativeTime = preferenceUtil.allowNegativeTime.not() }
            }
            switchItem(R.string.show_time_gap) {
                iicon = MaterialDesignIconic.Icon.gmi_compare
                selectable = false
                checked = preferenceUtil.showTimeGap
                onToggled { preferenceUtil.showTimeGap = preferenceUtil.showTimeGap.not() }
            }
            divider { }
            primaryItem(R.string.about) {
                iicon = MaterialDesignIconic.Icon.gmi_info
                selectable = false
                onClick { _ ->
                    activity?.showAboutDialog()
                    true
                }
            }
            onOpened { onDrawerOpened(activity) }
            onClosed { onDrawerClosed(activity) }
        }
    }

    private fun showBasicTimePicker(activity: AppCompatActivity) {
        val newFragment = BasicTimeSettingsFragment()
        newFragment.show(activity.supportFragmentManager, "timePicker")
    }

    private fun showHourglassTimePicker(activity: AppCompatActivity) {
        val newFragment = HourglassTimeSettingsFragment()
        newFragment.show(activity.supportFragmentManager, "timePicker")
    }

    private fun showTournamentTimePicker(activity: AppCompatActivity) {
        val newFragment = TournamentTimeSettingsFragment()
        newFragment.show(activity.supportFragmentManager, "timePicker")
    }
}