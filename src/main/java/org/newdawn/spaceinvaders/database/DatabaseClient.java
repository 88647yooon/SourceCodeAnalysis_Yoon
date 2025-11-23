package org.newdawn.spaceinvaders.database;

public interface DatabaseClient {

    String get(String path, String idToken) throws Exception;

    String getWithQuery(String path, String idToken, String query) throws Exception;

    String put(String path, String idToken, String json) throws Exception;

    String post(String path, String idToken, String json) throws Exception;

}
