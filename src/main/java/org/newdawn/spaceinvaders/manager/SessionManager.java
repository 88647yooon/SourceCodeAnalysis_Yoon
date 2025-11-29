package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.database.AuthSession;

public class SessionManager {
    private AuthSession session;

    // 현재 세션 반환 (없으면 null)
    public AuthSession getSession() {
        return session;
    }

    // 로그인 성공 후 세션 설정
    public void setSession(AuthSession session) {
        this.session = session;
    }

    // 로그인 되어 있는지 여부
    public boolean hasSession() {
        return session != null && session.isLoggedIn();
    }

    // 로그아웃: 세션 제거
    public void clearSession() {
        this.session = null;
    }
}
