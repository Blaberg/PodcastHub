package se.umu.cs.jobl0099.podcasthub;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    //Variables to connect PodcastHub with spotify api
    private static final String CLIENT_ID = "5912d7a08e6f42ffa48d576aea7e9d8e";
    private static final String REDIRECT_URI = "podcasthub://callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;
    private static String TOKEN = null;

    //Variables for Volly REST Requests
    private TextView mTextViewResult;
    private RequestQueue mQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewResult = findViewById(R.id.textView);
        mQueue = Volley.newRequestQueue(this);

        //Create Play/Pause button
        final Button button = findViewById(R.id.button);
        //Get the PlayerState
        button.setOnClickListener(v -> mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
            //If the player is not paused: Pause the player if button ir pressed
            if (!playerState.isPaused) {
                mSpotifyAppRemote.getPlayerApi().pause();
                Log.d("MainActivity", "Song Paused");
                button.setText("Play");
            }//If the player is paused: Resume the player if button is pressed
            else if (playerState.isPaused) {
                mSpotifyAppRemote.getPlayerApi().resume();
                Log.d("MainActivity", "Song Resumed");
                button.setText("Pause");
            }

        }));

        final Button requestBtn = findViewById(R.id.requestBtn);

        requestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jsonParse();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Create connection params
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        //Connect the app to the spotify API when the app starts
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    //If the connection was successful set the spotifyAppRemote as a global variable and start the connected function
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");
                        authenticate();
                        // Now you can start interacting with App Remote
                        connected();
                    }

                    //if the connection failed handle errors
                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });

        // We will start writing our code here.
    }

    private void connected() {
        //Play hardcoded song
        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");

        //subscribe to the playerstate. Print the song that is currently playing.
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        Log.d("MainActivity", track.name + " by " + track.artist.name);
                    }
                });
        // Then we will write some more code here.
    }

    @Override
    protected void onStop() {
        //when the app is stopped, disconnect from spotify.
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        // Aaand we will finish off here.
    }

    private void jsonParse() {
        String url = "https://api.spotify.com/v1/tracks/11dFghVXANMlKmJXsNCbNl";
        Map<String, String> params = new HashMap<String, String>();


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    //JSONObject jsonObject = response.getJSONObject("name");
                    mTextViewResult.setText(response.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer " + TOKEN);
                return params;
            }
        };

        mQueue.add(request);
    }

    private void authenticate() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    this.TOKEN = response.getAccessToken();
                    Log.d("OAuth", TOKEN);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d("Authentication", response.getError().toString());
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }
}