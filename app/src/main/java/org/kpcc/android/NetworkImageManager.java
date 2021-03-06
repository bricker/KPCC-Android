package org.kpcc.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

class NetworkImageManager {
    private final static NetworkImageManager instance = new NetworkImageManager();
    private static final String PROGRAM_TILE_URL = "http://media.scpr.org/iphone/program-images/program_tile_%s@2x.jpg";
    private final ImageLoader mImageLoader;

    private NetworkImageManager() {
        // On the S4 this was around 16000 kilobytes.
        int cacheSize = (int) Runtime.getRuntime().maxMemory() / 1024 / 8;
        mImageLoader = new ImageLoader(HttpRequest.Manager.instance.requestQueue,
                new BitmapLruCache(cacheSize));
    }

    static NetworkImageManager getInstance() {
        return instance;
    }

    public synchronized void setPrerollImage(NetworkImageView view, String url) {
        view.setImageUrl(url, mImageLoader);
        view.setVisibility(View.VISIBLE);
    }

    public synchronized void setBitmap(final Context context, final ImageView view, String slug) {
        mImageLoader.get(buildTileUrl(slug), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null) {
                    Animation fader = AnimationUtils.loadAnimation(context, R.anim.fadein);
                    view.setImageBitmap(response.getBitmap());
                    view.startAnimation(fader);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                setDefaultBitmap(view);
            }
        });
    }

    public synchronized void setDefaultBitmap(ImageView view) {
        view.setImageResource(R.drawable.tile_generic);
    }

    private String buildTileUrl(String slug) {
        return String.format(PROGRAM_TILE_URL, slug);
    }


    public class BitmapLruCache
            extends LruCache<String, Bitmap>
            implements ImageLoader.ImageCache {

        public BitmapLruCache(int sizeInKiloBytes) {
            super(sizeInKiloBytes);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            // Generally one of our background images is around 2100 kb
            // This is the uncompressed bitmap size, not the compressed JPG size
            // Should we be compressing these images?
            return value.getRowBytes() * value.getHeight() / 1024;
        }

        @Override
        public Bitmap getBitmap(String url) {
            return get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }
    }
}
