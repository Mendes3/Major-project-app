package com.example.test;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView infoIp;
    TextView infoMsg;
    TextView txtResult;
    EditText userId;
    TextView hiText;

    Button startButton;
    Button stopButton;
    Button distributeButton;
    String msgLog = "";
    StringBuffer response;

    //newly added code for fetching from database
    private String filesJSON;

    private static final String JSON_ARRAY ="result";
    private static final String FILE_URL = "url";

    private JSONArray arrayFiles= null;

    private final int TRACK = 0;

    private static final String FILES_URL = "http://192.168.1.112/database/getfiles.php";           //** insert the updated ip address of the localhost

    private Button buttonFetchFiles;
    TextView txtException;

    String url="http://192.168.1.112/database/update.php"; //__________this url will be different__________
    //declaration of global variables
    private int sec =0;
    private boolean isRunning;
    private boolean wasRunning;


    ServerSocket httpServerSocket;
    private final String CHANNEL_ID = "Server notification";
    private final int notificationId = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        status=(RadioGroup) findViewById(R.id.radioGroup1);

        infoIp = findViewById(R.id.infoIp);
        infoMsg = findViewById(R.id.msg);

        txtResult = findViewById(R.id.txtResult);
        startButton = findViewById(R.id.btnStart);
        startButton.setEnabled(false);
        stopButton = findViewById(R.id.btnStop);
        distributeButton = findViewById(R.id.btnDistribute);

        stopButton.setEnabled(false);
        userId = findViewById(R.id.deviceId);
        hiText = findViewById(R.id.title);

        txtException = findViewById(R.id.ExceptionMsg);
        buttonFetchFiles = findViewById(R.id.buttonFetchFiles);
        buttonFetchFiles.setOnClickListener(this);



        if (savedInstanceState != null){
            sec = savedInstanceState.getInt("seconds");
            isRunning = savedInstanceState.getBoolean("running");
            wasRunning = savedInstanceState.getBoolean("wasRunning");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Server Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final String dbipaddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
//                        new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.server)
                        .setContentTitle("Notification")
                        .setAutoCancel(true)
                        .setDefaults(sec);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);


//                writeFile();


                //fordatabase("Active","0",user);
                String user = userId.getText().toString();
                if (user.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter UserId First", Toast.LENGTH_LONG).show();
                    infoIp.setText("Enter UserId First");

                }
                else{
                    hiText.setText("Welcome, "+user);
                    readFile();

                    String ipaddress = (getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");
                    // infoIp.setText(ipaddress);// just for test purpose

                    infoIp.setText(dbipaddress+":"+HttpServerThread.HttpServerPORT);
                    infoMsg.setText("SERVER STARTED");

                    HttpServerThread httpServerThread = new HttpServerThread();
                    httpServerThread.start();
                    notificationManager.notify(notificationId, builder.build());
                    //infoIp.setText("SERVER STARTED");
                    startButton.setText("Server ON");
                    isRunning = true;
                    startTimer();
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                }

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManagerCompat.from(MainActivity.this).cancel(notificationId);
                if (httpServerSocket != null) {
                    try {
                        httpServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                infoIp.setText("");
                infoMsg.setText("");
                //userId.setText("");
                startButton.setText("Start");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);

                isRunning = false;
                String user = userId.getText().toString();

                String str_sec= String.valueOf(sec);
                fordatabase("InActive", str_sec,user,dbipaddress);
                sec =0;

            }
        });
        distributeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Intents are objects of the android.content.Intent type. Your code can send them
                // to the Android system defining the components you are targeting.
                // Intent to start an activity called SecondActivity with the following code:
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);

                //Sending the distribution content to the second activity.
//                intent.putExtra("Distribution_content", temp);


                // start the activity connect to the specified class
                startActivity(intent);
            }
        });

    }



    private void extractJSON(){
        try {
            JSONObject jsonObject = new JSONObject(filesJSON);
            arrayFiles = jsonObject.getJSONArray(JSON_ARRAY);
            System.out.println("Here here here");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showFile(){
        try {
            JSONObject jsonObject = arrayFiles.getJSONObject(TRACK);

            getFile(jsonObject.getString(FILE_URL));
            System.out.println("URL URL URL::::::");
            System.out.println(jsonObject.getString(FILE_URL));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getAllFiles() { // get all files URL
        @SuppressLint("StaticFieldLeak")
        class GetAllFiles extends AsyncTask<String,Void,String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Fetching Data","Please Wait...",true,true);
            }

            @Override
            protected void onPostExecute(String s) {

//
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();
                filesJSON = s;
                extractJSON();
                showFile();
            }

            @Override
            protected String doInBackground(String... params) {
                String uri = params[0];
                BufferedReader bufferedReader = null;
                try {
                    URL url = new URL(uri);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    String json;
                    while((json = bufferedReader.readLine())!= null){
                        sb.append(json).append("\n");
                    }

                    return sb.toString().trim();
                }catch(Exception e){
                    System.out.println(e.toString());
                    txtException.setText(e.toString());
                    return "From exception!!";

//                    return null;
                }
            }
        }
        GetAllFiles gai = new GetAllFiles();
        gai.execute(FILES_URL);
    }

    private void getFile(String urlToFile){  //take a string as a parameter. The string would have the url to image extracted from json array.
//        final String[] content_fromdb = {""};
        @SuppressLint("StaticFieldLeak")
        class GetFile extends AsyncTask<String,Void, String>{
            ProgressDialog loading;

            @Override
            protected String doInBackground(String... params) {
                URL url = null; // creating a url object
                StringBuilder content = new StringBuilder();
                String content_fromdb = null;


                String urlToFile = params[0];
                try {
                    url = new URL(urlToFile);
                    URLConnection urlConnection = url.openConnection(); // creating a url connection object

                    // wrapping the urlconnection in a bufferedreader
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    // reading from the urlconnection using the bufferedreader
                    while ((line = bufferedReader.readLine()) != null)
                    {
                        content.append(line).append("\n");
                    }
                    bufferedReader.close();

                    System.out.println("TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT");
                    content_fromdb = content.toString();
                    System.out.println(content_fromdb);
                    //txtException.setText(content_fromdb); // just for test purpose
                    txtException.setText("FIlE FETCHED");


                } catch (MalformedURLException e) { // this happens when the url is invalid
                    e.printStackTrace();
                    System.out.println("The URL is invalid");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("image image image image image image image image image image image image");
                System.out.println(content_fromdb);
                System.out.println("image image image image image image image image image image image image displayed displayed displayed");
                return content_fromdb;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this,"Downloading File...","Please wait...",true,true);

            }

            @Override
            protected void onPostExecute(String c) {
                super.onPostExecute(c);
                System.out.println("Downloading Downloading Downloading Downloading Downloading Downloading Downloading Downloading");
                System.out.println(c);
                startButton.setEnabled(true);
                writeFile(c);
                buttonFetchFiles.setEnabled(false);
                loading.dismiss();
            }
        }
        GetFile gi = new GetFile();
        gi.execute(urlToFile);
        System.out.println("+++++++++++++++++++++++++++++++++++++++");

    }
    private void fordatabase(final String state, String time, String user,String dbipaddress) {


//        int radioId = status.getCheckedRadioButtonId();
//        radiobutton = findViewById(radioId);
////        infoIp.setText(radiobutton.getText());
//        final String state= (String) radiobutton.getText();
        // WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo info = wifiManager.getConnectionInfo();
//        String address = info.getMacAddress();
//        System.out.println("MA mac hu");
//        System.out.println(address);
//        System.out.println("MA mac hu");

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);


        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Toast.makeText(MainActivity.this, "Insertion Is:" + response, Toast.LENGTH_SHORT).show(); //just fortest purpose
                Log.d("response",response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(MainActivity.this, "my error :" + error, Toast.LENGTH_LONG).show();
                Log.i("My error", "" + error);
                System.out.println(error);

            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> map = new HashMap<String, String>();
//                map.put("username",sname);
                map.put("Status",state);

                map.put("Ipaddress",dbipaddress);
                map.put("Offered_Time",time);
                map.put("userid",user);

                System.out.println("INSERTED INSERTED______+++++++++++");
                return map;
            }
        };
        queue.add(request);


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("seconds", sec);
        savedInstanceState.putBoolean("running", isRunning);
        savedInstanceState.putBoolean("wasRunning", wasRunning);
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        wasRunning = isRunning;
        isRunning = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (wasRunning) {
            isRunning = true;
        }
    }


    private void startTimer()
    {
//        final String[] time_value = {""};
        final String[] time_value = {""};
        final TextView timer = findViewById(R.id.timer);

        final Handler hd = new Handler();

        hd.post(new Runnable() {

            @Override

            public void run()
            {
                int hours_var = sec / 3600;
                int minutes_var = (sec % 3600) / 60;
                int secs_var = sec % 60;

                time_value[0] = String.format(Locale.getDefault(),
                        "%d:%02d:%02d", hours_var, minutes_var, secs_var);

                timer.setText(time_value[0]);

                if (isRunning)
                {
                    sec++;
                }

                hd.postDelayed(this, 1000);

            }

        });
        System.out.println("++++++++++++++++");
        System.out.println(time_value[0]);
        System.out.println("++++++++++++++++");
//        return time_value[0];
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    @Override
    public void onClick(View v) {
        if(v == buttonFetchFiles) {
            getAllFiles();
        }
    }

    private class HttpServerThread extends Thread {

        static final int HttpServerPORT = 8888;

        @Override
        public void run() {
            Socket socket = null;

            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);

                while(true){
                    socket = httpServerSocket.accept();

                    HttpResponseThread httpResponseThread = new HttpResponseThread(socket);
                    httpResponseThread.start();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class HttpResponseThread extends Thread {

        Socket socket;

        HttpResponseThread(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            BufferedReader is;
            PrintWriter os;
            String request;

            try {
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = is.readLine();

                os = new PrintWriter(socket.getOutputStream(), true);


//                //reading file from assests folder
//                try {
//                    InputStream inst = getAssets().open("index.html");
//                    int size = inst.available();
//                    byte[] buffer = new byte[size];
//                    inst.read(buffer);
//                    inst.close();
//                    response = new String(buffer);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }



                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/html" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
                socket.close();

                msgLog += "Request of " + request
                        + " from " + socket.getInetAddress().toString() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        //infoMsg.setText(msgLog);//just for test purpose
                        infoMsg.setText("SERVER IS HOSTING THE FILE");

                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public void writeFile(String temp) {

        //First reading from the assests folder--later reading would be from database.
//        try {
//            InputStream inst = getAssets().open("index.html");
//            int size = inst.available();
//            byte[] buffer = new byte[size];
//            inst.read(buffer);
//            inst.close();
//            temp = new String(buffer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.print(temp);


        //Storing the file from assest  into app-specific storage folder.
        try {
            FileOutputStream fileOutputStream = openFileOutput("index.html", MODE_PRIVATE);
            fileOutputStream.write(temp.getBytes());
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readFile() {

        //Reading from the app-specific internal storage for hosting.
        try {
            FileInputStream fileInputStream = openFileInput("index.html");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            String lines;
            while ((lines = bufferedReader.readLine()) != null) {
                stringBuffer.append(lines + "\n");
            }
            response = stringBuffer;
//            txtResult.setText(response);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
