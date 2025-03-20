package com.example.chalkitup.ui.offline

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.bundled.BundledEmojiCompatConfig

class OfflineFirebase: Application() {
        override fun onCreate(){
            super.onCreate();
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            // EmojiCompat initialization, Without emoji edition i got incompatibility crashes, this fixed that issue
            val config = BundledEmojiCompatConfig(this)
            EmojiCompat.init(config)

        }
}