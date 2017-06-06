/******************************************************************************
 *                                                                            *
 * Copyright 2017 Steven Foskett, Jimmy Ho, Ryan Porterfield                  *
 * Permission is hereby granted, free of charge, to any person obtaining a    *
 * copy of this software and associated documentation files (the "Software"), *
 * to deal in the Software without restriction, including without limitation  *
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,   *
 * and/or sell copies of the Software, and to permit persons to whom the      *
 * Software is furnished to do so, subject to the following conditions:       *
 *                                                                            *
 * The above copyright notice and this permission notice shall be included in *
 * all copies or substantial portions of the Software.                        *
 *                                                                            *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE*
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER     *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    *
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        *
 * DEALINGS IN THE SOFTWARE.                                                  *
 *                                                                            *
 ******************************************************************************/

package com.einzig.ipst2.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.einzig.ipst2.R;
import com.einzig.ipst2.database.DatabaseInterface;
import com.einzig.ipst2.parse.AuthenticatorTask;
import com.einzig.ipst2.parse.EmailParseTask;
import com.einzig.ipst2.parse.GetMailTask;
import com.einzig.ipst2.parse.MailBundle;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Main activity class which launches the app.
 * Contains all startup and initialization code.
 *
 * @author Steven Foskett
 * @since 2017-05-15
 */
public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    /** ??? */
    static public final String EXTRA_ACCOUNTNAME = "extra_accountname";
    /** Preferences key used for saving and retrieving the user's email address */
    static public final String EMAIL_KEY = "email";
    /** Preferences key for email folder containing portal emails */
    static final public String FOLDER_KEY = "mailFolder";
    /** Preferences key for sending date through Bundle */
    static public final String MOST_RECENT_DATE_KEY = "recentDate";
    /** Used for the default key when something is uninitialized */
    static public final String NULL_KEY = "uninitialized";
    /** Preferences key for sending a portal through a bundle */
    static public final String PORTAL_KEY = "portal";
    /** Preferences key for sending a portal list through Bundle */
    static public final String PORTAL_LIST_KEY = "portalList";
    /** The PortalSubmission being combined with a PortalResponded */
    static public final String PORTAL_SUBMISSION_KEY = "portalSubmission";
    /** The key for saving portal submission sort preference */
    static public final String SORT_KEY = "sort";
    /** Tag used for logging for this class */
    static public final String TAG = "IPST";
    /** Used to get the result of LoginActivity */
    static private final int LOGIN_ACTIVITY_CODE = 0;
    static final int REQUEST_CODE_EMAIL = 1;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    /**
     * Preferences for saving app settings
     */
    private SharedPreferences preferences;

    /**
     * MainActivity constructor, initialize variables.
     */
    public MainActivity() {
        preferences = null;
    }

    /**
     * Display an error message dialog
     *
     * @param title       Title for error dialog
     * @param messageText Message for error dialog
     */
    private void errorFoundMessage(final int title, final int messageText) {
        Log.d(TAG, "Displaying error message");
        runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(MainActivity.this).setTitle(title)
                        .setMessage(messageText)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
            }
        });
    }

    /**
     * Get user account if the user has already logged in.
     *
     * @return Account user logged in on.
     */
    private Account getAccount() {
        String email = preferences.getString(EMAIL_KEY, NULL_KEY);
        Log.i(TAG, "Getting account " + email);
        AccountManager manager = AccountManager.get(this);
        for (Account account : manager.getAccounts()) {
            Log.d(TAG, "Has account " + account.name);
            Log.d(TAG, "account name " + email);
            Log.d(TAG, "account type " + account.type);
            if (account.name.equalsIgnoreCase(email) && account.type.equalsIgnoreCase("com.google"))
                return account;
        }
        Log.d(TAG, "returning null account");
        return null;
    }

    /**
     * Get saved user preferences.
     */
    private void getPreferences() {
        preferences = getPreferences(MODE_PRIVATE);
        Log.i(TAG, EMAIL_KEY + " -> " + preferences.getString(EMAIL_KEY, NULL_KEY));
        Log.i(TAG, MOST_RECENT_DATE_KEY + " -> " + preferences.getString(MOST_RECENT_DATE_KEY, NULL_KEY));
        Log.i(TAG, SORT_KEY + " -> " + preferences.getString(SORT_KEY, NULL_KEY));
    }

    /**
     * Search through accounts on the user's device now that we have permission to do so.
     */
    public void gotPermission_accounts() {
        AccountManager manager = AccountManager.get(MainActivity.this);
        int numGoogAcct = 0;
        Account[] accountList = manager.getAccounts();
        for (Account a : accountList) {
            if (a.type.equals("com.google")) {
                numGoogAcct++;
            }
        }
        if (numGoogAcct == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.noaccountstitle);
            builder.setMessage(R.string.noaccountsmessage);//.  Would you like to log in manually?")
            builder.setCancelable(true);
            builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.show();
        } else {
            Intent intent = AccountManager.newChooseAccountIntent(null, null,
                    new String[]{"com.google"}, false, null, null, null, null);
            startActivityForResult(intent, LOGIN_ACTIVITY_CODE);

        }
    }

    /**
     * TODO (Steven): Delete if we don't need this. If we do need it, remove this
     * This method is a hook for background threads and async tasks that need to provide the
     * user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
                            .getConnectionStatusCode();
                    Dialog dialog = googleAPI.getErrorDialog(MainActivity.this,
                            statusCode,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    /**
     * Login if we have permission or get permission if we don't.
     *
     * @see MainActivity#gotPermission_accounts()
     */
    public void loginHitMethod() {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS)
                    == PackageManager.PERMISSION_GRANTED) {
                gotPermission_accounts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.GET_ACCOUNTS}, REQUEST_CODE_EMAIL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    *  Method for building/showing the ui once emails are parsed.
    */
    public void buildUIAfterParsing() {
        DatabaseInterface db = new DatabaseInterface(this);
        long acceptedCount = db.getAcceptedCount();
        long pendingCount = db.getPendingCount();
        long rejectedCount = db.getRejectedCount();
        long totalCount = acceptedCount + pendingCount + rejectedCount;
        //Do stuff with these numbers.  Set bars with heights based on percentages. set button listeners here
        findViewById(R.id.progress_view_mainactivity).setVisibility(View.INVISIBLE);
        findViewById(R.id.mainui_mainactivity).setVisibility(View.VISIBLE);
        formatUI(acceptedCount, rejectedCount, pendingCount);
        RadioButton todayButton = (RadioButton) findViewById(R.id.todaytab_mainactivity);
        if (todayButton != null)
            todayButton.setOnCheckedChangeListener(this);
        RadioButton weekButton = (RadioButton) findViewById(R.id.weektab_mainactivity);
        if (weekButton != null)
            weekButton.setOnCheckedChangeListener(this);
        RadioButton monthButton = (RadioButton) findViewById(R.id.monthtab_mainactivity);
        if (monthButton != null)
            monthButton.setOnCheckedChangeListener(this);
        RadioButton allButton = (RadioButton) findViewById(R.id.alltab_mainactivity);
        if (allButton != null)
            allButton.setOnCheckedChangeListener(this);
    }

    /*
    * Method to format UI when changing radio buttons
     */
    public void formatUI(long accepted, long rejected, long pending)
    {
        ((TextView) findViewById(R.id.pendingtext_mainactivity)).setText(String.format(Locale.getDefault(), "%d", pending));
        ((TextView) findViewById(R.id.acceptedtext_mainactivity)).setText(String.format(Locale.getDefault(), "%d", accepted));
        ((TextView) findViewById(R.id.rejectedtext_mainactivity)).setText(String.format(Locale.getDefault(), "%d", rejected));

        setLayoutParamsGraphBars((int) ((pending * 100) / (accepted + rejected + pending)), (LinearLayout) findViewById(R.id.pendinggraph_mainactivity));
        setLayoutParamsGraphBars((int) ((rejected * 100) / (accepted + rejected + pending)), (LinearLayout) findViewById(R.id.rejectedgraph_mainactivity));
        setLayoutParamsGraphBars((int) ((accepted * 100) / (accepted + rejected + pending)), (LinearLayout) findViewById(R.id.acceptedgraph_mainactivity));
        ((TextView) findViewById(R.id.percentaccepted_mainactivity)).setText(String.format(Locale.getDefault(),"%d%%", (int) ((accepted * 100) / (accepted + rejected + pending))));
    }

    /*
    * Method to set layout params for graph bars
     */
    public void setLayoutParamsGraphBars(int height, LinearLayout layout)
    {
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, getResources().getDisplayMetrics());
        layout.setLayoutParams(params);
    }

    /*
    *  Method for when radiobuttons are clicked
    */
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        RadioButton tempButton = (RadioButton) view;
        tempButton.setTypeface(null, Typeface.BOLD);
        /*SparseIntArray textMap = new SparseIntArray();
        textMap.put(R.id.alltab_mainactivity, R.string.viewlistall);
        textMap.put(R.id.monthtab_mainactivity, R.string.viewlistmonth);
        textMap.put(R.id.todaytab_mainactivity, R.string.viewlisttoday);
        textMap.put(R.id.weektab_mainactivity, R.string.viewlistweek);
        if (checked) {
            Button buttonTextSet = (Button) findViewById(R.id.viewlist_mainactivity);
            buttonTextSet.setText(textMap.get(view.getId()));
        }*/
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.todaytab_mainactivity:
                if (checked) {
                    //TODO set text of labels what the today things are.

                    Button buttonTextSet = (Button) findViewById(R.id.viewlist_mainactivity);
                    buttonTextSet.setText(R.string.viewlisttoday);
                }
                break;
            case R.id.weektab_mainactivity:
                if (checked) {

                    Button buttonTextSet = (Button) findViewById(R.id.viewlist_mainactivity);
                    buttonTextSet.setText(R.string.viewlistweek);
                }
                break;
            case R.id.monthtab_mainactivity:
                if (checked) {

                    Button buttonTextSet = (Button) findViewById(R.id.viewlist_mainactivity);
                    buttonTextSet.setText(R.string.viewlistmonth);
                }
                break;
            case R.id.alltab_mainactivity:
                if (checked) {


                    Button buttonTextSet = (Button) findViewById(R.id.viewlist_mainactivity);
                    buttonTextSet.setText(R.string.viewlistall);
                }
                break;
        }
    }

    /*
    *  This method fixes formatting when radio boxes are changed
    * */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        buttonView.setTypeface(isChecked ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        buttonView.setBackground(isChecked ? getResources().getDrawable(R.drawable.cell_shape_radio)
                                            : getResources().getDrawable(R.drawable.cell_shape_radio_clear));
    }


    /*
     * Callback method if an activity is started for result via startActivityForResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + ") -> " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != LOGIN_ACTIVITY_CODE || resultCode != RESULT_OK) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(EMAIL_KEY, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
        editor.apply();
        Log.d(TAG, "Got account name " + data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
        parseEmail();
        Account me = getAccount();
        if (me != null) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // SHOW LOADING THINGIES
            findViewById(R.id.progress_view_mainactivity).setVisibility(View.VISIBLE);
            findViewById(R.id.gmail_login_button).setVisibility(View.INVISIBLE);
        } else {
            errorFoundMessage(R.string.accountnotfoundtitle, R.string.accountnotfoundmessage);
        }
    }

    /*
     * Called on startup by Android after the app starts and resources are available.
     * Used for more advanced initializations.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        getPreferences();
        Button gmail_login_button = (Button) findViewById(R.id.gmail_login_button);
        if (gmail_login_button != null) {
            gmail_login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginHitMethod();
                }
            });
        }
        if (!preferences.getString(EMAIL_KEY, NULL_KEY).equalsIgnoreCase(NULL_KEY)) {
            findViewById(R.id.progress_view_mainactivity).setVisibility(View.VISIBLE);
            findViewById(R.id.gmail_login_button).setVisibility(View.INVISIBLE);
            parseEmail();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_mainactivity:
                startActivity(new Intent(this, SettingsActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Provides the results of permission requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_EMAIL && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gotPermission_accounts();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * Parse emails from Niantic
     */
    private void parseEmail() {
        Account account = getAccount();
        if (account != null) {
            try {
                String token = new AuthenticatorTask(this, account).execute().get();
                MailBundle bundle = new GetMailTask(this, account, token).execute().get();
                new EmailParseTask(this, bundle).execute();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

}
