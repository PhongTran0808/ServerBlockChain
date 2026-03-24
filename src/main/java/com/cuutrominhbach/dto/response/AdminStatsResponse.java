package com.cuutrominhbach.dto.response;

public class AdminStatsResponse {
    private Long totalFund;
    private Long totalCitizens;
    private Long approvedShops;
    private Long totalAirdrops;

    public AdminStatsResponse(Long totalFund, Long totalCitizens, Long approvedShops, Long totalAirdrops) {
        this.totalFund = totalFund;
        this.totalCitizens = totalCitizens;
        this.approvedShops = approvedShops;
        this.totalAirdrops = totalAirdrops;
    }

    public Long getTotalFund() { return totalFund; }
    public Long getTotalCitizens() { return totalCitizens; }
    public Long getApprovedShops() { return approvedShops; }
    public Long getTotalAirdrops() { return totalAirdrops; }
}
