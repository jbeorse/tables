package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import org.opendatakit.activities.IOdkDataActivity;

/**
 * Fragment displaying the navigate module
 *
 * @author belendia@gmail.com
 *
 */
public class NavigateFragment extends Fragment implements IMapListViewCallbacks {

  private static final String TAG = NavigateFragment.class.getSimpleName();

  /**
   * Represents an index that can't possibly be in the list
   */
  public static final int INVALID_INDEX = -1;

  /**
   * Saves the index of the element that was selected.
   */
  private static final String INTENT_KEY_SELECTED_INDEX = "keySelectedIndex";

  /**
   * The index of an item that has been selected by the user.
   * We must default to invalid index because the initial load of the list view may take place
   * before onCreate is called
   */
  protected int mSelectedItemIndex = INVALID_INDEX;


  // TODO: Delete this
  TextView deleteThis;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName may not be available...
    if (savedInstanceState != null) {
      this.mSelectedItemIndex = savedInstanceState.containsKey(INTENT_KEY_SELECTED_INDEX) ?
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX) :
          INVALID_INDEX;
    }

    // TODO: Delete this
    deleteThis = new TextView(getActivity());
    deleteThis.setText("Navigate View");
    deleteThis.setVisibility(View.VISIBLE);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, mSelectedItemIndex);
  }

  /**
   * Resets the view (the list), and sets the visibility to visible.
   */
  void resetView() {

    // do not initiate reload until we have the database set up...
    Activity activity = getActivity();
    if (activity instanceof IOdkDataActivity) {
      if (((IOdkDataActivity) activity).getDatabase() == null) {
        return;
      }
    } else {
      Log.e(TAG,
          "Problem: NavigateView not being rendered from activity that is an " +
              "IOdkDataActivity");
      return;
    }

    if (getView() == null)
      return; // Can't do anything
  }

  /**
   * Informs the list view that no item is selected. Resets the state after a
   * call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    // TODO: Make this work with async API
    this.resetView();
  }

  public int getIndexOfSelectedItem() {
    return this.mSelectedItemIndex;
  }

  /**
   * Sets the index of the item to navigate to, which will be the row of the data wanting
   * to be displayed.
   */
  @Override
  public void setIndexOfSelectedItem(final int index) {
    this.mSelectedItemIndex = index;
    // TODO: Make this work with async API
    this.resetView();
  }

}