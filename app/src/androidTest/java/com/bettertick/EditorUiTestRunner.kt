package com.bettertick

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** UI fixtures use no account, Firebase services, workers, overlays or network data. */
class EditorUiTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
