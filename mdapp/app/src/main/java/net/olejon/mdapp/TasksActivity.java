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

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

public class TasksActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private ListView mListView;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Input manager
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Layout
        setContentView(R.layout.activity_tasks);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.tasks_toolbar);
        toolbar.setTitle(getString(R.string.tasks_title));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Floating action button
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.tasks_fab);

        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new MaterialDialog.Builder(mContext).title(getString(R.string.tasks_dialog_title)).customView(R.layout.activity_tasks_dialog, true).positiveText(getString(R.string.tasks_dialog_positive_button)).negativeText(getString(R.string.tasks_dialog_negative_button)).callback(new MaterialDialog.ButtonCallback()
                {
                    @Override
                    public void onPositive(MaterialDialog dialog)
                    {
                        EditText taskEditText = (EditText) dialog.findViewById(R.id.tasks_dialog_task);

                        String task = taskEditText.getText().toString().trim();

                        if(task.equals(""))
                        {
                            mTools.showToast("Du må fylle ut navn på oppgaven", 1);
                        }
                        else
                        {
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(TasksSQLiteHelper.COLUMN_TASK, task);
                            contentValues.put(TasksSQLiteHelper.COLUMN_CREATED_TIME, "");
                            contentValues.put(TasksSQLiteHelper.COLUMN_REMINDER_TIME, "");
                            contentValues.put(TasksSQLiteHelper.COLUMN_COMPLETED, "no");

                            mSqLiteDatabase.insert(TasksSQLiteHelper.TABLE, null, contentValues);

                            dialog.dismiss();

                            getTasks();
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog)
                    {
                        dialog.dismiss();
                    }
                }).showListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface dialogInterface)
                    {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    }
                }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black).autoDismiss(false).show();
            }
        });

        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fab);

        floatingActionButton.startAnimation(animation);
        floatingActionButton.setVisibility(View.VISIBLE);

        // List
        mListView = (ListView) findViewById(R.id.tasks_list);

        // Get tasks
        getTasks();

        // Tip dialog
        if(!mTools.getSharedPreferencesBoolean("TASKS_HIDE_TIP_DIALOG"))
        {
            new MaterialDialog.Builder(mContext).title(getString(R.string.tasks_tip_dialog_title)).content(getString(R.string.tasks_tip_dialog_message)).positiveText(getString(R.string.tasks_tip_dialog_positive_button)).callback(new MaterialDialog.ButtonCallback()
            {
                @Override
                public void onPositive(MaterialDialog dialog)
                {
                    mTools.setSharedPreferencesBoolean("TASKS_HIDE_TIP_DIALOG", true);
                }
            }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
        }
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            case R.id.tasks_menu_clear_tasks:
            {
                removeTasks();
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get tasks
    private void getTasks()
    {
        GetTasksTask getTasksTask = new GetTasksTask();
        getTasksTask.execute();
    }

    private class GetTasksTask extends AsyncTask<Void, Void, TasksSimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(TasksSimpleCursorAdapter tasksSimpleCursorAdapter)
        {
            mListView.setAdapter(tasksSimpleCursorAdapter);

            View listViewEmpty = findViewById(R.id.tasks_list_empty);
            mListView.setEmptyView(listViewEmpty);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id)
                {
                    if(mCursor.moveToPosition(i))
                    {
                        int taskId = mCursor.getInt(mCursor.getColumnIndexOrThrow(TasksSQLiteHelper.COLUMN_ID));

                        String taskCompleted = mCursor.getString(mCursor.getColumnIndexOrThrow(TasksSQLiteHelper.COLUMN_COMPLETED));

                        String completed = (taskCompleted.equals("no")) ? "yes" : "no";

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(TasksSQLiteHelper.COLUMN_COMPLETED, completed);

                        mSqLiteDatabase.update(TasksSQLiteHelper.TABLE, contentValues, TasksSQLiteHelper.COLUMN_ID+" = "+taskId, null);

                        getTasks();
                    }
                }
            });
        }

        @Override
        protected TasksSimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new TasksSQLiteHelper(mContext).getReadableDatabase();

            String[] queryColumns = {TasksSQLiteHelper.COLUMN_ID, TasksSQLiteHelper.COLUMN_TASK, TasksSQLiteHelper.COLUMN_COMPLETED};
            mCursor = mSqLiteDatabase.query(TasksSQLiteHelper.TABLE, queryColumns, null, null, null, null, TasksSQLiteHelper.COLUMN_COMPLETED+", "+TasksSQLiteHelper.COLUMN_TASK+" COLLATE NOCASE");

            String[] fromColumns = {TasksSQLiteHelper.COLUMN_TASK};
            int[] toViews = {R.id.tasks_list_item_task};

            return new TasksSimpleCursorAdapter(mContext, mCursor, fromColumns, toViews);
        }
    }

    // Remove tasks
    private void removeTasks()
    {
        int removed = mSqLiteDatabase.delete(TasksSQLiteHelper.TABLE, TasksSQLiteHelper.COLUMN_COMPLETED+" = 'yes'", null);

        String tasks = (removed == 1) ? getString(R.string.tasks_task) : getString(R.string.tasks_tasks);

        mTools.showToast(removed+" "+tasks+" "+getString(R.string.tasks_removed), 0);

        getTasks();
    }
}