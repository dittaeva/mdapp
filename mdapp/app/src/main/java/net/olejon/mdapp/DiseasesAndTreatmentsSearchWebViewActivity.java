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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class DiseasesAndTreatmentsSearchWebViewActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private MenuItem goForwardMenuItem;
    private ProgressBar mProgressBar;
    private WebView mWebView;

    private String pageTitle;
    private String pageUri;

    private boolean mWebViewAnimationHasBeenShown = false;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Connected?
        if(!mTools.isDeviceConnected())
        {
            mTools.showToast(getString(R.string.device_not_connected), 1);

            finish();

            return;
        }

        // Transition
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);

        // Intent
        Intent intent = getIntent();

        pageTitle = intent.getStringExtra("title");
        pageUri = intent.getStringExtra("uri");

        // Layout
        setContentView(R.layout.activity_diseases_and_treatments_search_webview);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.diseases_and_treatments_search_webview_toolbar);
        toolbar.setTitle(pageTitle);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.diseases_and_treatments_search_webview_toolbar_progressbar_horizontal);

        // Web view
        mWebView = (WebView) findViewById(R.id.diseases_and_treatments_search_webview_content);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        if(pageUri.contains("webofknowledge") || pageUri.contains("brukerhandboken")) webSettings.setUseWideViewPort(true);

        if(pageUri.contains("brukerhandboken")) webSettings.setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:35.0) Gecko/20100101 Firefox/35.0");

        mWebView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if(url.matches("^https?://.*?\\.pdf$"))
                {
                    mTools.showToast(getString(R.string.diseases_and_treatments_search_webview_downloading_pdf), 1);

                    mTools.downloadFile(pageTitle, url);

                    return true;
                }

                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public void onProgressChanged(WebView view, int newProgress)
            {
                if(newProgress == 100)
                {
                    mProgressBar.setVisibility(View.INVISIBLE);

                    if(mWebView.canGoForward())
                    {
                        goForwardMenuItem.setVisible(true);
                    }
                    else
                    {
                        goForwardMenuItem.setVisible(false);
                    }

                    if(!mWebViewAnimationHasBeenShown)
                    {
                        mWebViewAnimationHasBeenShown = true;

                        if(pageUri.contains("webofknowledge"))
                        {
                            mWebView.loadUrl("javascript:$('div.search-criteria input').val('"+pageTitle+"'); $('form#UA_GeneralSearch_input_form').submit()");
                        }
                        else if(pageUri.contains("uptodate"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('h2').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("bmj"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('small.monograph-title').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("nhi"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('h1').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("sml"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('article.sml_search_result').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("forskning"))
                        {
                            mWebView.loadUrl("javascript:var elements = document.getElementsByTagName('span'); elements[0].scrollIntoView();");
                        }
                        else if(pageUri.contains("helsebiblioteket"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('h1').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("helsenorge"))
                        {
                            mWebView.loadUrl("javascript:var offset = $('h1#sidetittel').offset(); window.scrollTo(0, offset.top);");
                        }
                        else if(pageUri.contains("brukerhandboken"))
                        {
                                mWebView.loadUrl("javascript:var offset = $('p#emnetittel').offset(); window.scrollTo(0, offset.top);");
                        }

                        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
                        mWebView.startAnimation(animation);

                        mWebView.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }
        });

        CookieManager cookieManager = CookieManager.getInstance();

        cookieManager.setCookie("http://apps.webofknowledge.com/", "ROAMING_LOGIN=\"25240265d905679e8451b2f889f62c816d61696c406f6c656a6f6e2e6e6574\"; USERNAME=\"mail@olejon.net\"; SID=\"Q2INjfCqiFR6bkCfOx5\"; CUSTOMER=\"CRISTIN under the Ministry of Education and Research - Norway\"; E_GROUP_NAME=\"University of Bergen\"; JSESSIONID=98BFA3B163FC02F082B30D3B2B5BC587");
        cookieManager.setCookie("http://nhi.no/", "userCategory=professional");
        cookieManager.setCookie("http://www.helsebiblioteket.no/", "whycookie-visited=1");
        cookieManager.setCookie("http://tidsskriftet.no/", "osevencookiepromptclosed=1");
        cookieManager.setCookie("https://helsenorge.no/", "mh-unsupportedbar=");

        if(pageUri.contains("webofknowledge") || pageUri.contains("brukerhandboken")) mWebView.setInitialScale(100);

        mWebView.loadUrl(pageUri);
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

        mWebView.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) CookieSyncManager.getInstance().sync();
    }

    // Back button
    @Override
    public void onBackPressed()
    {
        if(mWebView.canGoBack())
        {
            mWebView.goBack();
        }
        else
        {
            super.onBackPressed();

            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        }
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_diseases_and_treatments_search_webview, menu);

        goForwardMenuItem = menu.findItem(R.id.diseases_and_treatments_search_webview_menu_go_forward);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
                return true;
            }
            case R.id.diseases_and_treatments_search_webview_menu_go_forward:
            {
                mWebView.goForward();
                return true;
            }
            case R.id.diseases_and_treatments_search_webview_menu_print:
            {
                mTools.printDocument(mWebView, pageTitle);
                return true;
            }
            case R.id.diseases_and_treatments_search_webview_menu_uri:
            {
                mTools.openUri(pageUri);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}
