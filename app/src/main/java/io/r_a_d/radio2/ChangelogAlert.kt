package io.r_a_d.radio2

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.AssetManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ChangelogAlert (private val context: Context)
{
    // Get the AssetManager instance
    private val assetManager: AssetManager = context.assets

    // Declare an InputStream and a BufferedReader for reading the file
    private var inputStream: InputStream? = null
    private var bufferedReader: BufferedReader? = null

    fun showAllChangelog()
    {
        try {
            val changelogFiles = assetManager.list("changelogs")
            val changelogValues = changelogFiles?.map {  it.replace(".html", "")  }?.sorted()?.reversed()
            val changelogContent = readChangelogs(changelogValues)
            showChangelogAlert(changelogValues?.max(), changelogContent, false)
        } catch (e: IOException) {
            // Handle the exception
            e.printStackTrace()
        }
    }

    fun showLastChangelog()
    {
        val thisVersion  = try {
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            ).versionName
        } catch (e: NameNotFoundException) {
            Log.e(MainActivityTag
                , "could not get version name from manifest!")
            e.printStackTrace()
            ""
        }
        if (thisVersion != null) {
            try {
                val changelogFiles: List<String> = listOf(thisVersion)
                val changelogContent = readChangelogs(changelogFiles)
                showChangelogAlert(changelogFiles.maxOf { it }, changelogContent, true)
            } catch (e: IOException) {
                // Handle the exception
                e.printStackTrace()
            }
        }
    }

    private fun readChangelogs(versionList: List<String>?): String
    {
        var allChangelog: String = ""
        // Declare a StringBuilder for storing the file contents
        if (versionList != null) {
            for (version in versionList) {
                val stringBuilder = StringBuilder()
                try {
                    // Open the file from the assets folder
                    inputStream = assetManager.open("changelogs/$version.html")
                    // Create a BufferedReader for reading the file line by line
                    bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    // Read each line and append it to the StringBuilder
                    var line: String?
                    while ((bufferedReader?.readLine().also { line = it }) != null) {
                        stringBuilder.append(line).append("\n")
                    }
                    // Close the BufferedReader and the InputStream
                    bufferedReader?.close()
                    inputStream?.close()
                    // Get the TextView for displaying the file contents
                    // Set the text of the TextView to the file contents
                } catch (e: IOException) {
                    // Handle the exception
                    e.printStackTrace()
                }
                val changelog = stringBuilder.toString()

                val html =
                    HtmlCompat.fromHtml(changelog, HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH)
                allChangelog += html
            }
        }
        return allChangelog
    }

    private fun showChangelogAlert(thisVersion: String?, html: String, isFirstBoot: Boolean = false) {
        val alert = AlertDialog.Builder(context).also {
            it.setIcon(R.drawable.lollipop_logo)
            it.setTitle(context.getString(R.string.new_in_version, thisVersion))
            it.setMessage(html)
            it.setCancelable(false)
            it.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, _ ->
                if (isFirstBoot) {
                    preferenceStore.edit().putString(
                        context.getString(R.string.preferencestorelastversionchangelogshown),
                        thisVersion
                    ).apply()
                }
                dialog.dismiss()
            })
            if (isFirstBoot) {
                it.setOnDismissListener {
                    val alert = AlertDialog.Builder(context)
                        .setMessage(context.getString(R.string.you_can_read_again_the_changelog_in_the_settings_top_right_three_dots_buttons))
                        .setCancelable(false)
                        .setPositiveButton(
                            "OK",
                            DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
                        .create()
                        .show()
                }
            }
        }
        alert.create()
        alert.show()
    }

}