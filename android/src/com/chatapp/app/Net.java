package com.chatapp.app;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TCP 长连接客户端, 断线自动重连, 每行一个 JSON。
 * 所有发送统一由网络线程从发送队列取出并写出, 避免跨线程写 socket 的竞态问题。
 */
public class Net {

    public interface Listener {
        void onConnected();

        void onMessage(JSONObject obj);

        void onDisconnected(String reason);
    }

    private static Net inst;

    public static Net get() {
        if (inst == null) inst = new Net();
        return inst;
    }

    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile boolean running;
    private volatile boolean connected;
    private Listener listener;
    private Listener extraListener;
    private Thread loop;
    private int reconnectCount = 0;
    private int sessionId = 0;
    private long lastWrite = 0;
    private final LinkedBlockingQueue<String> outbox = new LinkedBlockingQueue<>();

    public void setListener(Listener l) {
        this.listener = l;
    }

    /** 全局消息监听(App 级), 与页面监听同时收到消息, 用于后台通知 */
    public void setExtraListener(Listener l) {
        this.extraListener = l;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start(final String host, final int port) {
        start(host, port, null);
    }

    public synchronized void start(final String host, final int port, final String firstMessage) {
        if (running) stop();
        running = true;
        connected = false;
        reconnectCount = 0;
        if (firstMessage != null) outbox.offer(firstMessage);
        final int mySession = ++sessionId;
        loop = new Thread(new Runnable() {
            @Override
            public void run() {
                runLoop(host, port, mySession);
            }
        });
        loop.setDaemon(true);
        loop.start();
    }

    public synchronized void stop() {
        sessionId++;
        running = false;
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        socket = null;
        writer = null;
    }

    /** 发送: 放入队列, 由网络线程统一写出 */
    public boolean send(String json) {
        if (!running) return false;
        outbox.offer(json);
        return true;
    }

    public boolean send(JSONObject obj) {
        return send(obj.toString());
    }

    private void runLoop(String host, int port, int mySession) {
        while (running && mySession == sessionId) {
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(host, port), 6000);
                s.setKeepAlive(true);
                s.setTcpNoDelay(true);
                s.setSoTimeout(2000);
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
                socket = s;
                writer = w;
                connected = true;
                reconnectCount = 0;
                lastWrite = System.currentTimeMillis();
                if (listener != null) listener.onConnected();
                if (extraListener != null) { try { extraListener.onConnected(); } catch (Exception ignored) {} }
                drainOutbox(w);
                String line;
                while (running && mySession == sessionId) {
                    drainOutbox(w);
                    try {
                        line = r.readLine();
                    } catch (SocketTimeoutException te) {
                        long now = System.currentTimeMillis();
                        if (now - lastWrite > 12000) {
                            try {
                                w.write("{\"type\":\"ping\"}\n");
                                w.flush();
                                lastWrite = now;
                            } catch (Exception ignored) {
                            }
                        }
                        continue; // 超时后继续, 顺便清空发送队列
                    }
                    if (line == null) break;
                    if (line.trim().isEmpty()) continue;
                    try {
                        JSONObject obj = new JSONObject(line);
                        if (extraListener != null) { try { extraListener.onMessage(obj); } catch (Exception ignored) {} }
                        if (listener != null) listener.onMessage(obj);
                    } catch (Exception e) {
                        Log.e("Net", "bad message: " + line, e);
                    }
                }
            } catch (Exception e) {
                Log.w("Net", "connection error", e);
            } finally {
                if (socket == s) {
                    connected = false;
                    socket = null;
                    writer = null;
                }
                try {
                    s.close();
                } catch (Exception ignored) {
                }
                if (running && mySession == sessionId && listener != null && reconnectCount == 0) {
                    listener.onDisconnected("连接断开，正在重连...");
                }
                if (extraListener != null) { try { extraListener.onDisconnected("disconnected"); } catch (Exception ignored) {} }
            }
            if (!running || mySession != sessionId) break;
            reconnectCount++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /** 由网络线程把发送队列中的消息全部写出 */
    private void drainOutbox(BufferedWriter w) {
        if (w == null) return;
        String msg;
        while ((msg = outbox.poll()) != null) {
            try {
                w.write(msg);
                w.write("\n");
                w.flush();
                lastWrite = System.currentTimeMillis();
                Log.i("Net", "sent " + msg.length() + "B");
            } catch (Exception e) {
                Log.w("Net", "send failed: " + msg, e);
                break;
            }
        }
    }
}
