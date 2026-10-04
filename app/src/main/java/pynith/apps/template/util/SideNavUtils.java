package pynith.apps.template.util;

/**
 * Created by Elias.
 */

public abstract class SideNavUtils {

    public static float evaluate(float fraction, float startValue, float endValue) {
        return startValue + fraction * (endValue - startValue);
    }
}
