package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical payment model representing standardized payment records.
 */
public class CanonicalPayment implements Serializable {
    
    @JsonProperty("txnId")
    private String txnId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("rail")
    private String rail;
    
    @JsonProperty("debtorIban")
    private String debtorIban;
    
    @JsonProperty("creditorIban")
    private String creditorIban;
    
    @JsonProperty("endToEndId")
    private String endToEndId;
    
    @JsonProperty("processedAt")
    private Instant processedAt;
    
    @JsonProperty("fileId")
    private String fileId;
    
    @JsonProperty("routingInfo")
    private Object routingInfo;
    
    @JsonProperty("limits")
    private Object limits;
    
    public CanonicalPayment() {
        this.processedAt = Instant.now();
    }
    
    public CanonicalPayment(String txnId, BigDecimal amount, String currency, String rail) {
        this.txnId = txnId;
        this.amount = amount;
        this.currency = currency;
        this.rail = rail;
        this.processedAt = Instant.now();
    }
    
    public String getTxnId() {
        return txnId;
    }
    
    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getRail() {
        return rail;
    }
    
    public void setRail(String rail) {
        this.rail = rail;
    }
    
    public String getDebtorIban() {
        return debtorIban;
    }
    
    public void setDebtorIban(String debtorIban) {
        this.debtorIban = debtorIban;
    }
    
    public String getCreditorIban() {
        return creditorIban;
    }
    
    public void setCreditorIban(String creditorIban) {
        this.creditorIban = creditorIban;
    }
    
    public String getEndToEndId() {
        return endToEndId;
    }
    
    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public Object getRoutingInfo() {
        return routingInfo;
    }
    
    public void setRoutingInfo(Object routingInfo) {
        this.routingInfo = routingInfo;
    }
    
    public Object getLimits() {
        return limits;
    }
    
    public void setLimits(Object limits) {
        this.limits = limits;
    }
    
    @Override
    public String toString() {
        return "CanonicalPayment{" +
                "txnId='" + txnId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", rail='" + rail + '\'' +
                ", fileId='" + fileId + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}

