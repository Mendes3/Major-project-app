package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {

//    EditText e1;
    TextView disMsg,e1;
    Button back_btn;


    String distributed_msg;

    public static String MY_IP = "";
    String fetchurl="http://192.168.1.112/database/fetchip.php"; //__________this url will be different__________


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        e1 = findViewById(R.id.txtIp);
//        myIp = findViewById(R.id.myIp);
        disMsg = findViewById(R.id.disMsg);
        back_btn = findViewById(R.id.back_button);

        Thread myThread = new Thread(new MyServer());
        myThread.start();

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Intents are objects of the android.content.Intent type. Your code can send them
                // to the Android system defining the components you are targeting.
                // Intent to start an activity called SecondActivity with the following code:
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);

                // start the activity connect to the specified class
                startActivity(intent);
                finish();
//                myThread.interrupt();
            }
        });


//        try {
//            MY_IP = getLocalIpAddress2();
//            myIp.setText("My IP: " +MY_IP);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }



//    //Receiving from the main activity the file to be distributed.
//        Intent intent = getIntent();
//
//        try {
//            distributed_msg = intent.getStringExtra("Distribution_content");
//        }catch(Exception e){
//            System.out.println(e.toString());
//        }

    }

    private String getLocalIpAddress2() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }


    //    Receiving message from peers
    class MyServer implements Runnable{

        ServerSocket ss;
        Socket mysocket;
        DataInputStream dis;
        String message;
        Handler handler = new Handler();

        @Override
        public void run() {

            try {
                ss = new ServerSocket(9700);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Waiting for peers", Toast.LENGTH_SHORT).show();
                    }
                });
                while (true)
                {
                    mysocket = ss.accept();
                    dis = new DataInputStream(mysocket.getInputStream());
                    //This message stores the information from the peer -- to be stored in app-specific storage.
                    message = dis.readUTF();

                    //storing message into app-specific storage.
                    try {
                        FileOutputStream fileOutputStream = openFileOutput("index.html", MODE_PRIVATE);
                        fileOutputStream.write(message.getBytes());
                        fileOutputStream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //The received is displayed for testing purpose
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            disMsg.setText(message);

                        }
                    });

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public void button_click(View v){


        //For secondary distribution app reading file should done after send button is clicked.
         read();
         fetchingfromdb();
        System.out.println("distributed messgae------" + distributed_msg);
    }


    //Sending Data to peers --- the peers ip addresses would be fetched from the database
    private class BackgroundTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dos;
        String ip, message, error;

        @Override
        protected String doInBackground(String... params) {
            ip = params[0];
            message = params[1];
            try {
                s =new Socket(ip, 9700);
                dos = new DataOutputStream(s.getOutputStream());
                dos.writeUTF(message); //distributed_msg is data to be shared by the primary distributor.
                dos.close();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
                error = e.toString();
            }
            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            disMsg.setText("Error: " + error +"Message is : "+message);
//            this.cancel(true);
//            if(isCancelled()){
//                break;
//            }
        }


    }
    private void fetchingfromdb() { // get all files URL
        class fetchingfromdb extends AsyncTask<String,Void,String> {
            ProgressDialog loading;
            BackgroundTask b = new BackgroundTask();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity2.this, "Fetching IP","Please Wait...",true,true);
            }

            @Override
            protected void onPostExecute(String s) {

                super.onPostExecute(s);
                e1.setText(s);
//                activeip = s;
                b.execute(s, distributed_msg);
                loading.dismiss();
                Toast.makeText(MainActivity2.this,s,Toast.LENGTH_LONG).show();

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
                        System.out.println(sb.toString().trim());

                    return sb.toString().trim();
                }catch(Exception e){
                    System.out.println(e.toString());
                    return "From exception!!";

//                    return null;
                }
            }
        }
        fetchingfromdb gai = new fetchingfromdb();
        gai.execute(fetchurl);
    }


    public void read(){
        try {
            FileInputStream fileInputStream = openFileInput("index.html");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            String lines;
            while ((lines = bufferedReader.readLine()) != null) {
                stringBuffer.append(lines + "\n");
            }
            StringBuffer response = stringBuffer;
            distributed_msg = response.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}