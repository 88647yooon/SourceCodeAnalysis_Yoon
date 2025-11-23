package org.newdawn.spaceinvaders.input;

import org.newdawn.spaceinvaders.screen.auth.AuthFormState;

public class LoginCharTypedHandler {
    private AuthFormState form;

    public LoginCharTypedHandler(AuthFormState form) {
        this.form = form;
    }

    public void CharTypedHandler(char c) {
        if (c == '\b') {
            handleBackspace();
            return;
        }

        if (Character.isISOControl(c)) {
            return;
        }

        handleAppend(c);
    }

    private void handleBackspace() {
        int index = form.getFieldIndex();

        switch (index) {
            case 0:
                trimLastChar(form.getEmail(), newVal -> form.setEmail(newVal));
                break;
            case 1:
                trimLastChar(form.getPassword(), newVal -> form.setPassword(newVal));
                break;
            case 2:
                trimLastChar(form.getPassword2(), newVal -> form.setPassword2(newVal));
                break;
            default:
                // 아무 것도 안 함
                break;
        }
    }

    private void handleAppend(char c) {
        int index = form.getFieldIndex();

        switch (index) {
            case 0:
                form.setEmail(form.getEmail() + c);
                break;
            case 1:
                form.setPassword(form.getPassword() + c);
                break;
            case 2:
                form.setPassword2(form.getPassword2() + c);
                break;
            default:
                // 아무 것도 안 함
                break;
        }
    }

    private void trimLastChar(String value, java.util.function.Consumer<String> setter) {
        if (value == null || value.isEmpty()) {
            return;
        }
        setter.accept(value.substring(0, value.length() - 1));
    }

}