package org.kpcc.android;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Created by rickb014 on 3/1/15.
 */
public class BackgroundImageManager {
    private static BackgroundImageManager INSTANCE = null;
    private final static String GENERIC_SLUG = "generic";

    private ImageLoader mImageLoader;

    public static void setupInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BackgroundImageManager();
        }
    }

    public static BackgroundImageManager getInstance() {
        return INSTANCE;
    }

    private BackgroundImageManager() {
        mImageLoader = new ImageLoader(HttpRequest.Manager.getInstance().getRequestQueue(),
                new ImageLoader.ImageCache() {
                    // There are 66 programs right now. We don't need to cache all of the images.
                    private final LruCache<String, Bitmap> cache = new LruCache<>(50);

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

}
