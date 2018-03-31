package johnwilde.androidchessclock.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils.setPicker
import johnwilde.androidchessclock.Utils.twoDigitFormatter
import johnwilde.androidchessclock.prefs.PreferencesUtil
import kotlinx.android.synthetic.main.bonus_row.view.*
import kotlinx.android.synthetic.main.dialog_simple.view.*
import kotlinx.android.synthetic.main.time_picker_row.view.*

class BasicTimeSettingsFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val a = activity as MainActivity
        val builder = AlertDialog.Builder(context!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_simple, null)

        fun setBonusEnabled(isEnabled: Boolean) {
            view.bonus_text.isEnabled = isEnabled
            view.delay_spinner.isEnabled = isEnabled
            view.bonus_seconds_label.isEnabled = isEnabled
            view.bonus_seconds.isEnabled = isEnabled
        }

        setPicker(view.hour, 0, 9, a.preferenceUtil.initialHours)
        setPicker(view.minute, 0, 59, a.preferenceUtil.initialMinutes)
        setPicker(view.second, 0, 59, a.preferenceUtil.initialSeconds)
        view.minute.setFormatter { i -> twoDigitFormatter(i) }
        view.second.setFormatter { i -> twoDigitFormatter(i) }

        view.use_bonus.isChecked = a.preferenceUtil.basicSettingsUseBonus
        setBonusEnabled(view.use_bonus.isChecked)
        view.use_bonus.setOnCheckedChangeListener({ _ , isChecked ->
            setBonusEnabled(isChecked)
        })

        setPicker(view.bonus_seconds, 0, 59, a.preferenceUtil.basicIncrementSeconds)
        setPicker(view.delay_spinner, 0, 1, a.preferenceUtil.basicDelayType.ordinal)
        view.delay_spinner.displayedValues = a.resources.getStringArray(R.array.delay_type_descriptions)

        val descriptions = a.resources.getStringArray(R.array.delay_type_full_descriptions)
        view.bonus_text.text = descriptions[view.delay_spinner.value]
        view.delay_spinner.setOnValueChangedListener { _, _, newVal ->
            view.bonus_text.text = descriptions[newVal]
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.OK, { _, _ ->
                    // save settings
                    a.preferenceUtil.initialHours = view.hour.value
                    a.preferenceUtil.initialMinutes = view.minute.value
                    a.preferenceUtil.initialSeconds = view.second.value
                    a.preferenceUtil.basicSettingsUseBonus = view.use_bonus.isChecked
                    a.preferenceUtil.basicIncrementSeconds = view.bonus_seconds.value
                    val ordinal = view.delay_spinner.value
                    a.preferenceUtil.basicDelayType = PreferencesUtil.DelayType.values()[ordinal]
                    a.clockManager.reset()
                })
                .setNegativeButton(R.string.cancel, {
                    _, _ -> this@BasicTimeSettingsFragment.dialog.cancel() })
        return builder.create()
    }
}