package com.airmap.airmapsdk.networking.services;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airmap.airmapsdk.models.airspace.AirMapAirspace;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericListOkHttpCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericOkHttpCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class AirspaceService extends BaseService {

    /**
     * Get an airspace by its ID
     *  @param airspaceId The ID of the airspace to get
     * @param listener   The callback that is invoked on success or error
     */
    public static Call getAirspace(@NonNull String airspaceId,
                                   @Nullable AirMapCallback<AirMapAirspace> listener) {
        return AirMap.getClient().get(String.format(airspaceByIdUrl, airspaceId), new GenericOkHttpCallback(listener, AirMapAirspace.class));
    }

    /**
     * Get airspaces by a list of their IDs
     *
     * @param airspaceIds The IDs of the airspaces to get
     * @param listener    The callback that is invoked on success or error
     */
    public static Call getAirspace(@NonNull List<String> airspaceIds,
                                   @Nullable AirMapCallback<List<AirMapAirspace>> listener) {
        Map<String, String> params = new HashMap<>();
        params.put("ids", TextUtils.join(",", airspaceIds));
        return AirMap.getClient().get(airspaceByIdsUrl, params, new GenericListOkHttpCallback(listener, AirMapAirspace.class));
    }
}
