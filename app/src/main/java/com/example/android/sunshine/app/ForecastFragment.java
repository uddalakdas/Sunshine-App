package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rishi on 10/04/16.
 */
public class ForecastFragment extends Fragment {
    private final String LOG_TAG = FetchWeatherDataTask.class.getSimpleName();
    ArrayAdapter<String> adapter;
    public ForecastFragment() {
    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment,menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == R.id.action_refresh) {
            new FetchWeatherDataTask().execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
       // String weatherData = getWeatherData();
        List<String> weekForecast = new ArrayList<>(Arrays.asList(data));
        adapter = new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,weekForecast);
        ListView listView = (ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(adapter);
        return rootView;
    }



    public class FetchWeatherDataTask extends AsyncTask<String,Void,String[]>{

        @Override
        protected String[] doInBackground(String... params){

            if(params.length==0)
                return null;

            String weatherData = null;
            BufferedReader reader = null;
            HttpURLConnection urlConnection = null;
            String query_value = params[0];
            String mode_value = "json";
            String unit_value = "metrics";
            String count_value = "7";
            String appId = "592de62616b37b9fdf89a0fb773a3c45";
            String forecasts[] = new String[Integer.parseInt(count_value)];
            try{
                final String QUERY_PARAM = "q";
                final String MODE_PARAM = "mode";
                final String UNIT_PARAM = "units";
                final String COUNT_PARAM = "cnt";
                final String APPID = "APPID";

                Uri.Builder uri = new Uri.Builder();
                uri.scheme("http")
                .authority("api.openweathermap.org")
                .appendPath("data")
                .appendPath("2.5")
                .appendPath("forecast")
                .appendPath("daily")
                .appendQueryParameter(QUERY_PARAM,query_value)
                .appendQueryParameter(MODE_PARAM,mode_value)
                .appendQueryParameter(UNIT_PARAM,unit_value)
                .appendQueryParameter(COUNT_PARAM,count_value)
                .appendQueryParameter(APPID,appId);
                URL url = new URL(uri.build().toString());
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if(inputStream == null)
                    return null;
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while((line = reader.readLine()) != null){
                    buffer.append(line + "\n");
                }
                if(buffer.length() == 0){
                    return null;
                }
                weatherData = buffer.toString();
            }catch (IOException e){
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }finally {
                if(urlConnection!=null){
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                return processWeatherData(weatherData);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error Parsing JSON", e);
                return null;
            }

            //Log.i(LOG_TAG,weatherData);
            //return weatherData;
            //return null;
        }
        public String[] processWeatherData(String weatherData) throws JSONException {
            String forecasts[] = null;
            final String LIST = "list";
            final String TEMP = "temp";
            final String MAX = "max";
            final String MIN = "min";
            final String WEATHER = "weather";
            final String MAIN = "main";

            JSONObject jsonObject = new JSONObject(weatherData);
            JSONArray jsonArray = (JSONArray)jsonObject.get(LIST);
            int length = jsonArray.length();
            forecasts = new String[length];
            for(int i=0;i<length;i++){
                JSONObject listObject = (JSONObject)jsonArray.get(i);
                JSONObject tempObject = (JSONObject)listObject.get(TEMP);
                double min = (double)(tempObject.get("min"));
                double max = (double)(tempObject.get("max"));

                JSONArray weatherArray = (JSONArray)(listObject.get("weather"));
                JSONObject weatherObject = (JSONObject)weatherArray.get(0);
                String weather = (String)weatherObject.get(MAIN);
                forecasts[i] = getOrganisedMsg(i,weather,max,min);
                Log.i(LOG_TAG,forecasts[i]);
            }
            return forecasts;
        }
        public String getOrganisedMsg(int dayIndex,String weather,double max,double min){
            String dateString = getDateString(dayIndex);
            String max_min_formatted = ((long)Math.round(max)) + "/" + ((long)Math.round(min));
            return dateString + " - " + weather + " - " + max_min_formatted;
        }
        public String getDateString(int dayIndex){
            Time time = new Time();
            time.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),time.gmtoff);
            time = new Time();

            long dateTime = time.setJulianDay(julianStartDay+dayIndex);
            return new SimpleDateFormat("EEE MMM dd").format(dateTime);
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null){
                adapter.clear();
                for(String string:strings){
                    adapter.add(string);
                }
            }
        }
    }
}