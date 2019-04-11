package com.httpserver.demo.api;

import java.io.IOException;
import java.nio.charset.Charset;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class WallFrontEndContextHandler implements ContextHandler {

    public WallFrontEndContextHandler() throws IOException {
    }

    public int serve(Request req, Response resp) throws IOException {
        return serveFrontend(req, resp, req.getContext().getPath());
    }

    private int serveFrontend(Request req, Response resp, String context) throws IOException {
        String relativePath = req.getPath().substring(context.length());

        if (relativePath.endsWith("/")) {
            return 404; // non-directory ending with slash (File constructor removed it)
        } else {
            String result = "";
            if (relativePath.length() == 0) {
                String fileName = "dist/index.html";
                result = readResource(fileName, Charsets.UTF_8);
                resp.getHeaders().add("Content-Type", "text/html");
                resp.send(200, result);
                return 200;
            } else {
                resp.getHeaders().add("Content-Type", "text/js");
                result = readResource("dist" + relativePath, Charsets.UTF_8);
                resp.send(200, result);
                return 200;
            }
        }
    }

    private String readResource(final String fileName, Charset charset) {
        try {
            return Resources.toString(Resources.getResource(fileName), charset);
        } catch (IOException ioe) {
            System.out.println("Error");
            ioe.printStackTrace();
            return "<html><body>Unable to find resource</body></html>";
        }
    }
}
