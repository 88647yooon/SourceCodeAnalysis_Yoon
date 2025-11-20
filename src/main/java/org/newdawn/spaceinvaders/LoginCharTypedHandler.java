package org.newdawn.spaceinvaders;

public class LoginCharTypedHandler {
    private final AuthFormState form;

    public LoginCharTypedHandler(AuthFormState form) {
        this.form = form;
    }

    public void CharTypedHandler(char c) {
        int index      = form.getFieldIndex();
        String email   = form.getEmail();
        String pw      = form.getPassword();
        String pw2     = form.getPassword2();

        if (c == '\b') { // 백스페이스
            if (index == 0 && !email.isEmpty()) {
                form.setEmail(email.substring(0, email.length() - 1));
            }
            if (index == 1 && !pw.isEmpty()) {
                form.setPassword(pw.substring(0, pw.length() - 1));
            }
            if (index == 2 && !pw2.isEmpty()) {
                form.setPassword2(pw2.substring(0, pw2.length() - 1));
            }
            return;
        }
        if (Character.isISOControl(c)) return;

        if (index == 0) {
            form.setEmail(email + c);
        } else if (index == 1) {
            form.setPassword(pw + c);
        } else if (index == 2) {
            form.setPassword2(pw2 + c);
        }
    }
}