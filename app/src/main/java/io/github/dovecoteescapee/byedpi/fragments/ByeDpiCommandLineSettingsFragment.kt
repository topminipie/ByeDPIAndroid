package io.github.dovecoteescapee.byedpi.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.preference.*
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.utility.findPreferenceNotNull
import androidx.appcompat.app.AlertDialog
import io.github.dovecoteescapee.byedpi.utility.HistoryUtils

class ByeDpiCommandLineSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cmdHistoryUtils: HistoryUtils
    private lateinit var editTextPreference: EditTextPreference
    private lateinit var historyCategory: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.byedpi_cmd_settings, rootKey)

        cmdHistoryUtils = HistoryUtils(requireContext())

        editTextPreference = findPreferenceNotNull("byedpi_cmd_args")
        historyCategory = findPreferenceNotNull("cmd_history_category")

        editTextPreference.setOnPreferenceChangeListener { _, newValue ->
            val newCommand = newValue.toString()
            if (newCommand.isNotBlank()) cmdHistoryUtils.addCommand(newCommand)
            updateHistoryCategory()
            true
        }

        updateHistoryCategory()
    }

    private fun updateHistoryCategory() {
        historyCategory.removeAll()
        val history = cmdHistoryUtils.getHistory()
        val pinnedHistory = cmdHistoryUtils.getPinnedHistory()

        pinnedHistory.forEach { command ->
            val preference = createPreference(command, isPinned = true)
            historyCategory.addPreference(preference)
        }

        history.forEach { command ->
            if (command !in pinnedHistory) {
                val preference = createPreference(command, isPinned = false)
                historyCategory.addPreference(preference)
            }
        }
    }

    private fun createPreference(command: String, isPinned: Boolean) =
        Preference(requireContext()).apply {
            title = command
            summary = if (isPinned) context.getString(R.string.cmd_history_pinned) else null
            setOnPreferenceClickListener {
                showActionDialog(command, isPinned)
                true
            }
        }

    private fun showActionDialog(command: String, isPinned: Boolean) {
        val options = arrayOf(
            getString(R.string.cmd_history_apply),
            if (isPinned) getString(R.string.cmd_history_unpin) else getString(R.string.cmd_history_pin),
            getString(R.string.cmd_history_copy),
            getString(R.string.cmd_history_delete)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cmd_history_menu))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyCommand(command)
                    1 -> if (isPinned) unpinCommand(command) else pinCommand(command)
                    2 -> copyToClipboard(command)
                    3 -> deleteCommand(command)
                }
            }
            .show()
    }

    private fun applyCommand(command: String) {
        editTextPreference.text = command
    }

    private fun pinCommand(command: String) {
        cmdHistoryUtils.pinCommand(command)
        updateHistoryCategory()
    }

    private fun unpinCommand(command: String) {
        cmdHistoryUtils.unpinCommand(command)
        updateHistoryCategory()
    }

    private fun deleteCommand(command: String) {
        cmdHistoryUtils.deleteCommand(command)
        updateHistoryCategory()
    }

    private fun copyToClipboard(command: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Command", command)
        clipboard.setPrimaryClip(clip)
    }
}