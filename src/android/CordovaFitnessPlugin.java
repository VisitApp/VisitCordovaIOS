package info.android.plugin.fitness;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.getvisitapp.google_fit.data.GoogleFitStatusListener;
import com.getvisitapp.google_fit.data.GoogleFitUtil;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

import io.cordova.fitnessappcordova.R;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaFitnessPlugin extends CordovaPlugin implements GoogleFitStatusListener {
    WebView mWebView;
    public static final String ACTIVITY_RECOGNITION = Manifest.permission.ACTIVITY_RECOGNITION;
    public static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 490;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 787;
    GoogleFitUtil googleFitUtil;
    CallbackContext callbackContext;

    String TAG = "mytag";
    Activity activity;
    boolean dailyDataSynced = false;
    boolean syncDataWithServer = false;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        Log.d(TAG, "plugin: pluginInitialize() calked");

        mWebView = (WebView) webView.getEngine().getView();
        activity = (Activity) this.cordova.getActivity();


    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("coolMethod")) {
            Log.d(TAG, "coolMethod() called");
            int arg1 = args.getInt(0);
            int arg2 = args.getInt(1);
            int result = arg1 + arg2;
            callbackContext.success("Result: " + result);
            return true;
        } else if (action.equals("loadVisitWebUrl")) {
            String baseUrl = args.getString(0);
            String default_client_id = args.getString(1);
            String authToken = args.getString(2);
            String userId = args.getString(3);

            Log.d(TAG, "baseUrl: " + baseUrl);
            Log.d(TAG, "defaultClientID: " + default_client_id);
            Log.d(TAG, "token: " + authToken);
            Log.d(TAG, "userId: " + userId);

            String magicLink = baseUrl + "star-health?token=" + authToken + "&id=" + userId;

            Log.d("mytag", "magicLink: " + magicLink);


            // Load the webpage
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FrameLayout rootLayout = (FrameLayout) activity.findViewById(android.R.id.content);

                    View progressBar = LayoutInflater.from(activity).inflate(R.layout.progress_bar_layout, null);
                    rootLayout.addView(progressBar);

                    mWebView.setWebViewClient(new SystemWebViewClient((SystemWebViewEngine) webView.getEngine()) {
                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            super.onPageStarted(view, url, favicon);
                            Log.d(TAG, "onPageStarted: " + url);

                        }


                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            Log.d(TAG, "onPageFinished: " + url);
                            rootLayout.removeView(progressBar);
                        }

                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            super.onReceivedError(view, errorCode, description, failingUrl);
                            Log.d(TAG, "onReceivedError: " + failingUrl);
                            rootLayout.removeView(progressBar);
                        }
                    });

                    googleFitUtil = new GoogleFitUtil(activity, CordovaFitnessPlugin.this, default_client_id, baseUrl);
                    mWebView.addJavascriptInterface(googleFitUtil.getWebAppInterface(), "Android");
                    googleFitUtil.init();

                    webView.showWebPage(magicLink, false, false, new HashMap<>());

                    mWebView.setDownloadListener(new DownloadListener() {
                        @Override
                        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                            Log.d("mytag", "downloadUrl:" + url + ",userAgent:" + userAgent + ",contentDisposition:" + contentDisposition + ",mimeType:" + mimeType + ",contentLength:" + contentLength);

                            webView.showWebPage(url, true, false, new HashMap<>());

                        }
                    });

                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                //this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Permission Denied"));
                return;
            }
        }

        switch (requestCode) {
            case ACTIVITY_RECOGNITION_REQUEST_CODE:
                Log.d(TAG, "ACTIVITY_RECOGNITION_REQUEST_CODE permission granted");
                cordova.setActivityResultCallback(this);
                googleFitUtil.askForGoogleFitPermission();
                break;
            case LOCATION_PERMISSION_REQUEST_CODE:
                break;
        }
    }


    /**
     * This get called from the webview when user taps on [Connect To Google Fit]
     */

    @Override
    public void askForPermissions() {
        if (dailyDataSynced) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cordova.requestPermissions(this, ACTIVITY_RECOGNITION_REQUEST_CODE, new String[]{ACTIVITY_RECOGNITION});
        } else {
            googleFitUtil.askForGoogleFitPermission();
        }
    }

    /**
     * 1A
     * This get called after user has granted all the fitness permission
     */
    @Override
    public void onFitnessPermissionGranted() {
        Log.d(TAG, "onFitnessPermissionGranted() called");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                googleFitUtil.fetchDataFromFit();
            }
        });
    }

    /**
     * 1B
     * This is used to load the Daily Fitness Data into the Home Tab webView.
     */
    @Override
    public void loadWebUrl(String url) {
        Log.d("mytag", "daily Fitness Data url:" + url);
        webView.loadUrl(url);
        dailyDataSynced = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult called. requestCode: " + requestCode + " resultCode: " + resultCode);

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == 4097 || requestCode == 1900) {
            cordova.setActivityResultCallback(this);
            googleFitUtil.onActivityResult(requestCode, resultCode, intent);

        }


    }


    /**
     * 2A
     * This get used for requesting data that are to be shown in detailed graph
     */

    @Override
    public void requestActivityData(String type, String frequency, long timestamp) {
        Log.d(TAG, "requestActivityData() called.");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (type != null && frequency != null) {
                    googleFitUtil.getActivityData(type, frequency, timestamp);
                }
            }
        });
    }

    /**
     * 2B
     * This get called when google fit return the detailed graph data that was requested previously
     */

    @Override
    public void loadGraphDataUrl(String url) {
        mWebView.evaluateJavascript(
                url,
                null
        );
    }

    @Override
    public void syncDataWithServer(String baseUrl, String authToken, long googleFitLastSync, long gfHourlyLastSync) {
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    googleFitUtil.sendDataToServer(baseUrl + "/", authToken, googleFitLastSync, gfHourlyLastSync);
                    syncDataWithServer = true;
                }
            });
        }
    }

    @Override
    public void askForLocationPermission() {
        if (!cordova.hasPermission(LOCATION_PERMISSION)) {
            cordova.requestPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, new String[]{LOCATION_PERMISSION});
        }
    }

    @Override
    public void closeVisitPWA() {
        Log.d(TAG,"closeVisitPWA() called");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PackageManager packageManager = activity.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(activity.getPackageName());
                ComponentName componentName = intent.getComponent();
                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                activity.startActivity(mainIntent);
                Runtime.getRuntime().exit(0);
            }
        });
    }


}