package org.kpcc.android;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.view.View;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

public class NetworkImageManager {
    public final static NetworkImageManager instance = new NetworkImageManager();
    private final static String GENERIC_SLUG = "generic";
    private ImageLoader mImageLoader;

    private NetworkImageManager() {
        mImageLoader = new ImageLoader(HttpRequest.Manager.instance.getRequestQueue(),
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

    public void setPrerollImage(NetworkImageView view, String url) {
        view.setImageUrl(url, mImageLoader);
        view.setVisibility(View.VISIBLE);
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
