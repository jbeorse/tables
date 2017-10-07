package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.todddavies.components.progressbar.ProgressWheel;
import org.opendatakit.activities.IOdkDataActivity;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.providers.GeoProvider;
import org.opendatakit.tables.utils.DistanceUtil;
import org.opendatakit.tables.views.CellInfo;
import org.opendatakit.tables.views.CompassView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying the navigate module
 *
 * @author belendia@gmail.com
 *
 */
public class NavigateFragment extends Fragment implements IMapListViewCallbacks,
    GeoProvider.DirectionEventListener, GeoProvider.LocationEventListener{

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

  private enum SignalState {
    NO_SIGNAL, POOR_SIGNAL, MODERATE_SIGNAL, GOOD_SIGNAL
  }

  private GeoProvider mGeoProvider;

  // default location accuracy
  private static final double GOOD_LOCATION_ACCURACY = 10;
  private static final double MODERATE_LOCATION_ACCURACY = 50;

  private ProgressWheel mSignalQualitySpinner;

  private TextView mHeadingTextView;
  private TextView mBearingTextView;
  private TextView mDistanceTextView;

  private CompassView mCompass;
  private CompassView mCensusLocation;

  private UserTable mTable;
  private ColumnDefinition mLatitudeColumn;
  private ColumnDefinition mLongitudeColumn;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName may not be available...
    if (savedInstanceState != null) {
      this.mSelectedItemIndex = savedInstanceState.containsKey(INTENT_KEY_SELECTED_INDEX) ?
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX) :
          INVALID_INDEX;
    }

    mGeoProvider = new GeoProvider(getActivity());
    mGeoProvider.setDirectionEventListener(this);
    mGeoProvider.setLocationEventListener(this);

    mSignalQualitySpinner = (ProgressWheel) getActivity().findViewById(R.id.signalQualitySpinner);
    mCompass = (CompassView) getActivity().findViewById(R.id.compass);
    mCensusLocation = (CompassView) getActivity().findViewById(R.id.destination);

    mBearingTextView = (TextView) getActivity().findViewById(R.id.bearingTextView);
    mHeadingTextView = (TextView) getActivity().findViewById(R.id.headingTextView);
    mDistanceTextView = (TextView) getActivity().findViewById(R.id.distanceTextView);

    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    mTable = activity.getUserTable();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    try {
      DbHandle db = dbInterface.openDatabase(activity.getAppName());

      mLatitudeColumn = orderedDefns.find(getLatitudeElementKey(db));
      mLongitudeColumn = orderedDefns.find(getLongitudeElementKey(db));
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
      WebLogger.getLogger(activity.getAppName()).e(TAG, "Unable to access database");
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (mGeoProvider.isGpsProviderOn() == false
        && mGeoProvider.isNetworkOn() == false) {
      setSpinnerColor(SignalState.NO_SIGNAL);
      Toast.makeText(getActivity(),
          getString(R.string.location_unavailable),
          Toast.LENGTH_SHORT).show();
    } else {
      setSpinnerColor(SignalState.POOR_SIGNAL);
    }

    mDistanceTextView.setText(getActivity().getString(R.string.distance,
        "-"));
  }

  // TODO: Filter by distance?

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, mSelectedItemIndex);
  }

  @Override
  public void onResume() {
    super.onResume();

    mGeoProvider.start();
    if (mGeoProvider.isGpsProviderOn()
        || mGeoProvider.isNetworkOn()) {
      setSpinnerColor(SignalState.POOR_SIGNAL);
      mSignalQualitySpinner.startSpinning();
    }

    // TODO: Configure accuracy thresholds
    // TODO: Configure form to launch
    // TODO: Configure filter by place name
  }

  @Override
  public void onPause() {
    super.onPause();

    mGeoProvider.stop();

    mSignalQualitySpinner.stopSpinning();
    mSignalQualitySpinner.setText(getString(R.string.acc_value));
  }

  @Override
  public void onLocationChanged(Location location) {
    updateNotification();
    if (isAdded()) {
      updateDistance(location);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {

    if (mGeoProvider.isGpsProviderOn() == false
        && mGeoProvider.isNetworkOn() == false) {
      updateNotification();

      mSignalQualitySpinner.stopSpinning();
      mSignalQualitySpinner.setText(getString(R.string.acc_value));

      setSpinnerColor(SignalState.NO_SIGNAL);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if (mGeoProvider.isGpsProviderOn()
        || mGeoProvider.isNetworkOn()) {
      updateNotification();
      setSpinnerColor(SignalState.POOR_SIGNAL);
      mSignalQualitySpinner.startSpinning();
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    switch (status) {
    case LocationProvider.AVAILABLE:
      if (mGeoProvider.getCurrentLocation() != null) {
        updateNotification();
      }
      break;
    case LocationProvider.OUT_OF_SERVICE:
      break;
    case LocationProvider.TEMPORARILY_UNAVAILABLE:
      break;
    }
  }

  @Override
  public void onHeadingToNorthChanged(float heading) {
    if (isAdded()) {
      mHeadingTextView.setText(getActivity().getString(R.string.heading,
          String.valueOf((int) (heading)),
          mGeoProvider.getDegToGeo(heading)));
      mCompass.setDegrees(heading);
    }
  }

  @Override
  public void onBearingToCensusLocationChanged(float bearing, float heading) {
    if (isAdded()) {
      mCensusLocation.setVisibility(View.VISIBLE);
      mBearingTextView.setText(getActivity().getString(R.string.bearing,
          String.valueOf((int) (bearing)),
          mGeoProvider.getDegToGeo(bearing)));

      float rotation = 360 - bearing + heading;

      mCensusLocation.setDegrees(rotation);
    }
  }

  private void updateDistance(Location location) {
    if (mGeoProvider.getDestinationLocation() != null) {
      double distance = DistanceUtil.getDistance(mGeoProvider
          .getDestinationLocation().getLatitude(), mGeoProvider
          .getDestinationLocation().getLongitude(), location
          .getLatitude(), location.getLongitude());
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance,
          DistanceUtil.getFormatedDistance(distance)));
    }
  }

  private void updateNotification() {
    Location location = mGeoProvider.getCurrentLocation();
    if (isAdded() && location != null) {
      mSignalQualitySpinner.setText(truncateDouble(
          location.getAccuracy(), 2)
          + " " + getString(R.string.meter_shorthand));

      if (location.getAccuracy() > 0
          && location.getAccuracy() <= GOOD_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.GOOD_SIGNAL);
      } else if (location.getAccuracy() > GOOD_LOCATION_ACCURACY
          && location.getAccuracy() <= MODERATE_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.MODERATE_SIGNAL);
      } else if (location.getAccuracy() > MODERATE_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.POOR_SIGNAL);
      } else {
        setSpinnerColor(SignalState.NO_SIGNAL);
      }
    }
  }

  private void setSpinnerColor(SignalState signal) {
    Activity activity = getActivity();

    int textColor;
    int barColor;
    int rimColor;// circle border color

    switch (signal) {
    case GOOD_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_green);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_green);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_green);
      break;
    case MODERATE_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_yellow);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_yellow);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_yellow);
      break;
    case POOR_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_red);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_red);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_red);
      break;
    case NO_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_black);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_black);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_black);
      break;
    default:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_black);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_black);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_black);
      break;
    }

    mSignalQualitySpinner.setTextColor(textColor);
    mSignalQualitySpinner.setBarColor(barColor);
    mSignalQualitySpinner.setRimColor(rimColor);
  }

  private String truncateDouble(double number, int digitsAfterDouble) {
    StringBuilder numOfDigits = new StringBuilder();
    for (int i = 0; i < digitsAfterDouble; i++) {
      numOfDigits.append("#");
    }
    DecimalFormat df = new DecimalFormat("#"
        + (digitsAfterDouble > 0 ? "." + numOfDigits.toString() : ""));
    return df.format(number);
  }


  /**
   * Resets the view (the list), and sets the visibility to visible.
   */
  void resetView() {

    if (mSelectedItemIndex == INVALID_INDEX) {
      mGeoProvider.setDestinationLocation(null);
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance, "-"));
      return;
    }

    Row selectedRow = mTable.getRowAtIndex(mSelectedItemIndex);
    String lat = selectedRow.getDataByKey(mLatitudeColumn.getElementKey());
    String lon = selectedRow.getDataByKey(mLongitudeColumn.getElementKey());

    Location destination = new Location(""); // TODO: Do we need a provider?
    destination.setLatitude(Double.parseDouble(lat));
    destination.setLongitude(Double.parseDouble(lon));

    //destination.setAccuracy((float) census.getAccuracy());
    //destination.setAltitude(census.getAltitude()); // TODO: Do we need/have altitude and accuracy?

    mGeoProvider.setDestinationLocation(destination);
    if (mGeoProvider.getCurrentLocation() != null) {
      updateDistance(mGeoProvider.getCurrentLocation());
    } else {
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance, "-"));
    }
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

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    // TODO: Return which row ID we navigated to
  }

  private String getLatitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLatitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

  private String getLongitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLongitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

}