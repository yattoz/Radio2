package io.r_a_d.radio2

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.r_a_d.radio2.playerstore.Song

fun CreateClipboardListener(context: Context, song: Song): View.OnLongClickListener
{
val setClipboardListener: View.OnLongClickListener = View.OnLongClickListener {
    val text = song.artist.value + " - " + song.title.value
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
    val snackBarLength = if (preferenceStore.getBoolean("snackbarPersistent", true))
        Snackbar.LENGTH_INDEFINITE
    else Snackbar.LENGTH_LONG
    val snackBar = Snackbar.make(it, "", snackBarLength)

    if (snackBarLength == Snackbar.LENGTH_INDEFINITE)
        snackBar.setAction("OK") { snackBar.dismiss() }

    snackBar.behavior = BaseTransientBottomBar.Behavior().apply {
        setSwipeDirection(BaseTransientBottomBar.Behavior.SWIPE_DIRECTION_ANY)
    }
    snackBar.setText(context.getString(R.string.song_to_clipboard))
    snackBar.show()
    true
    }
    return setClipboardListener
}
