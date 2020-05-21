package com.wyh2004.proxy;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author WYH2004
 */
public class Main {

    private static List<String> whiteList;

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        String path = new File("").getAbsolutePath();

        FileAppender appender = (FileAppender) org.apache.log4j.Logger.getRootLogger().getAppender("logFile");
        appender.setFile(path + File.separator + "latest.log");
    }

    private static void scanFile(String path) throws IOException {
        List<String> list = new ArrayList<String>();
        FileInputStream fis = new FileInputStream(path);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        while ((line = br.readLine()) != null) {
            list.add(line);
        }
        whiteList = new ArrayList<>(list);
        br.close();
        isr.close();
        fis.close();
    }

    public static void exists() {

        File file = new File(".\\whitelist.txt");
        File logDir = new File(".\\logs");
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e){
                logger.error("无法生成whitelist.txt",e);
            }
        }
        if (!logDir.exists()) {
            logDir.mkdir();
        }
    }

    public static void main(String[] args) throws IOException {
        Main.exists();
        logger.info("感谢您使用 TCP-Proxy!");
        logger.info("原作者: miny1233");
        logger.info("修改版作者: WYH2004");
        logger.info("技术援助: Hello_Han");
        Scanner input = new Scanner(System.in);
        //默认IP以及端口号
        String default_Port = "25565";
        String default_ToIP = "r53.hypixel.net";
        String default_ToPort = "25565";

        //自定义
        logger.info("是否开启IP白名单系统:(1: 开启 0:关闭)");
        logger.info("不输入则默认关闭IP白名单系统");
        String W = input.nextLine();
        if(W == null || W.length() == 0) {
            W = "0";
        }

        logger.info("本地端口(25565):");
        String input_Port = input.nextLine();
        if(input_Port == null || input_Port.length() == 0) {
            input_Port = default_Port;
        }

        logger.info("目标IP(mc.hypixel.net):");
        String input_ToIP = input.nextLine();
        if(input_ToIP == null || input_ToIP.length() == 0) {
            input_ToIP = default_ToIP;
        }

        logger.info("目标端口(25565):");
        String input_ToPort = input.nextLine();
        if(input_ToPort == null || input_ToPort.length() == 0) {
            input_ToPort = default_ToPort;
        }

        logger.info("-------------------------");
        logger.info("当前设置数值:");
        logger.info("本地端口: " + input_Port);
        logger.info("目标IP: " + input_ToIP);
        logger.info("目标端口: " + input_Port);

        int WhiteList = Integer.parseInt(W);
        int Port = Integer.parseInt(input_Port);
        String ToIP = input_ToIP;
        int ToPort = Integer.parseInt(input_ToPort);

        //SRV记录
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        hashtable.put("java.naming.provider.url", "dns:");
        try {
            Attribute qwqre = (new InitialDirContext(hashtable)).getAttributes("_Minecraft._tcp." + ToIP, new String[]{"SRV"}).get("srv");
            if (qwqre != null) {
                String[] re = qwqre.get().toString().split(" ", 4);
                logger.info("检测到SRV记录，自动跳转到SRV记录");
                ToIP = re[3];
                logger.info("目标IP: " + ToIP);
                ToPort = Integer.parseInt(re[2]);
                logger.info("目标端口: " + ToPort);
            }
        } catch (Exception ignored) {}

        //TCP代理
        logger.info("TCP代理已启动!");
        ServerSocket serverSocket = new ServerSocket(Port);
        if (WhiteList == 1){
            logger.info("IP白名单状态: True");
            while (true) {
                Socket conn = serverSocket.accept();
                try {
                    scanFile("whitelist.txt");
                    if(whiteList.contains(conn.getRemoteSocketAddress().toString().replace("/", "").split(":")[0])){
                        logger.info(conn.getRemoteSocketAddress().toString() + " 已连接");
                        ConnectTo connectTo = new ConnectTo(ToIP, ToPort, conn);
                        Thread thread = new Thread(connectTo);
                        thread.start();
                    }else{
                        logger.warn(conn.getRemoteSocketAddress().toString() + " 想要连接, 但是已被拒绝！因为这个IP不在白名单内!");
                    }
                }catch (java.io.FileNotFoundException e){
                    logger.error("根目录下无法找到whitelist.txt", new RuntimeException("not find whitelist.txt"));
                }
            }
        } else {
            logger.info("IP白名单状态: False");
            while (true){
                Socket conn = serverSocket.accept();
                logger.info(conn.getRemoteSocketAddress().toString() + " 已连接");
                ConnectTo connectTo = new ConnectTo(ToIP, ToPort, conn);
                Thread thread = new Thread(connectTo);
                thread.start();
            }
        }
    }
}

class ReMessage implements Runnable {
    InputStream inputStream;
    Socket socket,server;
    OutputStream outputStream;
    byte[] bytes = new byte[1024*1024*8];
    String RemoteIP;

    @Override
    public void run() {
        int len;
        while (true){
            try{
                len = inputStream.read(bytes);
                outputStream.write(bytes,0,len);
            }
            catch (Exception error){
                try {
                    socket.close();
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println();
                System.gc();
                return;
            }
        }
    }

    public ReMessage(Socket connection,Socket S,String ReIP)  {
        socket = connection;
        server = S;
        RemoteIP = ReIP;
        try {
            outputStream = server.getOutputStream();
            inputStream = socket.getInputStream();
        }
        catch (Exception error){
            System.out.print(error.toString());
            System.gc();
        }
    }
}

class ConnectTo implements Runnable{
    String ToIP;
    int ToPort;
    Socket connection;

    public static Logger logger = LoggerFactory.getLogger(ConnectTo.class);

    @Override
    public void run() {

        try {
            Socket socket = new Socket(ToIP, ToPort);
            ReMessage re = new ReMessage(connection, socket,connection.getRemoteSocketAddress().toString());
            ReMessage send = new ReMessage(socket, connection,"");
            Thread r = new Thread(re);
            Thread s = new Thread(send);
            r.start();
            s.start();
        }
        catch (Exception error){
            logger.warn(connection.getRemoteSocketAddress().toString() +"异常断开");
            try {
                connection.close();
            }
            catch (Exception e){}
            System.gc();
        }
    }

    public ConnectTo(String TOIP,int TOPort,Socket Connection){
        ToIP = TOIP;
        ToPort = TOPort;
        connection = Connection;
    }
}
