package org.kpcc.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

/**
 * Created by rickb014 on 3/1/15.
 */
public class RequestManager {
    private static RequestManager INSTANCE = null;
    private final static String GENERIC_SLUG = "generic";

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    public static void setupInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new RequestManager(context);
        }
    }

    public static RequestManager getInstance() {
        return INSTANCE;
    }

    private RequestManager(Context context) {
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public void setBackgroundImage(NetworkImageView view, String slug) {
        view.setErrorImageResId(R.drawable.tile_generic);

        if (slug.equals(GENERIC_SLUG)) {
            // We bundle this with the app, no need to make a request.
            view.setBackgroundResource(R.drawable.tile_generic);
        } else {
            view.setImageUrl(ProgramsManager.buildTileUrl(slug), mImageLoader);
        }
    }

    public void setDefaultBackgroundImage(NetworkImageView view) {
        setBackgroundImage(view, GENERIC_SLUG);
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

}
