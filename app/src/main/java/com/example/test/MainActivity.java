package com.example.test;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
    String temp;


    ServerSocket httpServerSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoIp = findViewById(R.id.infoIp);
        infoMsg = findViewById(R.id.msg);

        txtResult = (TextView) findViewById(R.id.txtResult);
        startButton = (Button)findViewById(R.id.btnStart);
        stopButton = (Button)findViewById(R.id.btnStop);
        stopButton.setEnabled(false);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeFile();
                readFile();

                String ipaddress = (getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");
                infoIp.setText(ipaddress);

                HttpServerThread httpServerThread = new HttpServerThread();
                httpServerThread.start();

                startButton.setText("Server ON");
                startButton.setEnabled(false);
                stopButton.setEnabled(true);

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
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
