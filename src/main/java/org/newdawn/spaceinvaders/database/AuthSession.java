package org.newdawn.spaceinvaders.database;


public class AuthSession {

    private final String uid;
    private final String email;
    private final String idToken;

    public AuthSession(String uid, String email, String idToken) {
        this.uid = uid;
        this.email = email;
        this.idToken = idToken;
    }

    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getIdToken() { return idToken; }

    public boolean isLoggedIn(){
        return uid != null && idToken != null;
    }

}
