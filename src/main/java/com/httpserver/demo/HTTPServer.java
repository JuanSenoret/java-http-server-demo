package com.httpserver.demo;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.httpserver.demo.api.VirtualHost;
import com.httpserver.demo.api.SocketHandlerThread;
import com.httpserver.demo.api.Request;
import com.httpserver.demo.api.Response;
import com.httpserver.demo.api.ContextHandler;
import com.httpserver.demo.api.FileContextHandler;
import com.httpserver.demo.api.Utility;

public class HTTPServer {

    protected volatile int port;
    protected volatile int socketTimeout = 10000;
    protected volatile ServerSocketFactory serverSocketFactory;
    protected volatile boolean secure;
    protected volatile Executor executor;
    protected volatile ServerSocket serv;
    protected final Map<String, VirtualHost> hosts = new ConcurrentHashMap<String, VirtualHost>();
    protected Utility utility;

    /**
     * Constructs an HTTPServer which can accept connections on the given port.
     * Note: the {@link #start()} method must be called to start accepting
     * connections.
     *
     * @param port the port on which this server will accept connections
     */
    public HTTPServer(int port) {
        setPort(port);
        addVirtualHost(new VirtualHost(null)); // add default virtual host
    }

    /**
     * Constructs an HTTPServer which can accept connections on the default HTTP port 4200.
     * Note: the {@link #start()} method must be called to start accepting connections.
     */
    public HTTPServer() {
        this(4200);
    }

    /**
     * Sets the port on which this server will accept connections.
     *
     * @param port the port on which this server will accept connections
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the virtual host with the given name.
     *
     * @param name the name of the virtual host to return, or null for
     *        the default virtual host
     * @return the virtual host with the given name, or null if it doesn't exist
     */
    public VirtualHost getVirtualHost(String name) {
        return hosts.get(name == null ? VirtualHost.DEFAULT_HOST_NAME : name);
    }

    /**
     * Returns all virtual hosts.
     *
     * @return all virtual hosts (as an unmodifiable set)
     */
    public Set<VirtualHost> getVirtualHosts() {
        return Collections.unmodifiableSet(new HashSet<VirtualHost>(hosts.values()));
    }

    /**
     * Adds the given virtual host to the server.
     * If the host's name or aliases already exist, they are overwritten.
     *
     * @param host the virtual host to add
     */
    public void addVirtualHost(VirtualHost host) {
        String name = host.getName();
        hosts.put(name == null ? VirtualHost.DEFAULT_HOST_NAME : name, host);
    }

    /**
     * Starts this server. If it is already started, does nothing.
     * Note: Once the server is started, configuration-altering methods
     * of the server and its virtual hosts must not be used. To modify the
     * configuration, the server must first be stopped.
     *
     * @throws IOException if the server cannot begin accepting connections
     */
    public synchronized void start() throws IOException {
        if (serv != null) {
            return;
        }
        if (serverSocketFactory == null) {
            serverSocketFactory = ServerSocketFactory.getDefault(); // plain sockets
        } // assign default server socket factory if needed
        serv = createServerSocket();
        if (executor == null) // assign default executor if needed
            executor = Executors.newCachedThreadPool(); // consumes no resources when idle
        // register all host aliases (which may have been modified)
        for (VirtualHost host : getVirtualHosts())
            for (String alias : host.getAliases())
                hosts.put(alias, host);
        // start handling incoming connections
        // MODIFIED BY JUAN SENORET: Added port, secure, hosts, serv, socketTimeout, executor parameters respect to the original implementation.
        // I have organize the project code in files and avoid a thousend of line file.
        new SocketHandlerThread(port, secure, hosts, serv, socketTimeout, executor).start();
    }

    /**
     * Creates the server socket used to accept connections, using the configured
     *
     * @return the created server socket
     * @throws IOException if the socket cannot be created
     */
    protected ServerSocket createServerSocket() throws IOException {
        ServerSocket serv = serverSocketFactory.createServerSocket();
        serv.setReuseAddress(true);
        serv.bind(new InetSocketAddress(port));
        return serv;
    }

    /**
     * Starts a stand-alone HTTP server, serving files from disk.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // MODIFIED BY JUAN SENORET:
        Utility utility = new Utility();
        try {
            if (args.length == 0) {
                System.err.printf("Usage: java [-options] %s <directory> [port]%n", HTTPServer.class.getName());
                //return;
            } else {
                File dir = new File(args[0]);
                if (!dir.canRead()) {
                    throw new FileNotFoundException(dir.getAbsolutePath());
                }
                int port = args.length < 2 ? 4200 : Integer.parseInt(args[1]);
                // set up server
                for (File f : Arrays.asList(new File("/etc/mime.types"), new File(dir, ".mime.types")))
                    if (f.exists())
                        utility.addContentTypes(f);
                HTTPServer server = new HTTPServer(port);
                VirtualHost host = server.getVirtualHost(null); // default host
                host.setAllowGeneratedIndex(true); // with directory index pages
                // Add Endpoints to the server
                host.addContext("/", new FileContextHandler(dir));
                host.addContext("/api/keep-alive", new ContextHandler() {
                    public int serve(Request req, Response resp) throws IOException {
                        long now = System.currentTimeMillis();
                        resp.getHeaders().add("Content-Type", "text/plain");
                        resp.send(200, String.format("Keep Alive %tF %<tT", now));
                        return 0;
                    }
                });
                server.start();
                System.out.println("HTTPServer is listening on following port " + port);
            }
        } catch(Exception e) {
            System.err.println("error: " + e);
        }
    }
}
