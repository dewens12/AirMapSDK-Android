package com.airmap.airmapsdk.networking.services;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.util.AirMapConfig;
import com.airmap.airmapsdk.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import timber.log.Timber;

@SuppressWarnings("unused")
public class MappingService extends BaseService {

    public enum AirMapLayerType implements Serializable {
        TFRS("tfrs", R.string.tile_layer_tfr_faa),
        Wildfires("wildfires", R.string.tile_layer_wildfires),
        Fires("fires", R.string.tile_layer_fires),
        Emergencies("emergencies", R.string.tile_layer_emergencies),
        Prohibited("sua_prohibited", R.string.tile_layer_prohibited),
        Restricted("sua_restricted", R.string.tile_layer_restricted_airspace),
        NationalParks("national_parks", R.string.tile_layer_national_parks),
        NOAA("noaa", R.string.tile_layer_noaa),
        Schools("schools", R.string.tile_layer_schools),
        Hospitals("hospitals", R.string.tile_layer_hospitals),
        Heliports("heliports", R.string.tile_layer_heliports),
        PowerPlants("power_plants", R.string.tile_layer_power_plants),
        AirportsCommercial("airports_commercial,airports_commercial_private", R.string.tile_layer_airports),
        AirportsRecreational("airports_recreational,airports_recreational_private", R.string.tile_layer_airports),
        AirportsCommercialPrivate("airports_commercial_private", R.string.tile_layer_private_airports),
        AirportsRecreationalPrivate("airports_recreational_private", R.string.tile_layer_private_airports),
        ClassB("class_b", R.string.tile_layer_class_b),
        ClassC("class_c", R.string.tile_layer_class_c),
        ClassD("class_d", R.string.tile_layer_class_d),
        ClassE("class_e0", R.string.tile_layer_class_e),
        ClassF("class_f", R.string.tile_layer_class_f),
        HazardAreas("hazard_areas", R.string.tile_layer_hazard_areas),
        AerialRecreationalAreas("aerial_recreational_areas", R.string.tile_layer_aerial_rec_areas),
        DenselyPopulatedRegion("densely_populated_region", R.string.tile_layer_did),
        Cities("cities", R.string.tile_layer_cities),
        Custom("custom", R.string.tile_layer_custom),
        Prisons("prisons", R.string.tile_layer_prisons),
        Universities("universities", R.string.tile_layer_universities),
        SeaplaneBase("seaplane_base", R.string.tile_layer_seaplane_base),
        Other("aerial_recreational_areas,custom,hazard_areas,hospitals,power_plants,prisons,schools,universities,cities", R.string.tile_layer_other_cautionary_areas),
        Notam("notam", R.string.notams),
        AMA("ama_field", R.string.amas),
        Unknown("type_not_found", R.string.tile_layer_unknown);

        private final String text;
        private final int title;

        AirMapLayerType(String text, @StringRes int title) {
            this.text = text;
            this.title = title;
        }

        @Override
        public String toString() {
            return text;
        }

        public int getTitle() {
            return title;
        }

        public static AirMapLayerType fromString(String text) {
            switch (text) {
                case "tfrs":
                    return TFRS;
                case "wildfires":
                    return Wildfires;
                case "fires":
                    return Fires;
                case "emergencies":
                    return Emergencies;
                case "sua_prohibited":
                    return Prohibited;
                case "sua_restricted":
                    return Restricted;
                case "national_parks":
                    return NationalParks;
                case "noaa":
                    return NOAA;
                case "schools":
                    return Schools;
                case "hospitals":
                    return Hospitals;
                case "heliports":
                    return Heliports;
                case "power_plants":
                    return PowerPlants;
                case "airports_commercial,airports_commercial_private":
                case "airports_commercial":
                    return AirportsCommercial;
                case "airports_recreational":
                case "airports_recreational,airports_recreational_private":
                    return AirportsRecreational;
                case "airports_commercial_private":
                    return AirportsCommercialPrivate;
                case "airports_recreational_private":
                    return AirportsRecreationalPrivate;
                case "class_b":
                    return ClassB;
                case "class_c":
                    return ClassC;
                case "class_d":
                    return ClassD;
                case "class_e0":
                    return ClassE;
                case "class_f":
                    return ClassF;
                case "hazard_areas":
                    return HazardAreas;
                case "aerial_recreational_areas":
                    return AerialRecreationalAreas;
                case "densely_populated_region":
                    return DenselyPopulatedRegion;
                case "cities":
                    return Cities;
                case "custom":
                    return Custom;
                case "prisons":
                    return Prisons;
                case "universities":
                    return Universities;
                case "seaplane_base":
                    return SeaplaneBase;
                case "aerial_recreational_areas,custom,hazard_areas,hospitals,power_plants,prisons,schools,universities,cities":
                    return Other;
                case "notam":
                    return Notam;
                case "ama":
                case "ama_field":
                    return AMA;
                default:
                    return Unknown;
            }
        }

        public AirMapAirspaceType getAirspaceType() {
            switch (this) {
                case TFRS:
                    return AirMapAirspaceType.TFR;
                case Wildfires:
                    return AirMapAirspaceType.Wildfires;
                case Fires:
                    return AirMapAirspaceType.Fires;
                case Emergencies:
                    return AirMapAirspaceType.Emergencies;
                case Prohibited:
                case Restricted:
                    return AirMapAirspaceType.SpecialUse;
                case NationalParks:
                case NOAA:
                    return AirMapAirspaceType.Park;
                case Schools:
                    return AirMapAirspaceType.School;
                case Hospitals:
                    return AirMapAirspaceType.Hospitals;
                case PowerPlants:
                    return AirMapAirspaceType.PowerPlant;
                case SeaplaneBase:
                case AirportsCommercial:
                case AirportsRecreational:
                case AirportsCommercialPrivate:
                case AirportsRecreationalPrivate:
                    return AirMapAirspaceType.Airport;
                case Heliports:
                case ClassB:
                case ClassC:
                case ClassD:
                case ClassE:
                case ClassF:
                    return AirMapAirspaceType.ControlledAirspace;
                case HazardAreas:
                    return AirMapAirspaceType.HazardArea;
                case AerialRecreationalAreas:
                    return AirMapAirspaceType.RecreationalArea;
                case DenselyPopulatedRegion:
                case Cities:
                    return AirMapAirspaceType.City;
                case Custom:
                    return AirMapAirspaceType.Custom;
                case Prisons:
                    return AirMapAirspaceType.Prison;
                case Universities:
                    return AirMapAirspaceType.University;
                case Notam:
                    return AirMapAirspaceType.Notam;
                case AMA:
                    return AirMapAirspaceType.AMA;
                case Unknown:
                    return AirMapAirspaceType.Unknown;
            }
            return null;
        }
    }

    public enum AirMapAirspaceType {
        Airport("airport", R.string.airspace_type_airport),
        Heliport("heliport", R.string.airspace_type_heliport),
        Park("park", R.string.airspace_type_national_park),
        PowerPlant("power_plant", R.string.airspace_type_power_plant),
        ControlledAirspace("controlled_airspace", R.string.airspace_type_controlled),
        School("school", R.string.airspace_type_school),
        SpecialUse("special_use_airspace", R.string.airspace_type_special_use),
        SeaplaneBase("seaplane_base", R.string.airspace_type_seaplane_base),
        TFR("tfr", R.string.airspace_type_tfr_faa),
        Wildfires("wildfire", R.string.airspace_type_wildfire),
        Fires("fire", R.string.airspace_type_fire),
        Emergencies("emergency", R.string.airspace_type_emergency),
        Hospitals("hospital", R.string.airspace_type_hospital),
        HazardArea("hazard_area", R.string.airspace_type_hazard_area),
        RecreationalArea("recreational_area", R.string.airspace_type_aerial_rec_area),
        City("city", R.string.airspace_type_city),
        Custom("custom", R.string.airspace_type_custom),
        Prison("prison", R.string.airspace_type_prison),
        University("university", R.string.airspace_type_university),
        Notam("notam", R.string.airspace_type_notam),
        AMA("ama_field", R.string.airspace_type_ama),
        County("county", R.string.county),
        Country("country", R.string.country),
        Embassy("embassy", R.string.embassy),
        FIR("fir", R.string.fir),
        FederalBuilding("federal_building", R.string.federal_building),
        GliderPort("gliderport", R.string.glider_port),
        Highway("highway", R.string.highway),
        IndustrialProperty("industrial_property", R.string.industrial_property),
        MilitaryProperty("military_property", R.string.military_property),
        PoliceStation("police_station", R.string.police_station),
        Powerline("powerline", R.string.powerline),
        Railway("railway", R.string.railway),
        ResidentialProperty("residential_property", R.string.residential_property),
        Stadium("stadium", R.string.stadium),
        State("state", R.string.state),
        Subprefecture("subprefecture", R.string.subprefecture),
        Supercity("supercity", R.string.supercity),
        UlmField("ulm_field", R.string.ulm_field),
        Waterway("waterway", R.string.waterway),
        JapanBase("jpn_base", R.string.japan_base_admin),
        Unknown("unknown", R.string.airspace_type_unknown);

        private final String text;
        private final int title;

        AirMapAirspaceType(String text, @StringRes int title) {
            this.text = text;
            this.title = title;
        }

        @Override
        public String toString() {
            return text;
        }

        public int getTitle() {
            return title;
        }

        public static AirMapAirspaceType fromString(String text) {
            switch (text) {
                case "airport":
                    return Airport;
                case "heliport":
                    return Heliport;
                case "park":
                    return Park;
                case "power_plant":
                    return PowerPlant;
                case "controlled_airspace":
                    return ControlledAirspace;
                case "school":
                    return School;
                case "special_use_airspace":
                    return SpecialUse;
                case "tfr":
                    return TFR;
                case "wildfire":
                    return Wildfires;
                case "fire":
                    return Fires;
                case "emergency":
                    return Emergencies;
                case "hospital":
                    return Hospitals;
                case "hazard_area":
                    return HazardArea;
                case "recreational_area":
                    return RecreationalArea;
                case "city":
                    return City;
                case "custom":
                    return Custom;
                case "prison":
                    return Prison;
                case "university":
                    return University;
                case "seaplane_base":
                    return SeaplaneBase;
                case "notam":
                    return Notam;
                case "ama":
                case "ama_field":
                    return AMA;
                case "country":
                    return Country;
                case "county":
                    return County;
                case "embassy":
                    return Embassy;
                case "fir":
                    return FIR;
                case "federal_building":
                    return FederalBuilding;
                case "gliderport":
                    return GliderPort;
                case "highway":
                    return Highway;
                case "industrial_property":
                    return IndustrialProperty;
                case "military_property":
                    return MilitaryProperty;
                case "police_station":
                    return PoliceStation;
                case "powerline":
                    return Powerline;
                case "railway":
                    return Railway;
                case "residential_property":
                    return ResidentialProperty;
                case "stadium":
                    return Stadium;
                case "state":
                    return State;
                case "subprefecture":
                    return Subprefecture;
                case "supercity":
                    return Supercity;
                case "ulm_field":
                    return UlmField;
                case "waterway":
                    return Waterway;
                default:
                    return Unknown;
            }
        }
    }

    public enum AirMapMapTheme {
        Standard("standard"),
        Dark("dark"),
        Light("light"),
        Satellite("satellite");

        private final String text;

        AirMapMapTheme(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static AirMapMapTheme fromString(String text) {
            switch (text) {
                case "standard":
                case "street":
                    return Standard;
                case "light":
                    return Light;
                case "dark":
                    return Dark;
                case "satellite":
                    return Satellite;
            }
            return null;
        }
    }

    @Deprecated
    protected String getTileSourceUrl(@Nullable List<AirMapLayerType> layers, AirMapMapTheme theme) {
        String tiles = (layers == null || layers.size() == 0) ? "_-_" : TextUtils.join(",", layers);
        return mapTilesBaseUrl + tiles + "?&theme=" + theme.toString() + "&apikey=" + AirMap.getInstance().getApiKey() + "&token=" + AirMap.getInstance().getApiKey();
    }

    protected String getRulesetTileUrlTemplate(String rulesetId, List<String> layers) {
        return mapTilesRulesUrl + rulesetId + "/" + TextUtils.join(",", layers) + "/{z}/{x}/{y}";
    }

    protected String getStylesUrl(AirMapMapTheme theme) {
        String stylesUrl = AirMapConfig.getMapStyleUrl();

        // fallback
        if (TextUtils.isEmpty(stylesUrl)) {
            stylesUrl = "https://cdn.airmap.com/static/map-styles/0.9.2/";
        }

        switch (theme) {
            case Light:
                stylesUrl += "light.json";
                break;
            case Dark:
                stylesUrl += "dark.json";
                break;
            case Satellite:
                stylesUrl += "satellite.json";
                break;
            default:
            case Standard:
                stylesUrl += "standard.json";
                break;
        }

        return stylesUrl;
    }

    protected Call getStylesJson(AirMapMapTheme theme, final AirMapCallback<JSONObject> listener) {
        return AirMap.getClient().get(getStylesUrl(theme), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.error(new AirMapException(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString;
                try {
                    jsonString = response.body().string();
                } catch (IOException e) {
                    Utils.error(listener, e);
                    return;
                }
                response.body().close();
                JSONObject result = null;
                try {
                    result = new JSONObject(jsonString);
                    listener.success(result);
                } catch (JSONException e) {
                    Timber.e(e, "Unable to parse map style:%s", jsonString);
                    listener.error(new AirMapException(e.getMessage()));
                }
            }
        });
    }
}
