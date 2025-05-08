package com.video.vidbr;

import android.content.Context;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;

import java.io.File;

public class VideoCacheManager {
    private static SimpleCache simpleCache;

    public static SimpleCache getCache(Context context) {
        if (simpleCache == null) {
            File cacheDir = new File(context.getCacheDir(), "video_cache");
            DatabaseProvider databaseProvider = new StandaloneDatabaseProvider(context);
            simpleCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), databaseProvider); // 100MB de cache
        }
        return simpleCache;
    }
}
