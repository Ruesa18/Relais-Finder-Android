package ch.ruefenacht.sandro.relaisfinder;

import org.json.JSONException;
import org.json.JSONObject;

public class RadioRepeaterModel {

    public int ID;
    public String title;
    public String callsign;
    public Double frequencyIn;
    public Double frequencyOut;

    public RadioRepeaterModel(JSONObject jsonObject) {
        try {
            this.ID = jsonObject.has("ID") ? (int) jsonObject.get("ID") : 0;
            this.title = jsonObject.has("title") ? (String) jsonObject.get("title") : "";
            this.callsign = jsonObject.has("callsign") ? (String) jsonObject.get("callsign") : "";
            this.frequencyIn = jsonObject.has("frequencyIn") ? (Double) jsonObject.get("frequencyIn") : 0f;
            this.frequencyOut = jsonObject.has("frequencyOut") ? (Double) jsonObject.get("frequencyOut") : 0f;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
