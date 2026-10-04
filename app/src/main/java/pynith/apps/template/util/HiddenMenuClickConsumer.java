package pynith.apps.template.util;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import pynith.apps.template.SlidingRootNavLayout;

/**
 * Created by Elias.
 */

public class HiddenMenuClickConsumer extends View {

    private SlidingRootNavLayout menuHost;

    public HiddenMenuClickConsumer(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return menuHost.isMenuClosed();
    }

    public void setMenuHost(SlidingRootNavLayout layout) {
        this.menuHost = layout;
    }
}
