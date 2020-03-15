//package com.example.smartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    EditText ip_EditText;
    EditText port_EditText;
    Button connectBtn;

    ToggleButton onoff_Toggle;

    TextView showTemp_Textview;
    TextView showWind_Textview;

    Button temp_Up;
    Button temp_Down;
    Button wind_Up;
    //Button wind_Down;

    Handler msgHandler;

    Socket socket;
    SocketClient client;

    ReceiveThread receive;

    OnThread on;
    OffThread off;

    TempUpThread tempUp;
    TempDownThread tempDown;
    WindUpThread windUp;
    //WindDownThread windDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip_EditText=(EditText)findViewById(R.id.ip_EditText);
        port_EditText=(EditText)findViewById(R.id.port_EditText);
        connectBtn=(Button)findViewById(R.id.connectBtn);

        onoff_Toggle=(ToggleButton)findViewById(R.id.onoff_Toggle);

        showTemp_Textview=(TextView)findViewById(R.id.showTemp_TextView);
        showWind_Textview=(TextView)findViewById(R.id.showWind_TextView);

        temp_Up=(Button)findViewById(R.id.temp_Up);
        temp_Down=(Button)findViewById(R.id.temp_Down);
        wind_Up=(Button)findViewById(R.id.wind_Up);
        //wind_Down=(Button)findViewById(R.id.wind_Down);

        msgHandler=new Handler(){
            @Override
            public void handleMessage(Message hdmsg){
                String data;

                if(hdmsg.what==0000){
                    data=hdmsg.obj.toString();

                    String[] init=data.split(":");

                    if(init[0].equals("1")) {
                        onoff_Toggle.setChecked(true);

                        temp_Up.setEnabled(true);
                        temp_Down.setEnabled(true);
                        wind_Up.setEnabled(true);
                        //wind_Down.setEnabled(true);
                    }
                    else {
                        onoff_Toggle.setChecked(false);

                        temp_Up.setEnabled(false);
                        temp_Down.setEnabled(false);
                        wind_Up.setEnabled(false);
                        //wind_Down.setEnabled(false);
                    }
                    showTemp_Textview.setText(init[1]);
                    showWind_Textview.setText(init[2]);
                }

                if(hdmsg.what==1111){
                    data=hdmsg.obj.toString();
                    showTemp_Textview.setText(data);
                }
                else if(hdmsg.what==2222){
                    data=hdmsg.obj.toString();
                    showWind_Textview.setText(data);
                }
                else if(hdmsg.what==3333){
                    data=hdmsg.obj.toString();

                    if(data.equals("1")) {
                        onoff_Toggle.setChecked(true);

                        temp_Up.setEnabled(true);
                        temp_Down.setEnabled(true);
                        wind_Up.setEnabled(true);
                        //wind_Down.setEnabled(true);
                    }
                    else {
                        onoff_Toggle.setChecked(false);

                        temp_Up.setEnabled(false);
                        temp_Down.setEnabled(false);
                        wind_Up.setEnabled(false);
                        //wind_Down.setEnabled(false);
                    }
                }
                else if(hdmsg.what==8888){
                    connectBtn.setEnabled(false);
                    onoff_Toggle.setEnabled(true);
                }
            }
        };

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String get;

                get=port_EditText.getText().toString();
                get=get.trim();

                if(get.length()>0) {
                    //System.out.println("test");
                    client = new SocketClient(ip_EditText.getText().toString(),
                            port_EditText.getText().toString());
                    client.start();
                }
            }
        });

        onoff_Toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onoff_Toggle.isChecked()){
                    on=new OnThread(socket);
                    on.start();

                    temp_Up.setEnabled(true);
                    temp_Down.setEnabled(true);
                    wind_Up.setEnabled(true);
                    //wind_Down.setEnabled(true);
                }
                else{
                    off=new OffThread(socket);
                    off.start();

                    temp_Up.setEnabled(false);
                    temp_Down.setEnabled(false);
                    wind_Up.setEnabled(false);
                    //wind_Down.setEnabled(false);
                }
            }
        });

        temp_Up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tempUp=new TempUpThread(socket);
                tempUp.start();
            }
        });

        temp_Down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tempDown=new TempDownThread(socket);
                tempDown.start();
            }
        });

        wind_Up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windUp=new WindUpThread(socket);
                windUp.start();
            }
        });

        /*wind_Down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windDown=new WindDownThread(socket);
                windDown.start();
            }
        });*/
    }

    class SocketClient extends Thread{
        String ip;
        String port;

        public SocketClient(String ip, String port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            super.run();

            try {
                // 연결후 바로 ReceiveThread 시작
                socket = new Socket(ip, Integer.parseInt(port));

                if(socket.isConnected()) {
                    System.out.println("test");

                    Message hdmsg=msgHandler.obtainMessage();
                    hdmsg.what=8888;
                    msgHandler.sendMessage(hdmsg);

                    receive = new ReceiveThread(socket);
                    receive.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiveThread extends Thread {
        Socket sock;
        DataInputStream in=null;

        public ReceiveThread(Socket socket) {
            this.sock = socket;

            try{
                in=new DataInputStream(socket.getInputStream());
            }catch(Exception e){ }
        }

        // 메세지 수신후 Handler로 전달
        @Override
        public void run() {
            super.run();

            try {
                while(in!=null){
                    String data;
                    int count=in.available();
                    byte[] rcv=new byte[count];

                    in.read(rcv);

                    data=new String(rcv);
                    System.out.println(data);

                    Message hdmsg=msgHandler.obtainMessage();

                    String[] msg=data.split(":");

                    if(msg.length==3){
                        hdmsg.what=0000;
                        hdmsg.obj=msg[0]+":"+msg[1]+":"+msg[2];

                        msgHandler.sendMessage(hdmsg);
                    }

                    if(msg[0].equals("temp")){
                        hdmsg.what=1111;
                        hdmsg.obj=msg[1];

                        msgHandler.sendMessage(hdmsg);
                    }
                    else if(msg[0].equals("wind")){
                        hdmsg.what=2222;
                        hdmsg.obj=msg[1];

                        msgHandler.sendMessage(hdmsg);
                    }
                    else if(msg[0].equals("stat")){
                        hdmsg.what = 3333;
                        hdmsg.obj = msg[1];

                        msgHandler.sendMessage(hdmsg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class OnThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public OnThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("0000");
                out.flush();
            }
        }
    }

    class OffThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public OffThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("9999");
                out.flush();
            }
        }
    }

    class TempUpThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public TempUpThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("1111");
                out.flush();
            }
        }
    }

    class TempDownThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public TempDownThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("2222");
                out.flush();
            }
        }
    }

    class WindUpThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public WindUpThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("3333");
                out.flush();
            }
        }
    }

    /*class WindDownThread extends Thread{
        Socket socket;
        PrintWriter out=null;

        public WindDownThread(Socket socket){
            this.socket=socket;

            try{
                out=new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if(out!=null){
                out.println("4444");
                out.flush();
            }
        }
    }*/
}
