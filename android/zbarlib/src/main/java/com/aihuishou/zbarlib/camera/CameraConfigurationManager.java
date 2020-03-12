/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aihuishou.zbarlib.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
@SuppressWarnings("deprecation") // camera APIs
final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;
    private Point bestPreviewSize;
    private Point previewSizeOnScreen;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        screenResolution = theScreenResolution;
        Log.i(TAG, "Screen resolution in current orientation: " + screenResolution);
        cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
        Log.i(TAG, "Camera resolution: " + cameraResolution);
        bestPreviewSize = findBestPreviewSizeValue(parameters, screenResolution);
        Log.i(TAG, "Best available preview size: " + bestPreviewSize);

        boolean isScreenPortrait = screenResolution.x < screenResolution.y;
        boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

        if (isScreenPortrait == isPreviewSizePortrait) {
            previewSizeOnScreen = bestPreviewSize;
        } else {
            previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
        }
        Log.i(TAG, "Preview size on screen: " + previewSizeOnScreen);
    }

    void setDesiredCameraParameters(Camera camera, boolean safeMode) {

        Camera theCamera = camera;
        Camera.Parameters parameters = theCamera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // 默认使用自动对焦
        String focusMode = findSettableValue(
                parameters.getSupportedFocusModes(),
                Camera.Parameters.FOCUS_MODE_AUTO);

        // Maybe selected auto-focus but not available, so fall through here:
        if (!safeMode && focusMode == null) {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                    Camera.Parameters.FOCUS_MODE_MACRO,
                    Camera.Parameters.FOCUS_MODE_EDOF);
        }
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }

        parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

        theCamera.setParameters(parameters);

        theCamera.setDisplayOrientation(90);

        Camera.Parameters afterParameters = theCamera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            bestPreviewSize.x = afterSize.width;
            bestPreviewSize.y = afterSize.height;
        }
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }



    public static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if(rawSupportedSizes == null) {
            Log.w("CameraConfiguration", "Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            if(defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            } else {
                return new Point(defaultSize.width, defaultSize.height);
            }
        } else {

            double screenAspectRatio = (double)screenResolution.x / (double)screenResolution.y;
            int maxResolution = 0;
            Camera.Size maxResPreviewSize = null;
            Iterator var7 = rawSupportedSizes.iterator();

            while(var7.hasNext()) {
                Camera.Size size = (Camera.Size)var7.next();
                int realWidth = size.width;
                int realHeight = size.height;
                int resolution = realWidth * realHeight;
                if(resolution >= 153600) {
                    boolean isCandidatePortrait = realWidth < realHeight;
                    int maybeFlippedWidth = isCandidatePortrait?realHeight:realWidth;
                    int maybeFlippedHeight = isCandidatePortrait?realWidth:realHeight;
                    double aspectRatio = (double)maybeFlippedWidth / (double)maybeFlippedHeight;
                    double distortion = Math.abs(aspectRatio - screenAspectRatio);
                    if(distortion <= 0.15D) {
                        if(maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                            Point exactPoint = new Point(realWidth, realHeight);
                            Log.i("CameraConfiguration", "Found preview size exactly matching screen size: " + exactPoint);
                            return exactPoint;
                        }

                        if(resolution > maxResolution) {
                            maxResolution = resolution;
                            maxResPreviewSize = size;
                        }
                    }
                }
            }

            if(maxResPreviewSize != null) {
                Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
                Log.i("CameraConfiguration", "Using largest suitable preview size: " + largestSize);
                return largestSize;
            } else {
                Camera.Size defaultPreview = parameters.getPreviewSize();
                if(defaultPreview == null) {
                    throw new IllegalStateException("Parameters contained no preview size!");
                } else {
                    Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
                    Log.i("CameraConfiguration", "No suitable preview sizes, using default: " + defaultSize);
                    return defaultSize;
                }
            }
        }
    }

    private String findSettableValue(Collection<String> supportedValues, String... desiredValues) {

        if(supportedValues != null) {
            String[] var3 = desiredValues;
            int var4 = desiredValues.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String desiredValue = var3[var5];
                if(supportedValues.contains(desiredValue)) {
                    return desiredValue;
                }
            }
        }

        Log.i("CameraConfiguration", "No supported values match");
        return null;
    }

}
