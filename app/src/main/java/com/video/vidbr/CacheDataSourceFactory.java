package com.video.vidbr;

import android.content.Context;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;

public class CacheDataSourceFactory implements DataSource.Factory {
    private final Context context;

    public CacheDataSourceFactory(Context context) {
        this.context = context;
    }

    @Override
    public DataSource createDataSource() {
        return new CacheDataSource(
                VideoCacheManager.getCache(context),
                new DefaultHttpDataSource.Factory().createDataSource()
        );
    }
}
