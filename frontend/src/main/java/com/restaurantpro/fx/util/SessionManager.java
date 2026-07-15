package com.restaurantpro.fx.util;


public class SessionManager {

    private static SessionManager instance;

    private Long   userId;
    private String nom;
    private String role;
    private String token;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(Long id, String nom, String role, String token) {
        this.userId = id;
        this.nom    = nom;
        this.role   = role;
        this.token  = token;
        ApiClient.setToken(token);
    }

    public void logout() {
        userId = null; nom = null; role = null; token = null;
        ApiClient.setToken(null);
    }

    public Long   getUserId() { return userId; }
    public String getNom()    { return nom;    }
    public String getRole()   { return role;   }
    public String getToken()  { return token;  }

    public boolean isAdmin()     { return "ADMIN".equals(role);     }
    public boolean isServeur()   { return "SERVEUR".equals(role);   }
    public boolean isCuisinier() { return "CUISINIER".equals(role); }
}
