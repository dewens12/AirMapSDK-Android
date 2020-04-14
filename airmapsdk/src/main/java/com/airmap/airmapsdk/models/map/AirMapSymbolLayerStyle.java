package com.airmap.airmapsdk.models.map;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.functions.stops.Stop;
import com.mapbox.mapboxsdk.style.functions.stops.Stops;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapSymbolLayerStyle extends AirMapLayerStyle {

    public final String iconImage;
    public final Function iconImageFunction;
    public final boolean avoidEdges;
    public final int symbolSpacing;
    public final double iconPadding;
    public final Function iconSizeFunction;
    public final String textField;
    public final String[] textFont;
    public final Function textSizeFunction;
    public final Float[] textOffsets;
    public final String textColor;
    public final int textPadding;
    public final boolean iconAllowOverlap;
    public final boolean iconKeepUpright;
    public final Function textOpacityFunction;
    public final Function iconOpacityFunction;

    protected AirMapSymbolLayerStyle(@NonNull JSONObject json) {
        super(json);

        JSONObject layoutJson = json.optJSONObject("layout");
        JSONObject iconImageJson = layoutJson.optJSONObject("icon-image");
        if (iconImageJson != null) {
            iconImage = null;
            iconImageFunction = getImageIconFunction(iconImageJson);
        } else {
            iconImage = optString(layoutJson, "icon-image");
            iconImageFunction = null;
        }
        avoidEdges = layoutJson.optBoolean("symbol-avoid-edges");
        symbolSpacing = layoutJson.optInt("symbol-spacing");
        iconPadding = layoutJson.optDouble("icon-padding");

        JSONObject iconSizeJson = layoutJson.optJSONObject("icon-size");
        if (iconImageJson != null) {
            iconSizeFunction = getZoomFunction(iconSizeJson);
        } else {
            iconSizeFunction = null;
        }

        textField = optString(layoutJson, "text-field");

        JSONArray textFontArray = layoutJson.optJSONArray("text-font");
        textFont = textFontArray != null ? new String[textFontArray.length()] : null;
        for (int i = 0; textFontArray != null && i < textFontArray.length(); i++) {
            textFont[i] = textFontArray.optString(i);
        }

        JSONObject textSizeJson = layoutJson.optJSONObject("text-size");
        if (textSizeJson != null) {
            textSizeFunction = getZoomFunction(textSizeJson);
        } else {
            textSizeFunction = null;
        }

        JSONArray textOffsetsArray = layoutJson.optJSONArray("text-offset");
        if (textOffsetsArray != null) {
            textOffsets = new Float[textOffsetsArray.length()];
            for (int i = 0; i < textOffsetsArray.length(); i++) {
                textOffsets[i] = (float) textOffsetsArray.optDouble(i);
            }
        } else {
            textOffsets = null;
        }

        textPadding = layoutJson.optInt("text-padding");

        JSONObject paintJson = json.optJSONObject("paint");
        if (paintJson != null) {
            textColor = optString(paintJson, "text-color");
            JSONObject textOpacityJson = paintJson.optJSONObject("text-opacity");
            if (textOpacityJson != null) {
                textOpacityFunction = getOpacityFunction(textOpacityJson);
            } else {
                textOpacityFunction = null;
            }

            JSONObject iconOpacityJson = paintJson.optJSONObject("icon-opacity");
            if (iconOpacityJson != null) {
                iconOpacityFunction = getOpacityFunction(iconOpacityJson);
            } else {
                iconOpacityFunction = null;
            }

        } else {
            textColor = "#000000";
            textOpacityFunction = null;
            iconOpacityFunction = null;
        }

        iconAllowOverlap = layoutJson.optBoolean("icon-allow-overlap");
        iconKeepUpright = layoutJson.optBoolean("icon-keep-upright");
    }

    public boolean isBackgroundStyle() {
        return id.contains("background");
    }

    public static Function getImageIconFunction(JSONObject imageIconJson) {
        String property = optString(imageIconJson, "property");
        String type = optString(imageIconJson, "type");
        String defaultIcon = optString(imageIconJson, "default");
        JSONArray stopsArray = imageIconJson.optJSONArray("stops");

        switch (type) {
            case "categorical": {
                Stop[] stops;
                if (stopsArray != null) {
                    stops = new Stop[stopsArray.length()];
                    for (int i = 0; i < stopsArray.length(); i++) {
                        JSONArray stopArray = stopsArray.optJSONArray(i);
                        if (stopArray != null) {
                            Object value1 = stopArray.opt(0);
                            Object value2 = stopArray.opt(1);
                            stops[i] = (Stop.stop(value1, PropertyFactory.iconImage((String) value2)));
                        }
                    }
                } else {
                    stops = new Stop[0];
                }

                return Function.property(property, Stops.categorical(stops)).withDefaultValue(new PropertyValue("default", defaultIcon));
            }
        }

        return null;
    }

    public static Function getZoomFunction(JSONObject iconSizeJson) {
        float defaultSize = (float) iconSizeJson.optDouble("default", 1.0f);
        float baseSize = (float) iconSizeJson.optDouble("base", 1f);
        JSONArray stopsArray = iconSizeJson.optJSONArray("stops");
        Stop[] stops;
        if (stopsArray != null) {
            stops = new Stop[stopsArray.length()];
            for (int i = 0; i < stopsArray.length(); i++) {
                JSONArray stopArray = stopsArray.optJSONArray(i);
                if (stopArray != null) {
                    Object value1 = stopArray.opt(0);
                    float value2 = (float) stopArray.optDouble(1);
                    stops[i] = Stop.stop(value1, PropertyFactory.iconSize(value2));
                }
            }
        } else {
            stops = new Stop[0];
        }

        return Function.zoom(Stops.exponential(stops));
    }

    public static Function getOpacityFunction(JSONObject opacityJson) {
        float defaultSize = (float) opacityJson.optDouble("default", 1.0f);
        float baseSize = (float) opacityJson.optDouble("base", 1f);
        JSONArray stopsArray = opacityJson.optJSONArray("stops");
        Stop[] stops;
        if (stopsArray != null) {
            stops = new Stop[stopsArray.length()];
            for (int i = 0; i < stopsArray.length(); i++) {
                JSONArray stopArray = stopsArray.optJSONArray(i);
                if (stopArray != null) {
                    Object value1 = stopArray.opt(0);
                    float value2 = (float) stopArray.optDouble(1);
                    stops[i] = Stop.stop(value1, PropertyFactory.textOpacity(value2));
                }
            }
        } else {
            stops = new Stop[0];
        }

        return Function.zoom(Stops.exponential(stops));
    }

    @Override
    public SymbolLayer toMapboxLayer(Layer layerToClone, String sourceId) {
        SymbolLayer layer = new SymbolLayer(id + "|" + sourceId + "|new", sourceId);
        layer.setSourceLayer(sourceId + "_" + sourceLayer);

        if (iconImageFunction != null) {
            layer.setProperties(PropertyFactory.iconImage(iconImageFunction));
        } else {
            layer.setProperties(PropertyFactory.iconImage(iconImage));
        }
        layer.setProperties(PropertyFactory.symbolAvoidEdges(avoidEdges));
        layer.setProperties(PropertyFactory.symbolSpacing((float) symbolSpacing));
        layer.setProperties(PropertyFactory.iconPadding((float) iconPadding));

        if (iconSizeFunction != null) {
            layer.setProperties(PropertyFactory.iconSize(iconSizeFunction));
        }

        layer.setProperties(PropertyFactory.textField(textField));

        if (textFont != null) {
            layer.setProperties(PropertyFactory.textFont(textFont));
        }

        if (textSizeFunction != null) {
            layer.setProperties(PropertyFactory.textSize(textSizeFunction));
        }

        if (textOffsets != null) {
            layer.setProperties(PropertyFactory.textOffset(textOffsets));
        }

        layer.setProperties(PropertyFactory.textPadding((float) textPadding));

        layer.setProperties(PropertyFactory.iconAllowOverlap(iconAllowOverlap));
        layer.setProperties(PropertyFactory.iconKeepUpright(iconKeepUpright));

        if (minZoom != 0) {
            layer.setMinZoom(minZoom);
        }

        if (filter != null) {
            layer.setFilter(filter);
        }

        if (textColor != null) {
            layer.setProperties(PropertyFactory.textColor(textColor));
        }

        if (textOpacityFunction != null) {
            layer.setProperties(PropertyFactory.textOpacity(textOpacityFunction));
        }

        if (iconOpacityFunction != null) {
            layer.setProperties(PropertyFactory.iconOpacity(iconOpacityFunction));
        }

        return layer;
    }
}
