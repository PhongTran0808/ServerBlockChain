package com.cuutrominhbach.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @jakarta.persistence.Column(name = "wallet_address", length = 100)
    private String walletAddress;

    @Column(name = "hash_password")
    private String hashPassword;

    private String province;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {}

    public User(Long id, String username, String fullName, Role role,
                String walletAddress, String hashPassword, String province,
                Boolean isApproved, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.walletAddress = walletAddress;
        this.hashPassword = hashPassword;
        this.province = province;
        this.isApproved = isApproved;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getWalletAddress() { return walletAddress; }
    public String getHashPassword() { return hashPassword; }
    public String getProvince() { return province; }
    public Boolean getIsApproved() { return isApproved; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(Role role) { this.role = role; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
    public void setHashPassword(String hashPassword) { this.hashPassword = hashPassword; }
    public void setProvince(String province) { this.province = province; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String username;
        private String fullName;
        private Role role;
        private String walletAddress;
        private String hashPassword;
        private String province;
        private Boolean isApproved;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder walletAddress(String walletAddress) { this.walletAddress = walletAddress; return this; }
        public Builder hashPassword(String hashPassword) { this.hashPassword = hashPassword; return this; }
        public Builder province(String province) { this.province = province; return this; }
        public Builder isApproved(Boolean isApproved) { this.isApproved = isApproved; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public User build() {
            return new User(id, username, fullName, role, walletAddress,
                    hashPassword, province, isApproved, createdAt);
        }
    }
}
