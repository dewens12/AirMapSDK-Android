package com.airmap.airmapsdk.networking.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airmap.airmapsdk.models.aircraft.AirMapAircraftManufacturer;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraftModel;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericListOkHttpCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericOkHttpCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

@SuppressWarnings("unused")
class AircraftService extends BaseService {

    /**
     * Get a list of all Aircraft Manufacturers
     *
     * @param listener The callback that is invoked on success or error
     */
    static Call getManufacturers(final AirMapCallback<List<AirMapAircraftManufacturer>> listener) {
        String url = aircraftManufacturersUrl;
        return AirMap.getClient().get(url, new GenericListOkHttpCallback(listener, AirMapAircraftManufacturer.class));
    }

    /**
     * Get a list of all Aircraft Manufacturers
     *
     * @param query    manufacturer to search for
     * @param listener The callback that is invoked on success or error
     */
    static Call getManufacturers(String query, final AirMapCallback<List<AirMapAircraftManufacturer>> listener) {
        String url = aircraftManufacturersUrl;
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        return AirMap.getClient().get(url, params, new GenericListOkHttpCallback(listener, AirMapAircraftManufacturer.class));
    }

    /**
     * Get a list of all Aircraft Models
     *
     * @param listener The callback that is invoked on success or error
     */
    static Call getModels(final AirMapCallback<List<AirMapAircraftModel>> listener) {
        String url = aircraftModelsUrl;
        return AirMap.getClient().get(url, new GenericListOkHttpCallback(listener, AirMapAircraftModel.class));
    }

    /**
     * Get a model by ID
     *
     * @param query          The model to search for
     * @param manufacturerId The ID of the manufacturer to get models for
     * @param listener       The callback that is invoked on success or error
     */
    static Call getModels(@Nullable String query, @Nullable String manufacturerId, final AirMapCallback<List<AirMapAircraftModel>> listener) {
        String url = aircraftModelsUrl;
        Map<String, String> params = new HashMap<>();
        params.put("manufacturer", manufacturerId);
        params.put("q", query);
        return AirMap.getClient().get(url, params, new GenericListOkHttpCallback(listener, AirMapAircraftModel.class));
    }

    /**
     * Get a model by ID
     *
     * @param manufacturerId The ID of the manufacturer to get models for
     * @param listener       The callback that is invoked on success or error
     */
    static Call getModels(String manufacturerId, final AirMapCallback<List<AirMapAircraftModel>> listener) {
        String url = aircraftModelsUrl;
        Map<String, String> params = new HashMap<>();
        params.put("manufacturer", manufacturerId);
        return AirMap.getClient().get(url, params, new GenericListOkHttpCallback(listener, AirMapAircraftModel.class));
    }

    /**
     * Get a model by ID
     *
     * @param modelId  The ID of the model to get
     * @param listener The callback that is invoked on success or error
     */
    static Call getModel(@NonNull String modelId, final AirMapCallback<AirMapAircraftModel> listener) {
        String url = String.format(aircraftModelUrl, modelId);
        return AirMap.getClient().get(url, new GenericOkHttpCallback(listener, AirMapAircraftModel.class));
    }
}
