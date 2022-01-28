package com.example.test;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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

    Button startButton;
    Button stopButton;
    String msgLog = "";

    EditText Status,Ipaddress;
    RadioGroup status;
    RadioButton radiobutton;
    StringBuffer response;
    String temp;

//newly added code for fetching from database
private String filesJSON;

    private static final String JSON_ARRAY ="result";
    private static final String FILE_URL = "url";

    private JSONArray arrayFiles= null;

    private int TRACK = 0;

    private static final String FILES_URL = "http://192.168.1.112/database/getfiles.php";           //** insert the updated ip address of the localhost

    private Button buttonFetchFiles;
    TextView txtException;

String url="http://192.168.1.112/database/insert.php"; //__________this url will be different__________
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

        txtResult = (TextView) findViewById(R.id.txtResult);
        startButton = (Button)findViewById(R.id.btnStart);
        stopButton = (Button)findViewById(R.id.btnStop);
        stopButton.setEnabled(false);

        txtException = (TextView) findViewById(R.id.ExceptionMsg);
        buttonFetchFiles = (Button) findViewById(R.id.buttonFetchFiles);
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
//                        getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, builder.build());




                writeFile();
                readFile();

                String ipaddress = (getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");
                infoIp.setText(ipaddress);

                HttpServerThread httpServerThread = new HttpServerThread();
                httpServerThread.start();

                startButton.setText("Server ON");
                isRunning = true;
                startTimer();
                fordatabase("Active","0");

                startButton.setEnabled(false);
                stopButton.setEnabled(true);

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManagerCompat.from(MainActivity.this).cancel(notificationId);
//                fordatabase("InActive");
                if (httpServerSocket != null) {
                    try {
                        httpServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                infoIp.setText("");
                infoMsg.setText("");
                startButton.setText("Start");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                isRunning = false;
                String str_sec= String.valueOf(sec);
                fordatabase("InActive", str_sec);
                sec =0;

            }
        });
    }
    private void extractJSON(){
        try {
            JSONObject jsonObject = new JSONObject(filesJSON);
            arrayFiles = jsonObject.getJSONArray(JSON_ARRAY);
            System.out.println("Here here here");
        } catch (JSONException e) {
//            System.out.println("tHere there there there there there there there there there");
            e.printStackTrace();
        }
    }

    private void showImage(){
        try {
            JSONObject jsonObject = arrayFiles.getJSONObject(TRACK);

            getImage(jsonObject.getString(FILE_URL));
            System.out.println("URL URL URL::::::");
            System.out.println(jsonObject.getString(FILE_URL));
            System.out.println("Show show show show Show show show show Show show show show Show show show show Show show show show");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getAllImages() {
        @SuppressLint("StaticFieldLeak")
        class GetAllImages extends AsyncTask<String,Void,String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Fetching Data","Please Wait...",true,true);
            }

            @Override
            protected void onPostExecute(String s) {

//                super.onPostExecute(s);
//                loading.dismiss();                    // this is used to cancel the loading icon, OK
//                System.out.println("TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST");

////                Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();
                filesJSON = s;
                extractJSON();
                showImage();
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
        GetAllImages gai = new GetAllImages();
        gai.execute(FILES_URL);
    }

    private void getImage(String urlToImage){  //take a string as a parameter. The string would have the url to image extracted from json array.
        @SuppressLint("StaticFieldLeak")
        class GetImage extends AsyncTask<String,Void, Bitmap>{
            ProgressDialog loading;
            @Override
            protected Bitmap doInBackground(String... params) {
                URL url = null; // creating a url object
                Bitmap image = null; // bitmap all images that is shown in the  android app
                StringBuilder content = new StringBuilder();

                String urlToImage = params[0];
                try {
                    url = new URL(urlToImage);
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


                    //image = BitmapFactory.decodeStream(url.openConnection().getInputStream()); // this is for image
                    //image = url.openConnection().getInputStream()); // for text

                    System.out.println("TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT TEXT");

                    System.out.println(content.toString());
                    txtException.setText(content.toString());

                } catch (MalformedURLException e) { // this happens when the url is invalid
                    e.printStackTrace();
                    System.out.println("The URL is invalid");
                } catch (IOException e) {
                    e.printStackTrace();
                }



//                 return "Image";
                System.out.println("image image image image image image image image image image image image");
                System.out.println(image);
                System.out.println("image image image image image image image image image image image image displayed displayed displayed");
                return image;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this,"Downloading Image...","Please wait...",true,true);
                System.out.println("Downloading Downloading Downloading Downloading Downloading Downloading Downloading Downloading");
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                loading.dismiss();
                System.out.println("Now now now now now");
            }
        }
        GetImage gi = new GetImage();
        gi.execute(urlToImage);
    }
    private void fordatabase(final String state, String time) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final String dbipaddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());

//        int radioId = status.getCheckedRadioButtonId();
//        radiobutton = findViewById(radioId);
////        infoIp.setText(radiobutton.getText());
//        final String state= (String) radiobutton.getText();

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);


        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Toast.makeText(MainActivity.this, "Insertion Is:" + response, Toast.LENGTH_SHORT).show();
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
        final TextView timer = (TextView)findViewById(R.id.timer);

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
            getAllImages();
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

                        infoMsg.setText(msgLog);
                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public void writeFile() {

        //First reading from the assests folder--later reading would be from database.
        try {
            InputStream inst = getAssets().open("index.html");
            int size = inst.available();
            byte[] buffer = new byte[size];
            inst.read(buffer);
            inst.close();
            temp = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
