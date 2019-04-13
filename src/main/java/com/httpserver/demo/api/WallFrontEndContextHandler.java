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
            System.out.println(relativePath);
            String result = "";

            if (relativePath.indexOf(".html") >=0) {
                String fileName = "dist/index.html";
                result = readResource(fileName, Charsets.UTF_8);
                resp.getHeaders().add("Content-Type", "text/html");
            } else if(relativePath.indexOf(".js") >= 0) {
                resp.getHeaders().add("Content-Type", "text/javascript");
                result = readResource("dist" + relativePath, Charsets.UTF_8);
            } else if(relativePath.indexOf(".jpg") >= 0) {
                resp.getHeaders().add("Content-Type", "image/jpg");
                result = readResource("dist" + relativePath, Charsets.UTF_8);
            }
            resp.send(200, result);
            return 200;
            /*if (relativePath.length() == 0) {
                String fileName = "dist/index.html";
                result = readResource(fileName, Charsets.UTF_8);
                resp.getHeaders().add("Content-Type", "text/html");
                resp.send(200, result);
                return 200;
            } else {
                if (relativePath.indexOf(".js") >= 0) {
                    resp.getHeaders().add("Content-Type", "text/javascript");
                } else if (relativePath.indexOf(".png") >= 0){
                    resp.getHeaders().add("Content-Type", "image/png");
                }
                result = readResource("dist/assets/img" + relativePath, Charsets.UTF_8);
                resp.send(200, result);
                return 200;
            }*/
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
