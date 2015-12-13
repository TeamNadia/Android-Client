package com.minsanity.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static String URL = "http://projectnadia.windowshelpdesk.co.uk/Server/getsongs.php";
    public static String URL_SUBMIT = "http://projectnadia.windowshelpdesk.co.uk/Server/submitsong.php";
    public static String URL_VOTE = "http://projectnadia.windowshelpdesk.co.uk/Server/vote.php";

    String screenID;
    String pin;
    final ArrayList<String> queue = new ArrayList<String>();
    RequestQueue requestQueue;
    StringRequest stringRequest;
    ArrayAdapter<String> adapter;
    Button btnSubmit;
    Button btnExit;
    Button btnReload;
    EditText txtVoteArtist;
    EditText txtVoteTrack;
    ListView listQueue;
    ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReload = (Button) findViewById(R.id.btnReload);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnExit = (Button) findViewById(R.id.btnExit);
        txtVoteTrack = (EditText) findViewById(R.id.txtVoteTrack);
        txtVoteArtist = (EditText) findViewById(R.id.txtVoteArtist);
        listQueue = (ListView) findViewById(R.id.listQueue);

        btnReload.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
        btnExit.setOnClickListener(this);

        requestQueue = Volley.newRequestQueue(this);

        //retrieves the screenID from the previous activity (room.java)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            screenID = extras.getString("screenID");
            pin = extras.getString("pin");
        }else{
            screenID = "1";
            pin = "1";
        }

        getMusicQueue();

        registerClickCallback();
    }

    private void refreshQueue(){
        queue.clear();
        getMusicQueue();
        adapter.notifyDataSetChanged();
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.btnSubmit:
                loading = ProgressDialog.show(this, "Please wait...", "Requesting track...", false, false);
                final String artist = txtVoteArtist.getText().toString();
                final String track = txtVoteTrack.getText().toString();
                stringRequest = new StringRequest(URL_SUBMIT + "?screen=" + screenID + "&pin=" + pin + "&artist=" + artist + "&track=" + track, new Response.Listener<String>(){
                    public void onResponse(String response) {
                        if (response.trim().equals("SUCCESS")) {
                            Toast.makeText(getApplicationContext(), "Success! Track: " + track + " by " + artist + " has been requested.", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Error! Could not request the track.", Toast.LENGTH_SHORT).show();
                        }
                        txtVoteArtist.setText("");
                        txtVoteTrack.setText("");
                        refreshQueue();
                        loading.dismiss();
                    }
                }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        error.printStackTrace();
                    }
                });
                requestQueue.add(stringRequest);
                break;
            case R.id.btnExit:
                requestQueue.stop();
                stringRequest.cancel();
                startActivity(new Intent(getApplicationContext(), room.class));
                break;
            case R.id.btnReload:
                loading = ProgressDialog.show(this, "Please wait...", "Requesting track...", false, false);
                refreshQueue();

                loading.dismiss();
                break;
        }
    }

    private void getMusicQueue(){
        stringRequest = new StringRequest(URL + ("?screen=" + screenID), new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
               storeResponse(response);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                error.printStackTrace();
            }
        });
        requestQueue.add(stringRequest);
    }

    private void storeResponse(String response){
        //queue is empty
        if(response.trim().equals("SCREENFAIL")){
            return;
        }
        try {
            JSONArray jsonArr = new JSONArray(response);
            for(int i = 0; i < jsonArr.length(); i++){
                String id = jsonArr.getJSONObject(i).getString("id");
                String artist = jsonArr.getJSONObject(i).getString("artist");
                String track = jsonArr.getJSONObject(i).getString("track");
                String votes = jsonArr.getJSONObject(i).getString("votes");
                queue.add(id + ":" + artist + " - " + track + "   [Votes = " + votes + "]");
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        populateList();
    }

    private void populateList(){
        String[] myItems = new String[queue.size()];
        for(int i = 0; i < queue.size(); i++){
            myItems[i] = queue.get(i).split(":")[1];
        }
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                myItems
        );

        listQueue.setAdapter(adapter);
    }

    private void registerClickCallback() {
        listQueue.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                loading = ProgressDialog.show(parent.getContext(), "Please wait...", "Loading...", false, false);

                final int trackIndex = position;
                String trackID = queue.get(trackIndex).split(":")[0];
                stringRequest = new StringRequest(URL_VOTE + "?screen=" + screenID + "&pin=" + pin + "&id=" + trackID + "&vote=1", new Response.Listener<String>(){
                    public void onResponse(String response) {
                        if (response.trim().equals("SUCCESS")) {
                            Toast.makeText(getApplicationContext(), "Success! " + queue.get(trackIndex).split(":")[1] + " has been up-voted.", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Error! Could not up-vote the track.", Toast.LENGTH_SHORT).show();
                        }
                        refreshQueue();
                        loading.dismiss();
                    }
                }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        error.printStackTrace();
                    }
                });
                requestQueue.add(stringRequest);
            }
        });
    }
}
