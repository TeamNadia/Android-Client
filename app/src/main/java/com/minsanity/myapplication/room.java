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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class room extends AppCompatActivity implements View.OnClickListener{

    int selectedIndex = 0;
    Button btnJoin;
    EditText txtRoomID;
    ListView listRooms;
    public static String URL_PINS = "http://projectnadia.windowshelpdesk.co.uk/Server/checkpin.php";
    public static String URL_SCREENS = "http://projectnadia.windowshelpdesk.co.uk/Server/getscreens.php";
    ArrayList<String> rooms;
    RequestQueue queue;
    StringRequest request;
    ArrayAdapter<String> adapter;
    ProgressDialog loading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        listRooms = (ListView) findViewById(R.id.listRooms);
        txtRoomID = (EditText) findViewById(R.id.txtRoomID);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(this);

        queue = Volley.newRequestQueue(this);

        rooms = new ArrayList<String>();

        listRooms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                selectedIndex = position;
            }
        });
        getListOfRooms();
    }

    private void populateList(){
        String[] myItems = new String[rooms.size()];
        for(int i = 0; i < rooms.size(); i++){
            myItems[i] = rooms.get(i).split(":")[1];
        }
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                myItems
        );

        listRooms.setAdapter(adapter);
    }

    private void getListOfRooms(){
        loading = ProgressDialog.show(this, "Please wait...", "Loading...", false, false);
        request = new StringRequest(URL_SCREENS, new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
                loading.dismiss();
                storeResponse(response);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                error.printStackTrace();
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void storeResponse(String response){
        try {
            JSONArray jsonArr = new JSONArray(response);
            for(int i = 0; i < jsonArr.length(); i++){
                String id = jsonArr.getJSONObject(i).getString("id");
                String roomName = jsonArr.getJSONObject(i).getString("location") + " - " + jsonArr.getJSONObject(i).getString("name");
                rooms.add(id + ":" + roomName);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        populateList();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btnJoin:
                loading = ProgressDialog.show(this, "Please wait...", "Loading...", false, false);
                final String pin = txtRoomID.getText().toString();
                final String screenID = rooms.get(selectedIndex).split(":")[0];

                request = new StringRequest(URL_PINS + ("?screen=" + screenID + "&pin=" + pin), new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        if (response.trim().equals("SUCCESS")) {
                            Toast.makeText(getApplicationContext(), "Success! Connected to room: " + rooms.get(selectedIndex), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("screenID", screenID);
                            intent.putExtra("pin", pin);
                            startActivity(intent);
                        }else{
                            Toast.makeText(getApplicationContext(), "Error! Wrong pin code.", Toast.LENGTH_SHORT).show();
                        }
                        loading.dismiss();
                    }
                }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        error.printStackTrace();
                    }
                });
                queue.add(request);
                break;
            case R.id.btnRefresh:
                loading = ProgressDialog.show(this, "Please wait...", "Refreshing list...", false, false);
                rooms.clear();
                getListOfRooms();
                adapter.notifyDataSetChanged();
                loading.dismiss();
                break;
        }
    }

}
