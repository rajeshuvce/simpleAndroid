package com.example.android.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class FetchWeatherFragment extends Fragment {

    public FetchWeatherFragment() {
    }

    ArrayAdapter<String> mForecastAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Log.d("RJ", "onOptionsItemSelected: "+ id);

        if (id == R.id.action_refresh) {

            Log.d("RJ", "onOptionsItemSelected: callign fw"+ id);

            FetchWeatherFragment.FetchForecaseAsyncTask fw = new FetchWeatherFragment.FetchForecaseAsyncTask();
            fw.execute("bangalore,IN");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



        @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecaseArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/40",
                "Wed - Cloudy - 72/32",
                "Thurs - Astroids - 65/23",
                "Fri - Heavy Rain - 65/56",
                "Sat - Help trapped in weatherstateion - 60/51",
                "Sun - sunny - 80/68"
        };
        List<String> weekForeCast = new ArrayList<String>(
                Arrays.asList(forecaseArray));



        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.lsit_item_forecast_textview,
                weekForeCast
        );

        ListView listView = (ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String foreCast = mForecastAdapter.getItem(position);
                    //Toast.makeText(getActivity(), foreCast,Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent( getActivity(), DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, foreCast);
                    startActivity(intent);
                }
            });

        return rootView;
    }



    public class FetchForecaseAsyncTask extends AsyncTask <String, Void, String[]> {

        private final String LOG_TAG = FetchForecaseAsyncTask.class.getSimpleName();

        private String getReadableDateString(long time) {
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");

            Log.e("RJ:", "getReadableDateString: " + format.toString());
            return format.format(date).toString();
        }

        private String formatHighAndLow(double high, double low) {
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLow = Long.toString(roundedHigh)+"/"+Long.toString(roundedLow);
            return highLow;
        }

        @Override
        protected void onPostExecute(String[] result) {
            //super.onPostExecute(result);
            if (result != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(result);

            }
        }

        public String[] getWeatherDataFromJSON(String forecastJsonStr, int numDays) throws JSONException {
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "description";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStr = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForeCast = weatherArray.getJSONObject(i);

                long dateTime = dayForeCast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForeCast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForeCast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighAndLow(high, low);
                resultStr[i] = day + "_" + description + "_" + highAndLow;
            }

            for (String s : resultStr) {
                Log.v(LOG_TAG, "Forecast entry:" + s);
            }

            return resultStr;
        }

        @Override
        protected String[] doInBackground(String... params) {


            if (params.length == 0) {
                return null;
            }
            // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

// Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String format = "json";
            String units = "metric";
            int numDays = 14;
            String apiKey = "004876d0b135b638cdaa213cbc161aa2";
            Log.d(LOG_TAG, "doInBackground: have started ");
            try {

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String APPID = "APPID";
                final String QUERY_PARAM   =  "q";
                final String FORAMT_PARAM = "mode";
                final String UNTIT_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri buildUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID, apiKey)
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORAMT_PARAM,format)
                        .appendQueryParameter(UNTIT_PARAM,units)
                        .appendQueryParameter(DAYS_PARAM,Integer.toString( numDays))
                        .build();
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                Log.e(LOG_TAG, "doInBackground: URL Is "+ buildUri.toString() );
                URL url = new URL(buildUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();


                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                forecastJsonStr = null;
            } finally {
                if (urlConnection != null) {
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
                Log.d("RJ", "doInBackground: forecastJSNStr is "+forecastJsonStr);
                   if(forecastJsonStr!= null){
                       return getWeatherDataFromJSON(forecastJsonStr, numDays);
                   }

            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        return null;
        }

    }

/*
    public void onRefresh(View view){

        FetchWeatherFragment.FetchForecaseAsyncTask fw = new FetchWeatherFragment.FetchForecaseAsyncTask();
        fw.execute();
    }*/
}
