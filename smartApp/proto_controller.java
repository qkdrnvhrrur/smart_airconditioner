import java.io.*;
import java.net.*;

class proto_controller{
    public static void main(String[] args){
        Socket socket=null;

        try{
            socket=new Socket("127.0.0.1", 8090);
            
            BufferedReader in=new BufferedReader
                                        (new InputStreamReader(socket.getInputStream()));
            PrintWriter out=new PrintWriter(socket.getOutputStream());
            BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));

            while(true){
                String snd=null;
                snd=reader.readLine();

                if(snd.equals("quit"))
                    break;

                out.println(snd);
                out.flush();

                String rcv=null;
                rcv=in.readLine();

                System.out.println(rcv);
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        finally{
            try{
                socket.close();
            }
            catch(Exception e){
            }
        }
    }
}
