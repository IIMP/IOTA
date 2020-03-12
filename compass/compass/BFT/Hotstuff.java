package org.iota.compass.BFT;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.util.*;
import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.io.IOException;
public class Hotstuff{
	/*
	Socket remoteSocket;
	BufferedWriter sendBufferedWriter;
	public Hotstuff(String host, int port) throws Exception{
		remoteSocket = new Socket(host,port);
		sendBufferedWriter = new BufferedWriter(new OutputStreamWriter(remoteSocket.getOutputStream()));
	}
	public void sendDataTest() throws Exception{
		String json1="{'data':25}";
		sendBufferedWriter.write(json1);
	}*/
	int idx_now;
	ServerSocket serverSocket;
	Map<String, Integer> cmd_map;
	public Hotstuff(){
		cmd_map = new HashMap<String, Integer>();
		idx_now = 0;
	}
	public int getIdx(){
		idx_now += 1;
		return idx_now;
	}
	public boolean call_send(String host, int port, String data, int idx){
		char a,b;
		b = (char)(idx%256);
		a = (char)(idx/256);
		cmd_map.put(data, idx);
		try {
			
			InetSocketAddress addr = new InetSocketAddress(host,port);
			Socket remoteSocket = new Socket();
			remoteSocket.connect(addr);
			BufferedWriter sendBufferedWriter = new BufferedWriter(new OutputStreamWriter(remoteSocket.getOutputStream()));
			String json1 = String.valueOf(a) + String.valueOf(b) + data;//"1112345678901234567890123456789011";
			System.out.println("json1: "+json1);
			sendBufferedWriter.write(json1);
			sendBufferedWriter.flush();
			remoteSocket.close();
			return true;
			
		}catch (IOException e) {
			e.printStackTrace();
			return false;
        }
	}
	public boolean setPort(int port){
		try { 
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(3000);
			return true;
		}catch (Exception e) {  
	        System.out.println("error: " + e.getMessage());
	        return false;
        } 
	}
	public byte[] listen_on(){
		try {
            Socket client = serverSocket.accept();
            try { 
                /*BufferedReader br=new BufferedReader(new InputStreamReader(client.getInputStream()));
                String clientInputStr = "";
                String tmp;
                while(!((tmp = br.readLine())==null)){
            		clientInputStr = clientInputStr + tmp;
        		}*/
        		InputStream is = client.getInputStream();
        					
    			byte[] datas = new byte[500];
    			int count = is.read(datas);
    			System.out.println("receive size:" + String.valueOf(count));
    			if(count == 1){
    				return new byte[2];
    			}  		
    			int[] dataFormat = new int[count];
    			for(int i=0;i<count;i++){
    				if(datas[i]<0){
    					dataFormat[i]=datas[i]&0xff;
    				}else{
    					dataFormat[i]=datas[i];
    				}
    			}
                
                client.close();
                return datas;
            }catch (Exception e) {  
                System.out.println("run error: " + e.getMessage());
                client.close();
                return new byte[1];
            } finally {  
                if (client != null) {  
                    try {  
                        client.close();  
                    } catch (Exception e) {  
                        client = null;  
                        System.out.println("finally error:" + e.getMessage());  
                    }  
                }
                client.close();
            }
        }catch (Exception e) {  
            System.out.println("error: " + e.getMessage());
            return new byte[1];
        } 
	}
	private class HandlerThread implements Runnable {  
        private Socket socket;  
        public HandlerThread(Socket client) {  
            socket = client;  
            new Thread(this).start();  
        }  
  
        public void run() {  
           
        }  
    }  
	/*public static void main(String[] args) {
		try {
			for(int i = 0; i < 4; i++){
				InetSocketAddress addr = new InetSocketAddress("127.0.0.1",10060+i);
				Socket remoteSocket = new Socket();
				remoteSocket.connect(addr);
				BufferedWriter sendBufferedWriter = new BufferedWriter(new OutputStreamWriter(remoteSocket.getOutputStream()));
				String json1="1112345678901234567890123456789011";
				sendBufferedWriter.write(json1);
				sendBufferedWriter.flush();
				remoteSocket.close();
			}
			
		}catch (IOException e) {
			e.printStackTrace();
        }
	}*/
}