package com.httpserver.demo.api;

import java.io.File;
import java.io.IOException;

/**
 * The {@code FileContextHandler} services a context by mapping it
 * to a file or folder (recursively) on disk.
 */
public class FileContextHandler implements ContextHandler {

    protected final File base;
    protected Utility utility;

    public FileContextHandler(File dir) throws IOException {
        this.base = dir.getCanonicalFile();
        this.utility = new Utility();
    }

    public int serve(Request req, Response resp) throws IOException {
        return utility.serveFile(base, req.getContext().getPath(), req, resp);
    }
}
