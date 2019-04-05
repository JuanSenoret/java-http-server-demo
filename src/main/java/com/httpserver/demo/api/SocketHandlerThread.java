package com.httpserver.demo.api;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The {@code SocketHandlerThread} handles accepted sockets.
 */
public class SocketHandlerThread extends Thread {
    int port;
    protected boolean secure;
    int socketTimeout;
    Executor executor;
    ServerSocket serv;
    protected Utility utility;
    protected Map<String, VirtualHost> hosts;
    // MODIFIED BY JUAN SENORET: Constructor changed to add relevant parameters to call get port and host information
    public SocketHandlerThread(int port, boolean secure, Map<String, VirtualHost> hosts, ServerSocket serv, int socketTimeout, Executor executor) {
        this.port = port;
        this.secure = secure;
        this.hosts = hosts;
        this.serv = serv;
        this.executor = executor;
        this.socketTimeout = socketTimeout;
        this.utility = new Utility();
    }

    @Override
    public void run() {
        setName(getClass().getSimpleName() + "-" + this.port);
        try {
            ServerSocket serv = this.serv; // keep local to avoid NPE when stopped
            while (serv != null && !serv.isClosed()) {
                final Socket sock = serv.accept();
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            try {
                                sock.setSoTimeout(socketTimeout);
                                sock.setTcpNoDelay(true); // we buffer anyway, so improve latency
                                handleConnection(sock.getInputStream(), sock.getOutputStream());
                            } finally {
                                try {
                                    // RFC7230#6.6 - close socket gracefully
                                    // (except SSL socket which doesn't support half-closing)
                                    if (!(sock instanceof SSLSocket)) {
                                        sock.shutdownOutput(); // half-close socket (only output)
                                        utility.transfer(sock.getInputStream(), null, -1); // consume input
                                    }
                                } finally {
                                    sock.close(); // and finally close socket fully
                                }
                            }
                        } catch (IOException ignore) {}
                    }
                });
            }
        } catch (IOException ignore) {}
    }

    /**
     * Handles communications for a single connection over the given streams.
     * Multiple subsequent transactions are handled on the connection,
     * until the streams are closed, an error occurs, or the request
     * contains a "Connection: close" header which explicitly requests
     * the connection be closed after the transaction ends.
     *
     * @param in the stream from which the incoming requests are read
     * @param out the stream into which the outgoing responses are written
     * @throws IOException if an error occurs
     */
    protected void handleConnection(InputStream in, OutputStream out) throws IOException {
        in = new BufferedInputStream(in, 4096);
        out = new BufferedOutputStream(out, 4096);
        Request req;
        Response resp;
        do {
            // create request and response and handle transaction
            req = null;
            resp = new Response(out);
            try {
                // MODIFIED BY JUAN SENORET: Changed the Request class in order to pass port and hosts information
                req = new Request(in, port, secure, hosts);
                utility.handleTransaction(req, resp);
            } catch (Throwable t) { // unhandled errors (not normal error responses like 404)
                if (req == null) { // error reading request
                    if (t instanceof IOException && t.getMessage().contains("missing request line"))
                        break; // we're not in the middle of a transaction - so just disconnect
                    resp.getHeaders().add("Connection", "close"); // about to close connection
                    if (t instanceof InterruptedIOException) // e.g. SocketTimeoutException
                        resp.sendError(408, "Timeout waiting for client request");
                    else
                        resp.sendError(400, "Invalid request: " + t.getMessage());
                } else if (!resp.headersSent()) { // if headers were not already sent, we can send an error response
                    resp = new Response(out); // ignore whatever headers may have already been set
                    resp.getHeaders().add("Connection", "close"); // about to close connection
                    resp.sendError(500, "Error processing request: " + t.getMessage());
                } // otherwise just abort the connection since we can't recover
                break; // proceed to close connection
            } finally {
                resp.close(); // close response and flush output
            }
            // consume any leftover body data so next request can be processed
            utility.transfer(req.getBody(), null, -1);
            // RFC7230#6.6: persist connection unless client or server close explicitly (or legacy client)
        } while (!"close".equalsIgnoreCase(req.getHeaders().get("Connection"))
                && !"close".equalsIgnoreCase(resp.getHeaders().get("Connection")) && req.getVersion().endsWith("1.1"));
    }
}
