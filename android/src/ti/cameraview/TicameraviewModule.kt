/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2018 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.cameraview

import android.util.Log
import org.appcelerator.kroll.KrollModule
import org.appcelerator.kroll.annotations.Kroll
import org.appcelerator.titanium.TiApplication
import ti.cameraview.constant.Defaults

@Kroll.module(name="ticameraview", id="ti.cameraview")
class TicameraviewModule : KrollModule() {
    companion object {
		const val LCAT = "ti.cameraview"

		@JvmStatic
		@Kroll.onAppCreate
		fun onAppCreate(app: TiApplication) {
			Log.d(LCAT, "onAppCreate")
		}

		@Kroll.constant const val TORCH_MODE_OFF = Defaults.TORCH_MODE_OFF
		@Kroll.constant const val TORCH_MODE_ON = Defaults.TORCH_MODE_ON

		@Kroll.constant const val ASPECT_RATIO_4_3 = Defaults.ASPECT_RATIO_4_3
		@Kroll.constant const val ASPECT_RATIO_16_9 = Defaults.ASPECT_RATIO_16_9

		@Kroll.constant const val FLASH_MODE_AUTO = Defaults.FLASH_MODE_AUTO
		@Kroll.constant const val FLASH_MODE_ON = Defaults.FLASH_MODE_ON
		@Kroll.constant const val FLASH_MODE_OFF = Defaults.FLASH_MODE_OFF

		@Kroll.constant const val SCALE_TYPE_FIT_CENTER = Defaults.SCALE_TYPE_FIT_CENTER
		@Kroll.constant const val SCALE_TYPE_FILL_START = Defaults.SCALE_TYPE_FILL_START
		@Kroll.constant const val SCALE_TYPE_FILL_CENTER = Defaults.SCALE_TYPE_FILL_CENTER
		@Kroll.constant const val SCALE_TYPE_FILL_END = Defaults.SCALE_TYPE_FILL_END
		@Kroll.constant const val SCALE_TYPE_FIT_START = Defaults.SCALE_TYPE_FIT_START
		@Kroll.constant const val SCALE_TYPE_FIT_END = Defaults.SCALE_TYPE_FIT_END

		@Kroll.constant const val FOCUS_MODE_AUTO = Defaults.FOCUS_MODE_AUTO
		@Kroll.constant const val FOCUS_MODE_TAP = Defaults.FOCUS_MODE_TAP
	}

	override fun getApiName(): String? {
		return "ti.cameraview"
	}
}
