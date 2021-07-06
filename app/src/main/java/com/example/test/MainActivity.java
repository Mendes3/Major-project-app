package com.example.test;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    TextView infoIp;
    TextView infoMsg;
    TextView txtResult;

    Button startButton;
    Button stopButton;
    String msgLog = "";

    StringBuffer response;


    ServerSocket httpServerSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoIp = (TextView) findViewById(R.id.infoIp);
        infoMsg = (TextView) findViewById(R.id.msg);

        infoIp.setText(getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");

        txtResult = (TextView) findViewById(R.id.txtResult);
        startButton = (Button)findViewById(R.id.btnStart);
        stopButton = (Button)findViewById(R.id.btnStop);

        writeFile();
        readFile();

        HttpServerThread httpServerThread = new HttpServerThread();
        httpServerThread.start();

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

        String temp="";
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
            txtResult.setText(response);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
