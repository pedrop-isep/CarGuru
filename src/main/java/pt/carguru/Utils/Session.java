package pt.carguru.Utils;

import pt.carguru.Models.User;

public class Session {
    private static User currentUser = null;

    public static void setUser(User user) { currentUser = user; }
    public static User getUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() { return currentUser != null && "admin".equals(currentUser.getRole()); }
    public static void clear() { currentUser = null; }
}
