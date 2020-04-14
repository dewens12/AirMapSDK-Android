package com.airmap.airmapsdk.models;

import com.airmap.airmapsdk.util.Utils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapWeatherUpdate implements AirMapBaseModel, Serializable {

    private Date time;
    private String timezone;
    private String condition;
    private String icon;
    private Wind wind;
    private double humidity;
    private double visibility;
    private double precipitation;
    private double temperature;
    private double dewPoint;
    private double mslp;

    public AirMapWeatherUpdate(JSONObject jsonObject) {
        constructFromJson(jsonObject);
    }

    public AirMapWeatherUpdate() {}

    @Override
    public AirMapWeatherUpdate constructFromJson(JSONObject json) {
        if (json != null) {
            String time = optString(json, "time");
            setTime(Utils.getDateFromIso8601String(time));
            setTimezone(optString(json, "timezone"));
            setCondition(optString(json, "condition"));
            setIcon(optString(json, "icon").replace("-", "_"));

            JSONObject windJSON = json.optJSONObject("wind");
            if (windJSON != null) {
                Wind wind = new Wind();
                wind.setHeading(windJSON.optInt("heading"));
                wind.setSpeed(windJSON.optInt("speed"));
                wind.setGusting(windJSON.optInt("gusting"));

                setWind(wind);
            }

            setHumidity(json.optDouble("humidity"));
            setVisibility(json.optDouble("visibility"));
            setPrecipitation(json.optDouble("precipitation"));
            setTemperature(json.optDouble("temperature"));
            setDewPoint(json.optDouble("dew_point"));
            setMslp(json.optDouble("mslp"));
        }
        return null;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getVisibility() {
        return visibility;
    }

    public void setVisibility(double visibility) {
        this.visibility = visibility;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public double getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(double dewPoint) {
        this.dewPoint = dewPoint;
    }

    public double getMslp() {
        return mslp;
    }

    public void setMslp(double mslp) {
        this.mslp = mslp;
    }

    /**
     * Determine the validity of the current weather
     *
     * @return whether the weather is valid or not
     */
    public boolean isValid() {
        return wind != null && condition != null && !condition.isEmpty();
    }

    public class Wind {
        int heading;
        int speed;
        int gusting;

        public int getHeading() {
            return heading;
        }

        public void setHeading(int heading) {
            this.heading = heading;
        }

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public int getGusting() {
            return gusting;
        }

        public void setGusting(int gusting) {
            this.gusting = gusting;
        }
    }
}
