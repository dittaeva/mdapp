package net.olejon.mdapp;

/*

Copyright 2015 Ole Jon Bjørkum

This file is part of LegeAppen.

LegeAppen is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

LegeAppen is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LegeAppen.  If not, see <http://www.gnu.org/licenses/>.

*/

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicationActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener
{
    private final Activity mActivity = this;

    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;

    private InputMethodManager mInputMethodManager;

    private Spinner mSpinner;
    private ProgressBar mProgressBar;
    private RelativeLayout mRelativeLayout;
    private LinearLayout mLinearVideoLayout;
    private LinearLayout mLinearFindInTextLayout;
    private EditText mEditTextFindInText;
    private TextView mTextViewFindInTextCount;
    private ImageButton mImageButtonFindInText;
    private View mCustomView;
    private ListView mListView;
    private WebView mWebView;

    private Menu mMenu;

    private MenuItem favoriteMenuItem;

    private WebChromeClient mWebChromeClient;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private long medicationId;

    private String medicationName;
    private String medicationManufacturer;
    private String medicationManufacturerUri;
    private String medicationType;
    private String medicationPrescriptionGroup;
    private String medicationPrescriptionGroupDescription;
    private String medicationContentSections;
    private String medicationUri;
    private String medicationPicturesUri;
    private String medicationPatientUri;
    private String medicationSpcUri;
    private String medicationSubstances;

    private String[] medicationSubstancesStringArray;

    private boolean mLinearLayoutAnimationHasBeenShown = false;
    private boolean mSetSection = true;

    private int mSectionIndex = 0;

    // Create activity
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Settings
        PreferenceManager.setDefaultValues(mContext, R.xml.settings, false);

        // Database
        mSqLiteDatabase = new MedicationsFavoritesSQLiteHelper(mContext).getWritableDatabase();

        // Intent
        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND))
        {
            String uri = intent.getStringExtra(Intent.EXTRA_TEXT).replace("no/medisin/", "no/m/medisin/").replace("?json", "");

            medicationId = mTools.getMedicationIdFromUri(uri);
        }
        else if(intentAction != null && intentAction.equals(Intent.ACTION_VIEW))
        {
            String uri = intent.getData().toString().replace("no/medisin/", "no/m/medisin/").replace("?json", "");

            medicationId = mTools.getMedicationIdFromUri(uri);
        }
        else
        {
            medicationId = intent.getLongExtra("id", 0);
        }

        if(medicationId == 0)
        {
            mTools.showToast(getString(R.string.medication_could_not_find_medication), 1);

            finish();
        }
        else
        {
            // Input manager
            mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            // Layout
            setContentView(R.layout.activity_medication);

            mRelativeLayout = (RelativeLayout) findViewById(R.id.medication_inner_layout);
            mLinearVideoLayout = (LinearLayout) findViewById(R.id.medication_video_layout);
            mLinearFindInTextLayout = (LinearLayout) findViewById(R.id.medication_find_in_text_layout);
            mEditTextFindInText = (EditText) findViewById(R.id.medication_find_in_text_search);
            mTextViewFindInTextCount = (TextView) findViewById(R.id.medication_find_in_text_search_count);
            mImageButtonFindInText = (ImageButton) findViewById(R.id.medication_find_in_text_search_next);

            // Toolbar
            Toolbar toolbar = (Toolbar) findViewById(R.id.medication_toolbar);

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Spinner
            mSpinner = (Spinner) findViewById(R.id.medication_toolbar_spinner);
            mSpinner.setOnItemSelectedListener(this);

            // Progress bar
            mProgressBar = (ProgressBar) findViewById(R.id.medication_toolbar_progressbar);

            // List
            mListView = (ListView) findViewById(R.id.medication_content_list);

            View listViewHeader = getLayoutInflater().inflate(R.layout.activity_medication_content_list_subheader, mListView, false);
            mListView.addHeaderView(listViewHeader, null, false);

            // Web view
            mWebView = (WebView) findViewById(R.id.medication_content);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(false);

            mWebView.setWebViewClient(new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    if(url.equals("http://www.felleskatalogen.no/m/medisin/interaksjon"))
                    {
                        Intent intent = new Intent(mContext, InteractionsActivity.class);
                        intent.putExtra("search", medicationName);
                        startActivity(intent);
                    }
                    else if(url.matches("^http://www.felleskatalogen.no/m/medisin/[^-]+-.*?-\\d+$") && !url.contains("/blaarev-register/"))
                    {
                        mTools.getMedicationWithFullContent(url);
                    }
                    else if(url.matches("^https?://.*?\\.pdf$"))
                        {
                            mTools.showToast(getString(R.string.medication_downloading_pdf), 1);

                            mTools.downloadFile(medicationName, url);
                        }
                        else
                        {
                            Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                            intent.putExtra("title", medicationName);
                            intent.putExtra("uri", url);
                            startActivity(intent);
                        }

                    return true;
                }
            });

            mWebChromeClient = new WebChromeClient()
            {
                @Override
                public void onProgressChanged(WebView view, int newProgress)
                {
                    if(newProgress == 100)
                    {
                        if(!mLinearLayoutAnimationHasBeenShown)
                        {
                            mLinearLayoutAnimationHasBeenShown = true;

                            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
                            mRelativeLayout.startAnimation(animation);

                            mRelativeLayout.setVisibility(View.VISIBLE);
                        }

                        if(mSetSection && savedInstanceState != null)
                        {
                            mSetSection = false;

                            mSectionIndex = savedInstanceState.getInt("section_index");

                            setSection(false);
                        }
                    }
                }

                @Override
                public void onShowCustomView(View view, CustomViewCallback callback)
                {
                    mLinearVideoLayout.addView(view);

                    mCustomView = view;
                    mCustomViewCallback = callback;

                    mRelativeLayout.setVisibility(View.GONE);
                    mLinearVideoLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onHideCustomView()
                {
                    if(mCustomView != null)
                    {
                        mLinearVideoLayout.removeView(mCustomView);
                        mLinearVideoLayout.setVisibility(View.GONE);

                        mCustomView = null;
                        mCustomViewCallback.onCustomViewHidden();

                        mRelativeLayout.setVisibility(View.VISIBLE);
                    }
                }
            };

            mWebView.setWebChromeClient(mWebChromeClient);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mWebView.setFindListener(new WebView.FindListener()
                {
                    @Override
                    public void onFindResultReceived(int i, int i2, boolean b)
                    {
                        if(i2 == 0)
                        {
                            mTextViewFindInTextCount.setVisibility(View.GONE);
                            mImageButtonFindInText.setVisibility(View.GONE);

                            mTools.showToast("Ingen treff", 1);
                        }
                        else
                        {
                            int active = i + 1;

                            mTextViewFindInTextCount.setText(String.valueOf(active)+"/"+String.valueOf(i2));
                            mTextViewFindInTextCount.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            mWebView.addJavascriptInterface(new JavaScriptInterface(mContext), "Android");

            mEditTextFindInText.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
                {
                    String find = mEditTextFindInText.getText().toString().trim();

                    if(find.equals(""))
                    {
                        mTextViewFindInTextCount.setVisibility(View.GONE);
                        mImageButtonFindInText.setVisibility(View.GONE);
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

                @Override
                public void afterTextChanged(Editable editable) { }
            });

            mEditTextFindInText.setOnEditorActionListener(new TextView.OnEditorActionListener()
            {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
                {
                    if(i == EditorInfo.IME_ACTION_NEXT || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                    {
                        mInputMethodManager.toggleSoftInputFromWindow(mEditTextFindInText.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                        String find = mEditTextFindInText.getText().toString().trim();

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                        {
                            mWebView.findAllAsync(find);
                        }
                        else
                        {
                            mWebView.findAll(find);
                        }

                        mImageButtonFindInText.setVisibility(View.VISIBLE);

                        return true;
                    }

                    return false;
                }
            });

            mImageButtonFindInText.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    mWebView.findNext(true);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) mWebView.loadUrl("javascript:scrollToPositionAfterFindInText();");
                }
            });
        }
    }

    // Resume activity
    @Override
    protected void onResume()
    {
        super.onResume();

        mWebView.resumeTimers();
    }

    // Pause activity
    @Override
    protected void onPause()
    {
        super.onPause();

        mWebView.loadUrl("javascript:pauseVideos();");

        mWebView.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) CookieSyncManager.getInstance().sync();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putInt("section_index", mSectionIndex);
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    // Back button
    @Override
    public void onBackPressed()
    {
        if(mCustomView != null)
        {
            mWebChromeClient.onHideCustomView();
        }
        else if(mLinearFindInTextLayout.getVisibility() == View.VISIBLE)
        {
            mWebView.clearMatches();
            mLinearFindInTextLayout.setVisibility(View.GONE);
            mEditTextFindInText.setText("");
            mTextViewFindInTextCount.setVisibility(View.GONE);
            mImageButtonFindInText.setVisibility(View.GONE);
        }
        else
        {
            super.onBackPressed();
        }
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_medication, menu);

        mMenu = menu;

        favoriteMenuItem = menu.findItem(R.id.medication_menu_star);

        //GetMedicationTask getMedicationTask = new GetMedicationTask();
        //getMedicationTask.execute(medicationId);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                Intent intent = NavUtils.getParentActivityIntent(this);

                if(NavUtils.shouldUpRecreateTask(this, intent))
                {
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities();
                }
                else
                {
                    NavUtils.navigateUpTo(this, intent);
                }

                return true;
            }
            case R.id.medication_menu_star:
            {
                //addToFavorites(medicationName, medicationManufacturer, medicationType, medicationPrescriptionGroup, medicationUri);
                return true;
            }
            case R.id.medication_menu_interactions:
            {
                Intent intent = new Intent(mContext, InteractionsActivity.class);
                intent.putExtra("search", medicationName);
                startActivity(intent);
                return true;
            }
            case R.id.medication_menu_pictures_uri:
            {
                if(medicationPicturesUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_pictures), 1);
                }
                else
                {
                    Intent intent = new Intent(mContext, MedicationPicturesActivity.class);
                    intent.putExtra("name", medicationName);
                    intent.putExtra("uri", medicationPicturesUri);
                    mContext.startActivity(intent);
                }

                return true;
            }
            case R.id.medication_menu_manufacturer_uri:
            {
                //getManufacturerFromUri(medicationManufacturerUri);
                return true;
            }
            case R.id.medication_menu_patient_uri:
            {
                if(medicationPatientUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_package_inserts), 1);
                }
                else
                {
                    getPackageInserts();
                }

                return true;
            }
            case R.id.medication_menu_spc_uri:
            {
                if(medicationSpcUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_spc), 1);
                }
                else
                {
                    Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                    intent.putExtra("title", medicationName);
                    intent.putExtra("uri", medicationSpcUri);
                    startActivity(intent);
                }

                return true;
            }
            case R.id.medication_menu_find_in_text:
            {
                boolean hideFindInTextTipDialog = mTools.getSharedPreferencesBoolean("MEDICATION_HIDE_FIND_IN_TEXT_TIP_DIALOG");

                if(!hideFindInTextTipDialog)
                {
                    new MaterialDialog.Builder(mContext).title(getString(R.string.medication_find_in_text_tip_dialog_title)).content(getString(R.string.medication_find_in_text_tip_dialog_message)).positiveText(getString(R.string.medication_find_in_text_tip_dialog_positive_button)).callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            mTools.setSharedPreferencesBoolean("MEDICATION_HIDE_FIND_IN_TEXT_TIP_DIALOG", true);
                        }
                    }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                }

                mLinearFindInTextLayout.setVisibility(View.VISIBLE);
                mEditTextFindInText.requestFocus();

                mInputMethodManager.toggleSoftInputFromWindow(mEditTextFindInText.getApplicationWindowToken(), InputMethodManager.SHOW_IMPLICIT, 0);

                return true;
            }
            case R.id.medication_menu_note:
            {
                Intent intent = new Intent(mContext, NotesEditActivity.class);
                intent.putExtra("title", medicationName);
                startActivity(intent);
                return true;
            }
            case R.id.medication_menu_print:
            {
                mTools.printDocument(mWebView, medicationName);
                return true;
            }
            case R.id.medication_menu_nlh:
            {
                Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                intent.putExtra("title", medicationName);
                intent.putExtra("uri", "http://m.legemiddelhandboka.no/s%C3%B8keresultat/?q="+medicationName);
                startActivity(intent);
                return true;
            }
            case R.id.medication_menu_uri:
            {
                mTools.openUri(medicationUri);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        if(i > 0)
        {
            mSectionIndex = i;

            setSection(true);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) { }

    // Section
    private void setSection(boolean animate)
    {
        try
        {
            if(mSectionIndex > 0)
            {
                JSONObject sectionJsonObject = new JSONArray(medicationContentSections).getJSONObject(mSectionIndex - 1);

                mListView.setSelection(mSectionIndex);

                String id = sectionJsonObject.getString("id");

                mWebView.loadUrl("javascript:scrollToSection('"+id+"', "+animate+");");
            }
        }
        catch(Exception e)
        {
            Log.e("MedicationActivity", Log.getStackTraceString(e));
        }
    }

    // Favorite
    /*private void addToFavorites(final String name, final String manufacturer, final String type, final String prescription_group, final String uri)
    {
        boolean medicationIsFavorite = medicationIsFavorite(medicationUri);

        if(medicationIsFavorite)
        {
            mSqLiteDatabase.delete(MedicationsFavoritesSQLiteHelper.TABLE, MedicationsFavoritesSQLiteHelper.COLUMN_URI+" = "+mTools.sqe(uri), null);

            favoriteMenuItem.setIcon(R.drawable.ic_star_outline_white_24dp);
        }
        else
        {
            ContentValues contentValues = new ContentValues();

            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_NAME, name);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_MANUFACTURER, manufacturer);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_TYPE, type);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_PRESCRIPTION_GROUP, prescription_group);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_URI, uri);

            mSqLiteDatabase.insert(MedicationsFavoritesSQLiteHelper.TABLE, null, contentValues);

            favoriteMenuItem.setIcon(R.drawable.ic_star_white_24dp);

            SnackbarManager.show(Snackbar.with(mActivity).text(getString(R.string.medication_saved_to_favorites)).colorResource(R.color.dark).actionLabel(getString(R.string.snackbar_undo)).actionLabelTypeface(Typeface.DEFAULT_BOLD).actionColorResource(R.color.orange).actionListener(new ActionClickListener()
            {
                @Override
                public void onActionClicked(Snackbar snackbar)
                {
                    addToFavorites(name, manufacturer, type, prescription_group, uri);
                }
            }));
        }

        mTools.updateWidget();
    }

    private boolean medicationIsFavorite(String uri)
    {
        Cursor cursor = mSqLiteDatabase.query(MedicationsFavoritesSQLiteHelper.TABLE, null, MedicationsFavoritesSQLiteHelper.COLUMN_URI+" = "+mTools.sqe(uri), null, null, null, null);

        long id = 0;

        if(cursor.moveToFirst())
        {
            try
            {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID));
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }

        cursor.close();

        return (id != 0);
    }*/

    // Manufacturer
    /*private void getManufacturerFromUri(String uri)
    {
        SQLiteDatabase sqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, null, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_URI+" = "+mTools.sqe(uri), null, null, null, null);

        long id = 0;

        if(cursor.moveToFirst())
        {
            try
            {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_ID));
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }

        if(id == 0)
        {
            mTools.showToast(mContext.getString(R.string.medication_could_not_find_manufacturer), 1);
        }
        else
        {
            Intent intent = new Intent(mContext, ManufacturerActivity.class);
            intent.putExtra("id", id);
            mContext.startActivity(intent);
        }

        cursor.close();
        sqLiteDatabase.close();
    }*/

    // Package insert
    private void getPackageInserts()
    {
        if(mTools.isDeviceConnected())
        {
            try
            {
                mProgressBar.setVisibility(View.VISIBLE);

                RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(getString(R.string.project_website_uri)+"api/1/felleskatalogen/medications/package-inserts/?uri="+URLEncoder.encode(medicationPatientUri, "utf-8"), new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response)
                    {
                        mProgressBar.setVisibility(View.GONE);

                        final int packageInsertsCount = response.length();

                        if(packageInsertsCount == 1)
                        {
                            try
                            {
                                JSONObject packageInsertJsonObject = response.getJSONObject(0);

                                Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                                intent.putExtra("title", medicationName);
                                intent.putExtra("uri", packageInsertJsonObject.getString("uri"));
                                startActivity(intent);
                            }
                            catch(Exception e)
                            {
                                Log.e("MedicationActivity", Log.getStackTraceString(e));
                            }
                        }
                        else
                        {
                            final String[] packageInsertsNames = new String[packageInsertsCount];
                            final String[] packageInsertsUris = new String[packageInsertsCount];

                            for(int i = 0; i < packageInsertsCount; i++)
                            {
                                try
                                {
                                    JSONObject packageInsertJsonObject = response.getJSONObject(i);

                                    packageInsertsNames[i] = packageInsertJsonObject.getString("name");
                                    packageInsertsUris[i] = packageInsertJsonObject.getString("uri");
                                }
                                catch(Exception e)
                                {
                                    Log.e("MedicationActivity", Log.getStackTraceString(e));
                                }
                            }

                            new MaterialDialog.Builder(mContext).title(getString(R.string.medication_package_inserts_dialog_title)).items(packageInsertsNames).itemsCallback(new MaterialDialog.ListCallback()
                            {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence)
                                {
                                    Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                                    intent.putExtra("title", medicationName);
                                    intent.putExtra("uri", packageInsertsUris[i]);
                                    startActivity(intent);
                                }
                            }).itemColorRes(R.color.dark_blue).show();
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        mProgressBar.setVisibility(View.GONE);

                        Log.e("MedicationActivity", error.toString());
                    }
                });

                jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                requestQueue.add(jsonArrayRequest);
            }
            catch(Exception e)
            {
                mTools.showToast(getString(R.string.medication_could_not_get_package_insert), 1);

                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }
        else
        {
            mTools.showToast(getString(R.string.device_not_connected), 1);
        }
    }

    // Get medication
    /*private class GetMedicationTask extends AsyncTask<Long, Void, HashMap<String, String>>
    {
        @Override
        protected void onPostExecute(HashMap<String, String> medication)
        {
            // Medication details
            final String medicationAtcCodes = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ATC_CODES);
            final String medicationBluePrescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_BLUE_PRESCRIPTION);
            final String medicationTriangle = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_TRIANGLE);
            final String medicationBlackTriangle = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_BLACK_TRIANGLE);
            final String medicationDopingStatus = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS);
            final String medicationDopingStatusDescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS_DESCRIPTION);
            final String medicationDopingStatusUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS_URI);
            final String medicationSchengenCertificate = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_SCHENGEN_CERTIFICATE);
            final String medicationContent = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_CONTENT);
            final String medicationFullContentReference = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_FULL_CONTENT_REFERENCE);
            final String medicationPoisoningUris = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_POISONING_URIS);

            medicationName = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME);
            medicationManufacturer = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER);
            medicationManufacturerUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER_URI);
            medicationType = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_TYPE);
            medicationPrescriptionGroup = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PRESCRIPTION_GROUP);
            medicationPrescriptionGroupDescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PRESCRIPTION_GROUP_DESCRIPTION);
            medicationContentSections = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_CONTENT_SECTIONS);
            medicationUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI);
            medicationPicturesUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PICTURES_URI);
            medicationPatientUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PATIENT_URI);
            medicationSpcUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_SPC_URI);
            medicationSubstances = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_SUBSTANCES);

            // Medication sections
            try
            {
                JSONArray jsonArray = new JSONArray(medicationContentSections);

                ArrayList<String> spinnerArrayList = new ArrayList<>();
                ArrayList<String> listArrayList = new ArrayList<>();

                spinnerArrayList.add(getString(R.string.medication_choose_section));

                for(int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    spinnerArrayList.add(jsonObject.getString("name"));
                    listArrayList.add(jsonObject.getString("name"));
                }

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getSupportActionBar().getThemedContext(), R.layout.toolbar_spinner_header, spinnerArrayList);
                spinnerArrayAdapter.setDropDownViewResource(R.layout.toolbar_spinner_item);

                mSpinner.setAdapter(spinnerArrayAdapter);

                ArrayAdapter<String> listArrayAdapter = new ArrayAdapter<>(mContext, R.layout.activity_medication_content_list_item, listArrayList);

                mListView.setAdapter(listArrayAdapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                    {
                        mSectionIndex = i;

                        setSection(true);
                    }
                });
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }

            // Medication is favorite?
            if(medicationIsFavorite(medicationUri)) favoriteMenuItem.setIcon(R.drawable.ic_star_white_24dp);

            // Medication substances, poisoning information and ATC codes
            try
            {
                int order = 3;

                if(!medicationSubstances.equals("[\"\"]"))
                {
                    order++;

                    MenuItem menuItem = mMenu.add(Menu.NONE, Menu.NONE, order, getString(R.string.medication_menu_substances));

                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                    {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem)
                        {
                            try
                            {
                                final JSONArray medicationSubstancesJsonArray = new JSONArray(medicationSubstances);

                                int medicationSubstancesJsonArrayLength = medicationSubstancesJsonArray.length();

                                medicationSubstancesStringArray = new String[medicationSubstancesJsonArrayLength];

                                for(int i = 0; i < medicationSubstancesJsonArrayLength; i++)
                                {
                                    medicationSubstancesStringArray[i] = medicationSubstancesJsonArray.getString(i).replace(" ", "");
                                }

                                new MaterialDialog.Builder(mContext).title(getString(R.string.medication_substances_dialog_title)).items(medicationSubstancesStringArray).itemsCallback(new MaterialDialog.ListCallback()
                                {
                                    @Override
                                    public void onSelection(MaterialDialog materialDialog, View view, int n, CharSequence charSequence)
                                    {
                                        mTools.getSubstance(medicationSubstancesStringArray[n]);
                                    }
                                }).itemColorRes(R.color.dark_blue).show();
                            }
                            catch(Exception e)
                            {
                                Log.e("MedicationActivity", Log.getStackTraceString(e));
                            }

                            return true;
                        }
                    });
                }

                if(!medicationPoisoningUris.equals(""))
                {
                    final JSONObject medicationPoisoningObject = new JSONObject(medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_POISONING_URIS));

                    final JSONArray medicationPoisoningCodesArray = medicationPoisoningObject.getJSONArray("codes");
                    final JSONArray medicationPoisoningUrisArray = medicationPoisoningObject.getJSONArray("uris");

                    final int medicationPoisoningCodesCount = medicationPoisoningCodesArray.length();

                    for(int i = 0; i < medicationPoisoningCodesCount; i++)
                    {
                        final String poisoningCode = medicationPoisoningCodesArray.getString(i);
                        final String poisoningUri = medicationPoisoningUrisArray.getString(i);

                        order++;

                        String menuItemAppend = (medicationPoisoningCodesCount == 1) ? "" : " ("+poisoningCode+")";

                        MenuItem menuItem = mMenu.add(Menu.NONE, Menu.NONE, order, getString(R.string.medication_menu_poisoning_uri)+menuItemAppend);

                        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem)
                            {
                                Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                                intent.putExtra("title", medicationName);
                                intent.putExtra("uri", poisoningUri);
                                startActivity(intent);

                                return true;
                            }
                        });
                    }
                }

                boolean showAtcCodesBelowMedicationName = mTools.getDefaultSharedPreferencesBoolean("SHOW_ATC_CODES_BELOW_MEDICATION_NAME");

                LayoutInflater layoutInflater = null;
                LinearLayout linearLayout = null;

                if(showAtcCodesBelowMedicationName)
                {
                    layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    linearLayout = (LinearLayout) findViewById(R.id.medication_detail_atc_codes_layout);

                    linearLayout.setVisibility(View.VISIBLE);
                }

                if(!medicationAtcCodes.equals(""))
                {
                    final JSONArray medicationAtcCodesJsonArray = new JSONArray(medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ATC_CODES));

                    for(int i = 0; i < medicationAtcCodesJsonArray.length(); i++)
                    {
                        final String atcCode = medicationAtcCodesJsonArray.getString(i);

                        order++;

                        MenuItem menuItem = mMenu.add(Menu.NONE, Menu.NONE, order, getString(R.string.medication_menu_atc_code_uri)+" ("+atcCode+")");

                        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem)
                            {
                                Intent intent = new Intent(mContext, AtcCodesActivity.class);
                                intent.putExtra("code", atcCode);
                                startActivity(intent);

                                return true;
                            }
                        });

                        if(showAtcCodesBelowMedicationName)
                        {
                            TextView textView = (TextView) layoutInflater.inflate(R.layout.activity_medication_detail_atc_code, null);

                            textView.setText(getString(R.string.medication_atc_code)+": "+atcCode);
                            textView.setPaintFlags(textView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

                            textView.setOnClickListener(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    Intent intent = new Intent(mContext, AtcCodesActivity.class);
                                    intent.putExtra("code", atcCode);
                                    startActivity(intent);
                                }
                            });

                            linearLayout.addView(textView);
                        }
                    }
                }
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }

            // Medication name
            TextView textView = (TextView) findViewById(R.id.medication_name);
            textView.setText(medicationName);

            // Medication type
            textView = (TextView) findViewById(R.id.medication_type);
            textView.setText(medicationType);

            // Medication needs Schengen certificate?
            if(medicationSchengenCertificate.equals("yes"))
            {
                textView = (TextView) findViewById(R.id.medication_schengen_certificate);
                textView.setText(getString(R.string.medication_schengen_certificate));
                textView.setPaintFlags(textView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("title", medicationName);
                        intent.putExtra("uri", "http://legemiddelverket.no/Bruk_og_raad/Medisiner-pa-utenlandsreise/Sider/default.aspx");
                        startActivity(intent);
                    }
                });

                textView.setVisibility(View.VISIBLE);
            }

            // Medication doping status
            textView = (TextView) findViewById(R.id.medication_doping_status);
            textView.setText(medicationDopingStatusDescription);

            if(medicationDopingStatus.equals("yellow"))
            {
                textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.medication_doping_status_orange), null, null, null);
                textView.setPaintFlags(textView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("title", medicationName);
                        intent.putExtra("uri", medicationDopingStatusUri);
                        startActivity(intent);
                    }
                });
            }
            else if(medicationDopingStatus.equals("red"))
            {
                textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.medication_doping_status_red), null, null, null);
                textView.setPaintFlags(textView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("title", medicationName);
                        intent.putExtra("uri", medicationDopingStatusUri);
                        startActivity(intent);
                    }
                });
            }

            // Medication prescription group
            Button button = (Button) findViewById(R.id.medication_prescription_group);
            button.setText(medicationPrescriptionGroup);

            if(medicationPrescriptionGroup.startsWith("B"))
            {
                mTools.setBackgroundDrawable(button, R.drawable.medication_prescription_group_orange);
            }
            else if(medicationPrescriptionGroup.startsWith("A"))
            {
                mTools.setBackgroundDrawable(button, R.drawable.medication_prescription_group_red);
            }

            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    mTools.showToast(medicationPrescriptionGroupDescription, 1);
                }
            });

            // Medication available on blue prescription?
            if(medicationBluePrescription.equals("yes"))
            {
                button = (Button) findViewById(R.id.medication_blue_prescription);

                button.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        mTools.showToast(getString(R.string.medication_blue_prescription), 1);
                    }
                });

                button.setVisibility(View.VISIBLE);
            }

            // Medication has triangle warning?
            if(medicationTriangle.equals("yes"))
            {
                ImageButton imageButton = (ImageButton) findViewById(R.id.medication_triangle);

                imageButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        mTools.showToast(getString(R.string.medication_triangle), 1);
                    }
                });

                imageButton.setVisibility(View.VISIBLE);
            }

            // Medication has black triangle warning?
            if(medicationBlackTriangle.equals("yes"))
            {
                ImageButton imageButton = (ImageButton) findViewById(R.id.medication_black_triangle);

                imageButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("title", medicationName);
                        intent.putExtra("uri", "http://www.legemiddelverket.no/Bivirkninger/legemiddelovervaaking/svart_trekant/Sider/default.aspx");
                        startActivity(intent);
                    }
                });

                imageButton.setVisibility(View.VISIBLE);
            }

            // Medication exists with more information?
            if(medicationFullContentReference.equals(""))
            {
                boolean hideMedicationTipDialog = mTools.getSharedPreferencesBoolean("MEDICATION_HIDE_MEDICATION_TIP_DIALOG");

                if(!hideMedicationTipDialog)
                {
                    new MaterialDialog.Builder(mContext).title(getString(R.string.medication_tip_dialog_title)).content(getString(R.string.medication_tip_dialog_message)).positiveText(getString(R.string.medication_tip_dialog_positive_button)).callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            mTools.setSharedPreferencesBoolean("MEDICATION_HIDE_MEDICATION_TIP_DIALOG", true);
                        }
                    }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                }
            }
            else
            {
                try
                {
                    JSONObject fullContentJsonObject = new JSONObject(medicationFullContentReference);

                    final String fullContentMedicationName = fullContentJsonObject.getString("name");
                    final String fullContentMedicationManufacturer = fullContentJsonObject.getString("manufacturer");
                    final String fullContentMedicationUri = fullContentJsonObject.getString("uri");

                    new MaterialDialog.Builder(mContext).title(medicationName).content(Html.fromHtml(getString(R.string.medication_full_content_reference_dialog_message_first)+"<br><br><b>"+fullContentMedicationName+"</b><br><small>"+fullContentMedicationManufacturer+"</small><br><br>"+getString(R.string.medication_full_content_reference_dialog_message_second))).positiveText(getString(R.string.medication_full_content_reference_dialog_positive_button)).negativeText(getString(R.string.medication_full_content_reference_dialog_negative_button)).callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            mTools.getMedicationWithFullContent(fullContentMedicationUri);

                            finish();
                        }
                    }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black).show();
                }
                catch(Exception e)
                {
                    Log.e("MedicationActivity", Log.getStackTraceString(e));
                }
            }

            // Web view
            mWebView.loadDataWithBaseURL("file:///android_asset/", medicationContent, "text/html", "utf-8", null);

            Handler handler = new Handler();

            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mWebView.loadUrl("javascript:addPregnancyAndBreastfeedingLinks();");
                }
            }, 1000);
        }

        @Override
        protected HashMap<String, String> doInBackground(Long... longs)
        {
            SQLiteDatabase sqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            Cursor cursor = sqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, null, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID+" = "+longs[0], null, null, null, null);

            String[] columns = cursor.getColumnNames();

            HashMap<String, String> medication = new HashMap<>();

            if(cursor.moveToFirst())
            {
                for(String column : columns)
                {
                    try
                    {
                        medication.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
                    }
                    catch(Exception e)
                    {
                        Log.e("MedicationActivity", Log.getStackTraceString(e));
                    }
                }
            }

            cursor.close();
            sqLiteDatabase.close();

            return medication;
        }
    }*/

    // JavaScript interface
    public class JavaScriptInterface
    {
        private final Context mContext;

        JavaScriptInterface(Context context)
        {
            mContext = context;
        }

        @JavascriptInterface
        public void JSshowSynonymDialog(final String id)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mProgressBar.setVisibility(View.VISIBLE);

                    RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://www.felleskatalogen.no/m/medisin/ordbok/json?term="+id, null, new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response)
                        {
                            mProgressBar.setVisibility(View.GONE);

                            try
                            {
                                String title = response.getString("currentSynonym");
                                String message = response.getString("description");

                                Pattern pattern = Pattern.compile("<span class=\"ordbok (\\d+-\\d)+\">([^<]+)</span>");
                                Matcher matcher = pattern.matcher(message);

                                ArrayList<String> synonymIdsArrayList = new ArrayList<>();
                                ArrayList<String> synonymNamesArrayList = new ArrayList<>();

                                int i = 0;

                                while(matcher.find())
                                {
                                    synonymIdsArrayList.add(matcher.group(1));
                                    synonymNamesArrayList.add(matcher.group(2));

                                    i++;
                                }

                                final String[] synonymIdsStringArrayList = new String[i];
                                final String[] synonymNamesStringArrayList = new String[i];

                                for(int n = 0; n < synonymIdsStringArrayList.length; n++)
                                {
                                    synonymIdsStringArrayList[n] = synonymIdsArrayList.get(n);
                                    synonymNamesStringArrayList[n] = mTools.ucfirst(synonymNamesArrayList.get(n).replaceAll("\\s", " "));
                                }

                                message = message.replaceAll("\n", " ").replaceAll("<span[^>]*>", "<font color=\"#009688\"><i>").replaceAll("</span>", "</i></font>");

                                MaterialDialog.Builder synonymDialog = new MaterialDialog.Builder(mContext).title(title).content(Html.fromHtml(message+"<br><br><small><i>"+getString(R.string.medication_synonym_dialog_source)+"</i></small>")).positiveText(getString(R.string.medication_synonym_dialog_positive_button)).callback(new MaterialDialog.ButtonCallback()
                                {
                                    @Override
                                    public void onNeutral(MaterialDialog dialog)
                                    {
                                        new MaterialDialog.Builder(mContext).title(getString(R.string.medication_synonym_list_dialog_title)).items(synonymNamesStringArrayList).itemsCallback(new MaterialDialog.ListCallback()
                                        {
                                            @Override
                                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence)
                                            {
                                                mProgressBar.setVisibility(View.VISIBLE);

                                                String id = synonymIdsStringArrayList[i];

                                                RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                                                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://www.felleskatalogen.no/m/medisin/ordbok/json?term="+id, null, new Response.Listener<JSONObject>()
                                                {
                                                    @Override
                                                    public void onResponse(JSONObject response)
                                                    {
                                                        mProgressBar.setVisibility(View.GONE);

                                                        try
                                                        {
                                                            String title = response.getString("currentSynonym");
                                                            String message = response.getString("description");

                                                            message = message.replaceAll("\n", " ").replaceAll("</?span[^>]*>", "");

                                                            new MaterialDialog.Builder(mContext).title(title).content(Html.fromHtml(message+"<br><br><small><i>"+getString(R.string.medication_synonym_dialog_source)+"</i></small>")).positiveText(getString(R.string.medication_synonym_dialog_positive_button)).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                                                        }
                                                        catch(Exception e)
                                                        {
                                                            mTools.showToast(getString(R.string.medication_could_not_get_data), 1);

                                                            Log.e("MedicationActivity", Log.getStackTraceString(e));
                                                        }
                                                    }
                                                }, new Response.ErrorListener()
                                                {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error)
                                                    {
                                                        mProgressBar.setVisibility(View.GONE);

                                                        mTools.showToast(getString(R.string.medication_could_not_get_data), 1);

                                                        Log.e("MedicationActivity", error.toString());
                                                    }
                                                });

                                                jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                                                requestQueue.add(jsonObjectRequest);
                                            }
                                        }).itemColorRes(R.color.teal).show();
                                    }
                                }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue);

                                if(synonymIdsStringArrayList.length > 0) synonymDialog.neutralText(R.string.medication_synonym_dialog_neutral_button).neutralColorRes(R.color.teal);

                                synonymDialog.show();
                            }
                            catch(Exception e)
                            {
                                mTools.showToast(getString(R.string.medication_could_not_get_data), 1);

                                Log.e("MedicationActivity", Log.getStackTraceString(e));
                            }
                        }
                    }, new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error)
                        {
                            mProgressBar.setVisibility(View.GONE);

                            mTools.showToast(getString(R.string.medication_could_not_get_data), 1);

                            Log.e("MedicationActivity", error.toString());
                        }
                    });

                    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                    requestQueue.add(jsonObjectRequest);
                }
            });
        }

        @JavascriptInterface
        public void JSshowPregnancyOrBreastfeedingDialog(final String chapter)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        mProgressBar.setVisibility(View.VISIBLE);

                        RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                        JSONArray medicationSubstancesJsonArray = new JSONArray(medicationSubstances);

                        String substances = "";

                        for(int i = 0; i < medicationSubstancesJsonArray.length(); i++)
                        {
                            substances += medicationSubstancesJsonArray.getString(i)+"|";
                        }

                        substances = URLEncoder.encode(substances.replaceAll("\\|$", ""), "utf-8");

                        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest("http://www.felleskatalogen.no/m/medisin/nlh/json?kapittel="+chapter+"&substans="+substances, new Response.Listener<JSONArray>()
                        {
                            @Override
                            public void onResponse(JSONArray response)
                            {
                                mProgressBar.setVisibility(View.GONE);

                                try
                                {
                                    String title = mTools.ucfirst(chapter);
                                    String message = "";

                                    for(int i = 0; i < response.length(); i++)
                                    {
                                        JSONObject jsonObject = response.getJSONObject(i);

                                        String substance = mTools.ucfirst(jsonObject.getString("substansnavn"));
                                        String description = jsonObject.getString("beskrivelse").replaceAll("<[^>]+>", "")+".";

                                        message += "<b>"+substance+"</b><br>"+description+"<br><br>";
                                    }

                                    message = message.replaceAll("\\([^\\)]+\\)", "").replaceAll(" \\.", ".")+"<small><i>Kilde: Norsk legemiddelhåndbok</i></small>";

                                    new MaterialDialog.Builder(mContext).title(title).content(Html.fromHtml(message)).positiveText("Lukk").contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                                }
                                catch(Exception e)
                                {
                                    Log.e("MedicationActivity", Log.getStackTraceString(e));
                                }
                            }
                        }, new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error)
                            {
                                mProgressBar.setVisibility(View.GONE);

                                mTools.showToast(getString(R.string.medication_could_not_get_data), 1);

                                Log.e("MedicationActivity", error.toString());
                            }
                        });

                        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                        requestQueue.add(jsonArrayRequest);
                    }
                    catch(Exception e)
                    {
                        Log.e("MedicationActivity", Log.getStackTraceString(e));
                    }
                }
            });
        }

        @JavascriptInterface
        public void JSresetSpinner()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mSpinner.setSelection(0);
                }
            });
        }
    }
}