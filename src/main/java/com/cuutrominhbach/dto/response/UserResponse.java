package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private Role role;
    private String walletAddress;
    private String province;
    private Boolean isApproved;
    private String approvalStatus;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.fullName = user.getFullName();
        r.role = user.getRole();
        r.walletAddress = user.getWalletAddress();
        r.province = user.getProvince();
        r.isApproved = user.getIsApproved();
        if (user.getIsApproved() == null) {
            r.approvalStatus = "PENDING";
        } else if (Boolean.TRUE.equals(user.getIsApproved())) {
            r.approvalStatus = "APPROVED";
        } else {
            r.approvalStatus = "REJECTED";
        }
        return r;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getWalletAddress() { return walletAddress; }
    public String getProvince() { return province; }
    public Boolean getIsApproved() { return isApproved; }
    public String getApprovalStatus() { return approvalStatus; }
}
