package com.jzk.motif_backend.model;


import jakarta.persistence.*;

@Entity
public class Friends {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "friend_id", nullable = false)
    private int friendId;
    private String relation;
    @Column(name = "user_id", nullable = false)
    private int userId;

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFriendId() { return friendId; }
    public void setFriendId(int friendId) { this.friendId = friendId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


}
