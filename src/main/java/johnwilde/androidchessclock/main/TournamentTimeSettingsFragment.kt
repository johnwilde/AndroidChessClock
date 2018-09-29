package johnwilde.androidchessclock.main

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils.setPicker
import johnwilde.androidchessclock.Utils.twoDigitFormatter
import johnwilde.androidchessclock.prefs.PreferencesUtil
import kotlinx.android.synthetic.main.bonus_row.view.*
import kotlinx.android.synthetic.main.dialog_tournament.view.*
import kotlinx.android.synthetic.main.time_picker_row.view.*

class TournamentTimeSettingsFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val a = activity as MainActivity
        val builder = AlertDialog.Builder(context!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_tournament, null)
        view.moves_container.visibility = View.VISIBLE

        fun setBonusEnabled(isEnabled: Boolean) {
            view.bonus_text.isEnabled = isEnabled
            view.delay_spinner.isEnabled = isEnabled
            view.bonus_seconds_label.isEnabled = isEnabled
            view.bonus_seconds.isEnabled = isEnabled
        }

        var hr = a.preferenceUtil.phase1Minutes / 60
        var min = a.preferenceUtil.phase1Minutes % 60
        initTimePicker(view.findViewById(R.id.phase1), hr, min)

        setPicker(view.phase1_moves, 0, 50, a.preferenceUtil.phase1NumberOfMoves)

        hr = a.preferenceUtil.phase2Minutes / 60
        min = a.preferenceUtil.phase2Minutes % 60
        initTimePicker(view.findViewById(R.id.phase2), hr, min)

        view.use_bonus.isChecked = a.preferenceUtil.tournamentUseBonus
        view.use_bonus.setOnCheckedChangeListener({ _, isChecked ->
            view.bonus_seconds.isEnabled = isChecked
            view.delay_spinner.isEnabled = isChecked
        })

        view.use_bonus.isChecked = a.preferenceUtil.tournamentUseBonus
        setBonusEnabled(view.use_bonus.isChecked)
        view.use_bonus.setOnCheckedChangeListener({ _, isChecked ->
            setBonusEnabled(isChecked)
        })

        setPicker(view.bonus_seconds, 0, 59, a.preferenceUtil.tournamentIncrementSeconds)
        setPicker(view.delay_spinner, 0, 1, a.preferenceUtil.tournamentDelayType.ordinal)
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
                .setPositiveButton(R.string.OK, { _, id ->
                    // save settings
                    a.preferenceUtil.phase1Minutes =
                            view.phase1.hour.value * 60 +
                            view.phase1.minute.value
                    a.preferenceUtil.phase1NumberOfMoves = view.phase1_moves.value
                    a.preferenceUtil.phase2Minutes =
                            view.phase2.hour.value * 60 +
                            view.phase2.minute.value
                    a.preferenceUtil.tournamentUseBonus = view.use_bonus.isChecked
                    a.preferenceUtil.tournamentIncrementSeconds = view.bonus_seconds.value
                    val ordinal = view.delay_spinner.value
                    a.preferenceUtil.tournamentDelayType = PreferencesUtil.DelayType.values()[ordinal]

                    a.clockManager.reset()
                })
                .setNegativeButton(R.string.cancel, {
                    _, _ -> this@TournamentTimeSettingsFragment.dialog.cancel() })
        return builder.create()
    }

    private fun initTimePicker(view: View, hours: Int, min: Int) {
        view.seconds_container.visibility = View.GONE
        setPicker(view.hour, 0, 9, hours)
        setPicker(view.minute, 0, 59, min)
        view.minute.setFormatter { i -> twoDigitFormatter(i) }
    }
}