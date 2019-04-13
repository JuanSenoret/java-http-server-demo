package com.httpserver.demo.api;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.json.JSONObject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoException;
import org.bson.BsonDateTime;
import org.bson.Document;

public class AddWallCommentContextHandler implements ContextHandler {

    private static final String DATABASE_NAME = "wallDB";
    private static final String DATABASE_URI = "mongodb://localhost:27017";
    private static final String COLLECTION_NAME = "wallcollection";
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private MongoCollection<Document> collection = null;
    private JSONObject jsonResponse = new JSONObject();

    public AddWallCommentContextHandler() {
    }

    public int serve(Request req, Response resp) throws IOException {
        return storeComment(req, resp);
    }

    private int storeComment(Request req, Response resp) throws IOException {
        // Access to the request parameters passed in the body
        Map<String, String> paramList = req.getParams();
        // Connect to your local Mongo DB (no credential is required to execute this code)
        try {
            mongoClient = MongoClients.create(DATABASE_URI);
            database = mongoClient.getDatabase(DATABASE_NAME);
            collection = database.getCollection(COLLECTION_NAME);
            Document doc = new Document("username", paramList.get("username"))
                                .append("comment", paramList.get("comment"))
                                .append("date", new Date())
                                .append("time-stamp", new BsonDateTime(System.currentTimeMillis()));
            collection.insertOne(doc);
            jsonResponse = new JSONObject();
            jsonResponse.put("message", "Comment successfully stored. No of comments in DB " + collection.countDocuments());
            jsonResponse.put("status", "DONE");
            jsonResponse.put("error", "");
        } catch (MongoException e) {
            System.out.println("MongoDB error");
            resp.getHeaders().add("Content-Type", "application/json");
            resp.getHeaders().add("Access-Control-Allow-Origin", "*");
            resp.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            resp.getHeaders().add("Access-Control-Allow-Methods", "POST");
            jsonResponse = new JSONObject();
            jsonResponse.put("message", "");
            jsonResponse.put("error", "MongoDB error");
            jsonResponse.put("status", "ERROR");
            resp.send(400, jsonResponse.toString());
            return 400;
        } finally {
            mongoClient.close();
        }
        resp.getHeaders().add("Content-Type", "application/json");
        resp.getHeaders().add("Access-Control-Allow-Origin", "*");
        resp.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.getHeaders().add("Access-Control-Allow-Methods", "POST");
        resp.send(200, jsonResponse.toString());
        return 200;
    }
}
