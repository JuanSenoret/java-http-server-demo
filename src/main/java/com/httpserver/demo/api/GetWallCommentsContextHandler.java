package com.httpserver.demo.api;

import org.json.JSONObject;
import org.json.JSONArray;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoException;
import com.mongodb.BasicDBObject;
import org.bson.Document;

import java.io.IOException;

public class GetWallCommentsContextHandler implements ContextHandler {

    private static final String DATABASE_NAME = "wallDB";
    private static final String DATABASE_URI = "mongodb://localhost:27017";
    private static final String COLLECTION_NAME = "wallcollection";
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private MongoCollection<Document> collection = null;
    private JSONObject jsonResponse = new JSONObject();

    public GetWallCommentsContextHandler() {
    }

    public int serve(Request req, Response resp) throws IOException {
        return getComments(req, resp);
    }

    private int getComments(Request req, Response resp) throws IOException {
        try {
            mongoClient = MongoClients.create(DATABASE_URI);
            database = mongoClient.getDatabase(DATABASE_NAME);
            collection = database.getCollection(COLLECTION_NAME);
            JSONArray jsonArray = new JSONArray();
            for (Document cur : collection.find().sort(new BasicDBObject("time-stamp", -1))) {
                JSONObject comment = new JSONObject();
                //System.out.println(cur.toJson());
                comment.put("username", cur.get("username"));
                comment.put("comment", cur.get("comment"));
                comment.put("date", cur.get("date"));
                jsonArray.put(comment);
            }
            jsonResponse.put("message", "Fetch comment successful done");
            jsonResponse.put("data", jsonArray);
            jsonResponse.put("status", "DONE");
            jsonResponse.put("error", "");
        } catch (MongoException e) {
            System.out.println("MongoDB error");
            resp.getHeaders().add("Content-Type", "application/json");
            resp.getHeaders().add("Access-Control-Allow-Origin", "*");
            resp.getHeaders().add("Access-Control-Allow-Methods", "GET");
            jsonResponse = new JSONObject();
            jsonResponse.put("error", "MongoDB error");
            jsonResponse.put("status", "ERROR");
            jsonResponse.put("message", "");
            resp.send(400, jsonResponse.toString());
            return 400;
        } finally {
            mongoClient.close();
        }
        resp.getHeaders().add("Content-Type", "application/json");
        resp.getHeaders().add("Access-Control-Allow-Origin", "*");
        resp.getHeaders().add("Access-Control-Allow-Methods", "GET");
        resp.send(200, jsonResponse.toString());
        return 200;
    }
}
