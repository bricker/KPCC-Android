package org.kpcc.android;

import android.support.v4.app.Fragment;

/**
 * Created by rickb014 on 7/13/16.
 */
public class StreamBindFragment extends Fragment {
    protected StreamServiceConnection mStreamConnection = new StreamServiceConnection();

    protected OnDemandPlayer getOnDemandPlayer() {
        if (mStreamConnection == null || !mStreamConnection.getStreamIsBound()) return null;

        StreamService service = mStreamConnection.getStreamService();
        if (service == null) return null;

        Stream stream = service.getCurrentStream();
        if (stream != null && stream instanceof OnDemandPlayer) {
            return (OnDemandPlayer)stream;
        } else {
            return null;
        }
    }

    protected LivePlayer getLivePlayer() {
        if (mStreamConnection == null || !mStreamConnection.getStreamIsBound()) return null;

        StreamService service = mStreamConnection.getStreamService();
        if (service == null) return null;

        Stream stream = service.getCurrentStream();
        if (stream != null && stream instanceof LivePlayer) {
            return (LivePlayer)stream;
        } else {
            return null;
        }
    }

    protected PrerollPlayer getPrerollPlayer() {
        if (mStreamConnection == null || !mStreamConnection.getStreamIsBound()) return null;

        StreamService service = mStreamConnection.getStreamService();
        if (service == null) return null;

        Stream stream = service.getCurrentStream();
        if (stream != null && stream instanceof PrerollPlayer) {
            return (PrerollPlayer)stream;
        } else {
            return null;
        }
    }
}
