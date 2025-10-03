package com.github.moravcik.configtracker.lib.types;

import java.util.List;

public class Config {
    private CreditPolicy creditPolicy;
    private ApprovalPolicy approvalPolicy;
    private RiskScoring riskScoring;

    public static class CreditPolicy {
        private Double maxCreditLimit;
        private Double minCreditScore;
        private Currency currency;
        private List<Exception> exceptions;

        public enum Currency { EUR, USD, GBP }

        public static class Exception {
            private String segment;
            private Double maxCreditLimit;
            private Boolean requiresTwoManRule;

            public String getSegment() { return segment; }
            public void setSegment(String segment) { this.segment = segment; }
            public Double getMaxCreditLimit() { return maxCreditLimit; }
            public void setMaxCreditLimit(Double maxCreditLimit) { this.maxCreditLimit = maxCreditLimit; }
            public Boolean getRequiresTwoManRule() { return requiresTwoManRule; }
            public void setRequiresTwoManRule(Boolean requiresTwoManRule) { this.requiresTwoManRule = requiresTwoManRule; }
        }

        public Double getMaxCreditLimit() { return maxCreditLimit; }
        public void setMaxCreditLimit(Double maxCreditLimit) { this.maxCreditLimit = maxCreditLimit; }
        public Double getMinCreditScore() { return minCreditScore; }
        public void setMinCreditScore(Double minCreditScore) { this.minCreditScore = minCreditScore; }
        public Currency getCurrency() { return currency; }
        public void setCurrency(Currency currency) { this.currency = currency; }
        public List<Exception> getExceptions() { return exceptions; }
        public void setExceptions(List<Exception> exceptions) { this.exceptions = exceptions; }
    }

    public static class ApprovalPolicy {
        private Boolean twoManRule;
        private Double autoApproveThreshold;
        private List<Level> levels;

        public static class Level {
            private Role role;
            private Double limit;

            public enum Role { TEAM_LEAD, HEAD_OF_CREDIT, CFO }

            public Role getRole() { return role; }
            public void setRole(Role role) { this.role = role; }
            public Double getLimit() { return limit; }
            public void setLimit(Double limit) { this.limit = limit; }
        }

        public Boolean getTwoManRule() { return twoManRule; }
        public void setTwoManRule(Boolean twoManRule) { this.twoManRule = twoManRule; }
        public Double getAutoApproveThreshold() { return autoApproveThreshold; }
        public void setAutoApproveThreshold(Double autoApproveThreshold) { this.autoApproveThreshold = autoApproveThreshold; }
        public List<Level> getLevels() { return levels; }
        public void setLevels(List<Level> levels) { this.levels = levels; }
    }

    public static class RiskScoring {
        private Weights weights;
        private Thresholds thresholds;

        public static class Weights {
            private Double incomeToDebtRatio;
            private Double age;
            private Double historyLengthMonths;
            private Double delinquencyCount;

            public Double getIncomeToDebtRatio() { return incomeToDebtRatio; }
            public void setIncomeToDebtRatio(Double incomeToDebtRatio) { this.incomeToDebtRatio = incomeToDebtRatio; }
            public Double getAge() { return age; }
            public void setAge(Double age) { this.age = age; }
            public Double getHistoryLengthMonths() { return historyLengthMonths; }
            public void setHistoryLengthMonths(Double historyLengthMonths) { this.historyLengthMonths = historyLengthMonths; }
            public Double getDelinquencyCount() { return delinquencyCount; }
            public void setDelinquencyCount(Double delinquencyCount) { this.delinquencyCount = delinquencyCount; }
        }

        public static class Thresholds {
            private Double low;
            private Double medium;
            private Double high;

            public Double getLow() { return low; }
            public void setLow(Double low) { this.low = low; }
            public Double getMedium() { return medium; }
            public void setMedium(Double medium) { this.medium = medium; }
            public Double getHigh() { return high; }
            public void setHigh(Double high) { this.high = high; }
        }

        public Weights getWeights() { return weights; }
        public void setWeights(Weights weights) { this.weights = weights; }
        public Thresholds getThresholds() { return thresholds; }
        public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }
    }

    public CreditPolicy getCreditPolicy() { return creditPolicy; }
    public void setCreditPolicy(CreditPolicy creditPolicy) { this.creditPolicy = creditPolicy; }
    public ApprovalPolicy getApprovalPolicy() { return approvalPolicy; }
    public void setApprovalPolicy(ApprovalPolicy approvalPolicy) { this.approvalPolicy = approvalPolicy; }
    public RiskScoring getRiskScoring() { return riskScoring; }
    public void setRiskScoring(RiskScoring riskScoring) { this.riskScoring = riskScoring; }
}