package com.httpserver.demo.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

/**
 * The {@code Request} class encapsulates a single HTTP request.
 */
public class Request {
    protected String method;
    protected URI uri;
    protected URL baseURL; // cached value
    protected String version;
    protected Headers headers;
    protected InputStream body;
    protected Map<String, String> params; // cached value
    protected VirtualHost host; // cached value
    protected VirtualHost.ContextInfo context; // cached value
    protected Map<String, VirtualHost> hosts;
    protected boolean secure;
    protected int port;
    protected Utility utility;

    /**
     * Constructs a Request from the data in the given input stream.
     *
     * @param in the input stream from which the request is read
     * @throws IOException if an error occurs
     */
    public Request(InputStream in, int port, boolean secure, Map<String, VirtualHost> hosts) throws IOException {
        // MODIFIED BY JUAN SENORET: Changed the Request class in order to pass port and hosts information
        this.port = port;
        this.secure = secure;
        this.hosts = hosts;

        this.utility = new Utility();

        readRequestLine(in);
        headers = utility.readHeaders(in);
        // RFC2616#3.6 - if "chunked" is used, it must be the last one
        // RFC2616#4.4 - if non-identity Transfer-Encoding is present,
        // it must either include "chunked" or close the connection after
        // the body, and in any case ignore Content-Length.
        // if there is no such Transfer-Encoding, use Content-Length
        // if neither header exists, there is no body
        String header = headers.get("Transfer-Encoding");
        if (header != null && !header.toLowerCase(Locale.US).equals("identity")) {
            if (Arrays.asList(utility.splitElements(header, true)).contains("chunked"))
                body = new ChunkedInputStream(in, headers);
            else
                body = in; // body ends when connection closes
        } else {
            header = headers.get("Content-Length");
            long len = header == null ? 0 : utility.parseULong(header, 10);
            body = new LimitedInputStream(in, len, false);
        }
    }

    /**
     * Returns the request method.
     *
     * @return the request method
     */
    public String getMethod() { return method; }

    /**
     * Returns the request URI.
     *
     * @return the request URI
     */
    public URI getURI() { return uri; }

    /**
     * Returns the request version string.
     *
     * @return the request version string
     */
    public String getVersion() { return version; }

    /**
     * Returns the request headers.
     *
     * @return the request headers
     */
    public Headers getHeaders() { return headers; }

    /**
     * Returns the input stream containing the request body.
     *
     * @return the input stream containing the request body
     */
    public InputStream getBody() { return body; }

    /**
     * Returns the path component of the request URI, after
     * URL decoding has been applied (using the UTF-8 charset).
     *
     * @return the decoded path component of the request URI
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * Sets the path component of the request URI. This can be useful
     * in URL rewriting, etc.
     *
     * @param path the path to set
     * @throws IllegalArgumentException if the given path is malformed
     */
    public void setPath(String path) {
        try {
            uri = new URI(uri.getScheme(), uri.getHost(),
                    utility.trimDuplicates(path, '/'), uri.getFragment());
            context = null; // clear cached context so it will be recalculated
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("error setting path", use);
        }
    }

    /**
     * Returns the base URL (scheme, host and port) of the request resource.
     * The host name is taken from the request URI or the Host header or a
     * default host (see RFC2616#5.2).
     *
     * @return the base URL of the requested resource, or null if it
     *         is malformed
     */
    public URL getBaseURL() {
        if (baseURL != null)
            return baseURL;
        // normalize host header
        String host = uri.getHost();
        if (host == null) {
            host = headers.get("Host");
            if (host == null) // missing in HTTP/1.0
                host = utility.detectLocalHostName();
        }
        int pos = host.indexOf(':');
        host = pos < 0 ? host : host.substring(0, pos);
        try {
            return baseURL = new URL(secure ? "https" : "http", host, port, "");
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    /**
     * Returns the request parameters, which are parsed both from the query
     * part of the request URI, and from the request body if its content
     * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
     * UTF-8 encoding is assumed in both cases.
     * <p>
     * The parameters are returned as a list of string arrays, each containing
     * the parameter name as the first element and its corresponding value
     * as the second element (or an empty string if there is no value).
     * <p>
     * The list retains the original order of the parameters.
     *
     * @return the request parameters name-value pairs,
     *         or an empty list if there are none
     * @throws IOException if an error occurs
     * @see Utility#parseParamsList(String)
     */
    public List<String[]> getParamsList() throws IOException {
        List<String[]> queryParams = utility.parseParamsList(uri.getRawQuery());
        List<String[]> bodyParams = Collections.emptyList();
        String ct = headers.get("Content-Type");
        // MODIFIED BY JUAN SENORET: to allow to accept Content-Type: application/json
        /*if (ct != null && (ct.toLowerCase(Locale.US).startsWith("application/x-www-form-urlencoded"))) {
            bodyParams = utility.parseParamsList(utility.readToken(body, -1, "UTF-8", 2097152)); // 2MB limit
        }*/
        if (ct != null) {
            if (ct.toLowerCase(Locale.US).startsWith("application/x-www-form-urlencoded")) {
                bodyParams = utility.parseParamsList(utility.readToken(body, -1, "UTF-8", 2097152)); // 2MB limit
            } else if (ct.toLowerCase(Locale.US).startsWith("application/json")) {
                bodyParams = utility.parseParamsListApplicationJson(utility.readToken(body, -1, "UTF-8", 2097152)); // 2MB limit
            }
        }
        if (bodyParams.isEmpty()) {
            return queryParams;
        }
        if (queryParams.isEmpty()) {
            return bodyParams;
        }
        queryParams.addAll(bodyParams);
        return queryParams;
    }

    /**
     * Returns the request parameters, which are parsed both from the query
     * part of the request URI, and from the request body if its content
     * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
     * UTF-8 encoding is assumed in both cases.
     * <p>
     * For multivalued parameters (i.e. multiple parameters with the same
     * name), only the first one is considered. For access to all values,
     * use {@link #getParamsList()} instead.
     * <p>
     * The map iteration retains the original order of the parameters.
     *
     * @return the request parameters name-value pairs,
     *         or an empty map if there are none
     * @throws IOException if an error occurs
     * @see #getParamsList()
     */
    public Map<String, String> getParams() throws IOException {
        if (params == null)
            params = utility.toMap(getParamsList());
        return params;
    }

    /**
     * Returns the absolute (zero-based) content range value read
     * from the Range header. If multiple ranges are requested, a single
     * range containing all of them is returned.
     *
     * @param length the full length of the requested resource
     * @return the requested range, or null if the Range header
     *         is missing or invalid
     */
    public long[] getRange(long length) {
        String header = headers.get("Range");
        return header == null || !header.startsWith("bytes=")
                ? null : utility.parseRange(header.substring(6), length);
    }

    /**
     * Reads the request line, parsing the method, URI and version string.
     *
     * @param in the input stream from which the request line is read
     * @throws IOException if an error occurs or the request line is invalid
     */
    protected void readRequestLine(InputStream in) throws IOException {
        // RFC2616#4.1: should accept empty lines before request line
        // RFC2616#19.3: tolerate additional whitespace between tokens
        String line;
        try {
            do { line = utility.readLine(in); } while (line.length() == 0);
        } catch (IOException ioe) { // if EOF, timeout etc.
            throw new IOException("missing request line"); // signal that the request did not begin
        }
        String[] tokens = utility.split(line, " ", -1);
        if (tokens.length != 3)
            throw new IOException("invalid request line: \"" + line + "\"");
        try {
            method = tokens[0];
            // must remove '//' prefix which constructor parses as host name
            uri = new URI(utility.trimDuplicates(tokens[1], '/'));
            version = tokens[2]; // RFC2616#2.1: allow implied LWS; RFC7230#3.1.1: disallow it
        } catch (URISyntaxException use) {
            throw new IOException("invalid URI: " + use.getMessage());
        }
    }

    /**
     * Returns the virtual host corresponding to the requested host name,
     * or the default host if none exists.
     *
     * @return the virtual host corresponding to the requested host name,
     *         or the default virtual host
     */
    public VirtualHost getVirtualHost() {
        return host != null ? host
                : (host = getVirtualHostByName(getBaseURL().getHost())) != null ? host
                : (host = getVirtualHostByName(null));
    }

    /**
     * Returns the virtual host with the given name.
     *
     * @param name the name of the virtual host to return, or null for
     *        the default virtual host
     * @return the virtual host with the given name, or null if it doesn't exist
     */
    public VirtualHost getVirtualHostByName(String name) {
        return hosts.get(name == null ? VirtualHost.DEFAULT_HOST_NAME : name);
    }

    /**
     * Returns the info of the context handling this request.
     *
     * @return the info of the context handling this request, or an empty context
     */
    public VirtualHost.ContextInfo getContext() {
        return context != null ? context : (context = getVirtualHost().getContext(getPath()));
    }

}
