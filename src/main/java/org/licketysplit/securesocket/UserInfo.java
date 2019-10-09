package org.licketysplit.securesocket;

import org.licketysplit.securesocket.messages.Message;

public class UserInfo {
    public static class UserIDMessage extends Message {
        private String username;

        public UserIDMessage(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public UserInfo getUserInfo() {
            return new UserInfo(username);
        }

        public UserIDMessage() {}

        @Override
        public byte[] toBytes() {
            return this.username.getBytes();
        }

        @Override
        public void fromBytes(byte[] data) {
            this.username = new String(data);
        }
    }

    public String getUsername() {
        return username;
    }

    private String username;

    public UserIDMessage getIDMessage() {
        return new UserIDMessage(username);
    }

    public UserInfo(String username) {
        this.username = username;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userObj = (UserInfo) o;
        return userObj.username.equals(username);
    }

    @Override

    public int hashCode() {
        int result = 0;
        for(int i = 0; i<username.length(); i++) {
            result *= 256;
            result += Character.getNumericValue(username.charAt(i));
        }
        return result;
    }
}
