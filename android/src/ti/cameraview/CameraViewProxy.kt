/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2017 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.cameraview

import android.app.Activity
import org.appcelerator.kroll.annotations.Kroll.proxy
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.view.TiUIView


@proxy(creatableInModule = TicameraviewModule::class, propertyAccessors = ["color"])
class CameraViewProxy : TiViewProxy() {
    companion object {
        private const val LCAT = "CameraViewProxy"
    }

    override fun createView(activity: Activity): TiUIView {
        val view: TiUIView = CameraView(this)
        view.layoutParams.autoFillsHeight = true
        view.layoutParams.autoFillsWidth = true
        return view
    }
}