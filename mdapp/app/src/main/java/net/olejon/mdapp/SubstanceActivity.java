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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SubstanceActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private MenuItem mAtcCodeMenuItem;
    private ListView mListView;

    private String substanceAtcCode;
    private String substanceName;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Intent
        final Intent intent = getIntent();

        final long substanceId = intent.getLongExtra("id", 0);

        // Open database
        SQLiteDatabase sqLiteDatabase = new SlDataSQLiteHelper(mContext).getReadableDatabase();

        String[] queryColumns = {SlDataSQLiteHelper.SUBSTANCES_COLUMN_ATC_CODE, SlDataSQLiteHelper.SUBSTANCES_COLUMN_NAME};
        Cursor cursor = sqLiteDatabase.query(SlDataSQLiteHelper.TABLE_SUBSTANCES, queryColumns, SlDataSQLiteHelper.SUBSTANCES_COLUMN_ID+" = "+substanceId, null, null, null, null);

        if(cursor.moveToFirst())
        {
            // Substance
            substanceAtcCode = cursor.getString(cursor.getColumnIndexOrThrow(SlDataSQLiteHelper.SUBSTANCES_COLUMN_ATC_CODE));
            substanceName = cursor.getString(cursor.getColumnIndexOrThrow(SlDataSQLiteHelper.SUBSTANCES_COLUMN_NAME));

            // Layout
            setContentView(R.layout.activity_substance);

            // Toolbar
            final Toolbar toolbar = (Toolbar) findViewById(R.id.substance_toolbar);
            toolbar.setTitle(substanceName);

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // ATC code
            TextView atcCodeTextView = (TextView) findViewById(R.id.substance_atc_code);
            atcCodeTextView.setText(substanceAtcCode);

            // List
            mListView = (ListView) findViewById(R.id.substance_list);
        }

        // Close database
        cursor.close();
        sqLiteDatabase.close();
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_substance, menu);

        mAtcCodeMenuItem = menu.findItem(R.id.substance_menu_atc);

        GetMedicationsTask getMedicationsTask = new GetMedicationsTask();
        getMedicationsTask.execute();

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
            case R.id.substance_menu_interactions:
            {
                Intent intent = new Intent(mContext, InteractionsActivity.class);
                intent.putExtra("search", substanceAtcCode.replace("ATC-kode: ", ""));
                startActivity(intent);
                return true;
            }
            case R.id.substance_menu_atc:
            {
                Intent intent = new Intent(mContext, AtcCodesActivity.class);
                intent.putExtra("code", substanceAtcCode.replace("ATC-kode: ", ""));
                startActivity(intent);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get medications
    private class GetMedicationsTask extends AsyncTask<Void, Void, SimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(SimpleCursorAdapter simpleCursorAdapter)
        {
            mAtcCodeMenuItem.setTitle(getString(R.string.substance_menu_atc)+" ("+substanceAtcCode.replace("ATC-kode: ", "")+")");

            mListView.setAdapter(simpleCursorAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id)
                {
                    if(mCursor.moveToPosition(i))
                    {
                        Intent intent = new Intent(mContext, MedicationActivity.class);

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        {
                            if(mTools.getDefaultSharedPreferencesBoolean("MEDICATION_MULTIPLE_DOCUMENTS")) intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        }

                        intent.putExtra("id", id);
                        startActivity(intent);
                    }
                }
            });
        }

        @Override
        protected SimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new SlDataSQLiteHelper(mContext).getReadableDatabase();

            String[] queryColumns = {SlDataSQLiteHelper.MEDICATIONS_COLUMN_ID, SlDataSQLiteHelper.MEDICATIONS_COLUMN_NAME, SlDataSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER};
            mCursor = mSqLiteDatabase.query(SlDataSQLiteHelper.TABLE_MEDICATIONS, queryColumns, SlDataSQLiteHelper.MEDICATIONS_COLUMN_SUBSTANCE+" LIKE "+mTools.sqe("%"+substanceName+"%"), null, null, null, SlDataSQLiteHelper.MEDICATIONS_COLUMN_NAME+" COLLATE NOCASE");

            String[] fromColumns = {SlDataSQLiteHelper.MEDICATIONS_COLUMN_NAME, SlDataSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER};
            int[] toViews = {R.id.substance_list_item_name, R.id.substance_list_item_manufacturer};

            return new SimpleCursorAdapter(mContext, R.layout.activity_substance_list_item, mCursor, fromColumns, toViews, 0);
        }
    }
}