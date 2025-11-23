package org.newdawn.spaceinvaders.database;

import java.io.Serializable;

public class AuthSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String uid;
    private final String email;
    private final transient String idToken;

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
