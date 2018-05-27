package com.example.amitwalke.mygoolgemapsearch.homeinfo.model;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.amitwalke.mygoolgemapsearch.BuildConfig;
import com.example.amitwalke.mygoolgemapsearch.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class AddressAutoCompleteAdapter<AutocompletePrediction> extends BaseAdapter implements Filterable {

    private final Activity activity;
    private final LayoutInflater inflater;
    /**
     * Handles autocomplete requests.
     */
    private GoogleApiClient mGoogleApiClient;
    /**
     * The autocomplete filter used to restrict queries to a specific set of place types.
     */
    private AutocompleteFilter mPlaceFilter;
    /**
     * Current results returned by this adapter.
     */
    private ArrayList<com.google.android.gms.location.places.AutocompletePrediction> mResultList = new ArrayList<com.google.android.gms.location.places.AutocompletePrediction>();
    /**
     * The bounds used for Places Geo Data autocomplete API requests.
     */
    private LatLngBounds mBounds;
    private static final CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    private Place myPlace;

    public AddressAutoCompleteAdapter(Activity activity, GoogleApiClient mGoogleApiClient, LatLngBounds bounds, AutocompleteFilter mPlaceFilter) {
        this.activity = activity;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mGoogleApiClient = mGoogleApiClient;
        this.mPlaceFilter = mPlaceFilter;
        mBounds = bounds;
    }

    /**
     * Sets the bounds for all subsequent queries.
     */
    public void setBounds(LatLngBounds bounds) {
        mBounds = bounds;
    }

    /**
     * Returns the number of results received in the last autocomplete query.
     */
    @Override
    public int getCount() {
        if (mResultList != null && mResultList.size() > 0) {
            return mResultList.size();
        } else {
            return 0;
        }
    }

    /**
     * Returns an item from the last autocomplete query.
     */
    @Override
    public com.google.android.gms.location.places.AutocompletePrediction getItem(int position) {
        if (position < mResultList.size())
            return mResultList.get(position);
        else
            return null;
    }


    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_place_list, parent, false);
            new ViewHolder(convertView);
        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        if (holder != null) {
            final com.google.android.gms.location.places.AutocompletePrediction item = getItem(position);
            if (item != null) {
                holder.txtMainAddress.setText(item.getPrimaryText(STYLE_BOLD));
                Places.GeoDataApi.getPlaceById(mGoogleApiClient, item.getPlaceId())
                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (places.getStatus().isSuccess() && places.getCount() > 0) {
                                    myPlace = places.get(0);
                                    if (holder.txtSubAddress != null) {
                                        holder.txtSubAddress.setText(myPlace.getAddress().toString());
                                    }
                                } else {
                                    Log.e("qwert", "Place not found");
                                }
                                places.release();
                            }
                        });
            }
        }
        return convertView;
    }

    private class ViewHolder {
        TextView txtMainAddress;
        TextView txtSubAddress;

        public ViewHolder(View view) {
            txtMainAddress = (TextView) view.findViewById(R.id.txt_main_address);
            txtSubAddress = (TextView) view.findViewById(R.id.txt_sub_address);
            view.setTag(this);
        }
    }

    /**
     * Returns the filter for the current set of autocomplete results.
     */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                // Skip the autocomplete query if no constraints are given.
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    mResultList = getAutocomplete(constraint);
                    if (mResultList != null) {
                        // The API successfully returned results.
                        results.values = mResultList;
                        results.count = mResultList.size();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged();
                } else {
                    try {
                        // The API did not return any results, invalidate the data set.
                        notifyDataSetInvalidated();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                // Override this method to display a readable result in the AutocompleteTextView
                // when clicked.
                if (resultValue instanceof com.google.android.gms.location.places.AutocompletePrediction) {
                    return ((com.google.android.gms.location.places.AutocompletePrediction) resultValue).getFullText(null);
                } else {
                    return super.convertResultToString(resultValue);
                }
            }
        };
    }

    /**
     * Submits an autocomplete query to the Places Geo Data Autocomplete API.
     * Results are returned as frozen AutocompletePrediction objects, ready to be cached.
     * objects to store the Place ID and description that the API returns.
     * Returns an empty list if no results were found.
     * Returns null if the API client is not available or the query did not complete
     * successfully.
     * This method MUST be called off the main UI thread, as it will block until data is returned
     * from the API, which may include a network request.
     *
     * @param constraint Autocomplete query string
     * @return Results from the autocomplete API or null if the query was not successful.
     * @see Places#GEO_DATA_API#getAutocomplete(CharSequence)
     * @see com.google.android.gms.location.places.AutocompletePrediction#freeze()
     */
    private ArrayList<com.google.android.gms.location.places.AutocompletePrediction> getAutocomplete(CharSequence constraint) {
        if (mGoogleApiClient.isConnected()) {

//            String zwangiToUnicode = ZwagyiToUnicodeUtils.zg2uni(constraint.toString());
            // Submit the query to the autocomplete API and retrieve a PendingResult that will
            // contain the results when the query completes.

            mPlaceFilter = getCountryWiseFilter();

            // mPlaceFilter = getRegionWiseFilter();

            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi
                            .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                    mBounds, null);

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.
            AutocompletePredictionBuffer autocompletePredictions = results
                    .await(60, TimeUnit.SECONDS);

            // Confirm that the query completed successfully, otherwise return null
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {

                autocompletePredictions.release();
                return null;
            }

            // Freeze the results immutable representation that can be stored safely.
            return DataBufferUtils.freezeAndClose(autocompletePredictions);
        }
        return null;
    }

    private AutocompleteFilter getCountryWiseFilter() {

        String countryName = BuildConfig.AUTOCOMPLETE_SUGGESTION_COUNTRY;

        AutocompleteFilter autocompleteFilter = null;

        if (!TextUtils.isEmpty(countryName)) {

            autocompleteFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(Place.TYPE_COUNTRY).setCountry(countryName)
                    .build();
        }

        return autocompleteFilter;
    }

    private AutocompleteFilter getRegionWiseFilter() {

        AutocompleteFilter autocompleteFilter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_REGIONS).build();

        return autocompleteFilter;
    }
}