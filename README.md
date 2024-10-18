![icon](https://r-a-d.io/assets/logo_image_small.png)![logo](https://r-a-d.io/assets/logotitle_2.png)

# R/a/dio, the brand-new Android app!


[![](_README_assets/GetItOnGooglePlay_Badge_Web_color_English.png)
](https://play.google.com/store/apps/details?id=io.r_a_d.radio2)


R/a/dio is a webradio that was founded 7+ years ago with the intention of bringing you a stream of (mostly) high quality anime music, and we keep that up to this day! You can always drop by and visit our website https://r-a-d.io for more information.

![the gif](./demo.gif)

### Features

- Listen to R/a/dio!
- Fine-tune the volume in the app to go lower than the lowest volume of Android!
- Adapt to all screens: small phone, big tablet, horizontal, vertical, split screen!
- Start and stop the stream by headphones plugging/unplugging, or with a bluetooth headset!
- Check last played and queued tracks!
- Request any title to the AFK streamer Hanyuu-sama!
- Display and request your favorite tracks! There's even a random-request button (**.ra f**) available!
- Chat in the IRC with the embedded WebIRC!
- Wake up with the sound of R/a/dio with the built-in Alarm Clock feature! Don't worry: if there is no network, it will play a default sound instead.
- Never miss a stream with the Streamer Notification Service! (**Warning: this feature polls the server regularly and consumes battery. It is MUCH MORE RECOMMENDED to register to Hanyuu-sama's updates on Twitter and use the Twitter app. But if for some reason you don't (or can't) use the Twitter App, this should get you covered.**)
- Supports lastFM, LibreFM and Listenbrainz scrobblers with [Pano Scrobbler](https://play.google.com/store/apps/details?id=com.arn.scrobble)
- Snooze the alarm! You can set up the snooze duration you want, or avoid being tempted and disable snooze altogether. When ringing, a special notification design will display bigger buttons with text instead of icons.
- Sleep with the sound of R/a/dio! You can set up a timer to stop the app after any amount of minutes. When the timer approaches, the sound will gradually fade out.

As always, thanks for listening!


# Releases

## Release 2.3.4

see tag v2.3.4

Contains fixes for Android 15 (released on Pixel devices on October 15, 2024)

- Android 15 FIX: edge-to-edge display disabled
  + With the new edge-to-edge policy (see this: [https://developer.android.com/develop/ui/views/layout/edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge)) the top and bottom borders were hidden by the Android UI (taskbar, bottom bar). This is now fixed and the app displays outside of these borders like before.
- Android 15 FIX: Audiofocus Request bypassed for alarm
  + The alarm playback now makes an AudiofocusRequest on Android 15, which is conflicting with the current way the focus request is handled. The audiofocus request is bypassed by the app when it's starting the alarm.
- Android 15 FIX: streamer service re-launches at boot
  + BootBroadcastReceiver (broadcast receiver that starts at boot time to start the Streamer Notification Service) now has a flag to let it start when your phone starts.
- Chat tab removed
  + The web IRC is long dead, so the tab is removed.
- Last Played and Queue show time
  + The Last Played list and Queue list now show the time at which the song was played, and the time the next songs will be played.

More dependencies updated too.

## Release 2.3.3

see tag v2.3.3

- Updated dependencies to compile with SDK 35 (Android 15)
- Updated exoplayer version to 2.19.1 (latest version before media3)
- Many small fixes here and there.

## Release 2.2.0

Was a long time ago.. No feature change, only fixes.

Numbering changed to start at 2.x.y.

## Release 2.0.0

Features added: 
- Alarm snooze
- Sleep timer
- Special notification design for alarm
- Added pull-to-refresh on favorites
- Added link to help on how to use favorites in IRC
- Added settings to control API fetch frequency when playback is stopped

Bug fixes:
- Better handling of queue, avoid duplicates
- Should handle correctly when the stream is down ("ed" playing)
- Update the streamer picture in notification at startup
- UI updates correctly when app is opened after alarm wake-up
- Backup alarm sound is now correctly triggered only when necessary
