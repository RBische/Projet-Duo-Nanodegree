package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;

/**
 * Created by biche on 16/08/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TodayWidgetRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context mContext;
    private Cursor mData;
    public static final String[] MATCH_COLUMNS = {
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.LEAGUE_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.MATCH_DAY
    };
    public static final int COL_MATCH_ID = 0;
    public static final int COL_DATE = 1;
    public static final int COL_MATCHTIME = 2;
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_LEAGUE = 5;
    public static final int COL_HOME_GOALS = 6;
    public static final int COL_AWAY_GOALS = 7;
    public static final int COL_MATCHDAY = 8;

    public TodayWidgetRemoteViewFactory(Context context, Intent intent) {
        this.mContext = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        // Get today's data from the ContentProvider
        final long identityToken = Binder.clearCallingIdentity();
        Date fragmentdate = new Date(System.currentTimeMillis());
        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
        Uri scoreWithDate = DatabaseContract.scores_table.buildScoreWithDate();
        mData = mContext.getContentResolver().query(scoreWithDate, MATCH_COLUMNS, null,
                new String[]{mformat.format(fragmentdate)}, null);
        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        mData.close();
    }

    @Override
    public int getCount() {
        return mData.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION ||
                mData == null || !mData.moveToPosition(position)) {
            return null;
        }
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_today_list_item);
        views.setTextViewText(R.id.home_name,mData.getString(COL_HOME));
        views.setTextViewText(R.id.away_name, mData.getString(COL_AWAY));
        views.setTextViewText(R.id.data_textview, mData.getString(COL_MATCHTIME));
        views.setTextViewText(R.id.score_textview, Utilities.getScores(mData.getInt(COL_HOME_GOALS), mData.getInt(COL_AWAY_GOALS)));
        views.setImageViewResource(R.id.home_crest,Utilities.getTeamCrestByTeamName(
                mData.getString(COL_HOME)));
        views.setImageViewResource(R.id.away_crest, Utilities.getTeamCrestByTeamName(
                mData.getString(COL_AWAY)
        ));
        final Intent fillInIntent = new Intent();
        Uri scoreWithDateUri = DatabaseContract.scores_table.buildScoreWithDate();
        fillInIntent.setData(scoreWithDateUri);
        fillInIntent.putExtra(MainActivity.EXTRA_MATCH_ID_TO_SHOW,mData.getInt(COL_MATCH_ID));
        views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.widget_today_list_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mData.moveToPosition(position))
            return mData.getLong(COL_MATCH_ID);
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
