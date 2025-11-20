package org.newdawn.spaceinvaders;

    public class AuthFormState {
        private boolean signupMode = false;  // false: 로그인, true: 회원가입
        private String email = "";
        private String password = "";
        private String password2 = "";
        private String message = "";
        private int fieldIndex = 0;          // 0=email, 1=password, 2=password2

        // --- getter ---
        public boolean isSignupMode()   { return signupMode; }
        public String  getEmail()       { return email; }
        public String  getPassword()    { return password; }
        public String  getPassword2()   { return password2; }
        public String  getMessage()     { return message; }
        public int     getFieldIndex()  { return fieldIndex; }
        public void    setEmail(String s)  { this.email = s; }
        public void    setPassword(String password) { this.password = password; }
        public void    setPassword2(String password2) { this.password2 = password2; }


        public void toggleMode() {
            signupMode = !signupMode;
            fieldIndex = 0;
            message = signupMode ? "회원가입 모드" : "로그인 모드";
        }


        public void moveFieldUp() {
            fieldIndex = Math.max(0, fieldIndex - 1);
        }
        public void moveFieldDown() {
            int max = signupMode ? 2 : 1;
            fieldIndex = Math.min(max, fieldIndex + 1);
        }

        /** 메세지 세터 (컨트롤러에서 사용) */
        public void setMessage(String msg) {
            this.message = msg != null ? msg : "";
        }



    }

