package johnwilde.androidchessclock.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils.setPicker
import johnwilde.androidchessclock.Utils.twoDigitFormatter
import kotlinx.android.synthetic.main.time_picker_row.view.*

class HourglassTimeSettingsFragment : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val a = activity as MainActivity
        val builder = AlertDialog.Builder(context!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_hourglass, null)

        view.hour_container.visibility = View.GONE
        setPicker(view.minute, 0, 59, a.preferenceUtil.hourglassMinutes)
        setPicker(view.second, 0, 59, a.preferenceUtil.hourglassSeconds)
        view.second.setFormatter { i -> twoDigitFormatter(i) }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.OK, DialogInterface.OnClickListener { dialog, id ->
                    // save settings
                    a.preferenceUtil.hourglassMinutes = view.minute.value
                    a.preferenceUtil.hourglassSeconds = view.second.value
                    a.clockManager.reset()
                })
                .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener {
                    dialog, id -> this@HourglassTimeSettingsFragment.dialog.cancel() })
        return builder.create()
    }
}