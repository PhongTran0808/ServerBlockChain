package com.cuutrominhbach.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_claims",
        uniqueConstraints = @UniqueConstraint(name = "uk_round_citizen", columnNames = {"round_id", "citizen_id"}))
public class DistributionClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id")
    private Long roundId;

    @Column(name = "citizen_id")
    private Long citizenId;

    private Long amount;

    @Lob
    @Column(name = "proof_json", columnDefinition = "LONGTEXT")
    private String proofJson;

    @Column(name = "claim_tx_hash")
    private String claimTxHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DistributionClaim() {
    }

    public Long getId() { return id; }
    public Long getRoundId() { return roundId; }
    public Long getCitizenId() { return citizenId; }
    public Long getAmount() { return amount; }
    public String getProofJson() { return proofJson; }
    public String getClaimTxHash() { return claimTxHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
    public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setProofJson(String proofJson) { this.proofJson = proofJson; }
    public void setClaimTxHash(String claimTxHash) { this.claimTxHash = claimTxHash; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
