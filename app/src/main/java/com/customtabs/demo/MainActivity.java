package com.customtabs.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "CustomTabsDemo";

    private static final Uri URL = Uri.parse("https://master.ds1902kx4fxqy.amplifyapp.com");

    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;
    private Button mLaunchButton;
    private TextView mLogView;

    private boolean isChannelRequestDone = false;
    private final StringBuilder mLogs = new StringBuilder();

    @Override
    protected void onStart() {
        super.onStart();

        isChannelRequestDone = false;
        CustomTabsCallback callback = new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                appendToLog(eventToString(navigationEvent) + ": " + bundleToString(extras));
            }

            @Override
            public void extraCallback(@NonNull String callbackName, @Nullable Bundle args) {
                appendToLog("Extra: " + callbackName + ": " + bundleToString(args));
            }

            @Override
            public void onMessageChannelReady(@Nullable Bundle extras) {
                appendToLog("onMessageChannelReady: " + bundleToString(extras));
            }

            @Override
            public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                appendToLog("onPostMessage: " + message + ", "  + bundleToString(extras));
            }

            @Override
            public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin,
                                                       boolean result, @Nullable Bundle extras) {
                appendToLog("onRelationshipValidationResult: " + relation
                        + ", Uri: " + requestedOrigin + ", Result: "
                        + result + ", " + bundleToString(extras));
                if (result && !isChannelRequestDone) {
                    boolean accepted = mSession.requestPostMessageChannel(URL);
                    appendToLog("Request Post Message Channel Accepted: " + accepted);
                    isChannelRequestDone = true;
                }
            }
        };

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                    @NonNull CustomTabsClient client) {
                mSession = client.newSession(callback);
                client.warmup(0);
                mLaunchButton.setEnabled(true);
                mSession.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN,
                        URL, new Bundle());
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) { }
        };

        String packageName = CustomTabsClient.getPackageName(this, null);
        if (packageName == null) {
            Toast.makeText(this, "Can't find a Custom Tabs provider.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogView = findViewById(R.id.logs);
        mLaunchButton = findViewById(R.id.launch);
        mLaunchButton.setOnClickListener(view -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession).build();
            intent.launchUrl(this, URL);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection == null) return;
        unbindService(mConnection);
        mConnection = null;
        mLaunchButton.setEnabled(false);
    }

    private static String eventToString(int navigationEvent) {
        switch(navigationEvent) {
            case CustomTabsCallback.NAVIGATION_STARTED: return "Navigation Started";
            case CustomTabsCallback.NAVIGATION_FINISHED: return "Navigation Finished";
            case CustomTabsCallback.NAVIGATION_FAILED: return "Navigation Failed";
            case CustomTabsCallback.NAVIGATION_ABORTED: return "Navigation Aborted";
            case CustomTabsCallback.TAB_SHOWN: return "Tab Shown";
            case CustomTabsCallback.TAB_HIDDEN: return "Tab Hidden";
            default: return "Unknown Event";
        }
    }

    private static String bundleToString(Bundle bundle) {
        StringBuilder b = new StringBuilder();

        b.append("{");

        if (bundle != null) {
            boolean first = true;

            for (String key : bundle.keySet()) {
                if (!first) {
                    b.append(", ");
                }
                first = false;

                b.append(key);
                b.append(": ");
                b.append(bundle.get(key));
            }
        }

        b.append("}");

        return b.toString();
    }

    private void appendToLog(String log) {
        Log.d(TAG, log);

        mLogs.append(log);
        mLogs.append("\n");

        mLogView.setText(mLogs.toString());
    }
}