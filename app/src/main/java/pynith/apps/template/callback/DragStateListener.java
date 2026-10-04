package pynith.apps.template.callback;

/**
 * Created by Elias.
 */

public interface DragStateListener {

    void onDragStart();

    void onDragEnd(boolean isMenuOpened);
}
