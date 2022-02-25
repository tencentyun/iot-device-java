package com.tencent.iot.hub.device.java.core.mqtt;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @descrition 指令linux指令
 */
public class SSHShell {

    private Session session;
    private ChannelExec channelExec;

    /**
     *   默认执行本地linux指令
     */
    public static String exeLocLinuxCmd(String cmd){
        String result = "";
        Runtime run = Runtime.getRuntime();
        try {
            Process process = run.exec(cmd);
            InputStream in = process.getInputStream();
            BufferedReader bs = new BufferedReader(new InputStreamReader(in));
            process.destroy();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *   远程执行linux指令
     */
    public static String exeLinuxBySSH(String host, int port, String user, String password, String command) throws JSchException, IOException {

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");

        session.setPassword(password);
        session.connect();

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String out = IOUtils.toString(in, "UTF-8");

        channelExec.disconnect();
        session.disconnect();

        return out;
    }

    /**
     * 创建连接
     */
    public Session connect(String host, int port, String user, String password) throws JSchException{
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");

        session.setPassword(password);
        session.connect();

        channelExec = (ChannelExec) session.openChannel("exec");
        return  session;
    }

    /**
     * 执行指令
     */
    public String execCmd(String command) throws JSchException, IOException{

        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String out = IOUtils.toString(in, "UTF-8");
        return out;
    }

    /**
     * 关闭连接
     */
    public void close(){
        channelExec.disconnect();
        session.disconnect();
    }
}


