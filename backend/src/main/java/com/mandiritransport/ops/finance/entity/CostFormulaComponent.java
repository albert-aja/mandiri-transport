package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("cost_formula_components")
public class CostFormulaComponent {

    @TableId(type = IdType.INPUT)
    private String id;
    private String formulaVersionId;
    private String componentCode;
    private String name;
    private String calcType;
    private String reconciliationPolicy;
    private String expenseCategoryLink;
    private BigDecimal fixedAmount;
    private BigDecimal rateValue;
    private BigDecimal monthlyAmount;
    private Integer assumedMonthlyTrips;
    private String expressionFormula;
    private String parametersJson;
    private String vehicleTypeFilter;
    private Integer sortOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFormulaVersionId() { return formulaVersionId; }
    public void setFormulaVersionId(String formulaVersionId) { this.formulaVersionId = formulaVersionId; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCalcType() { return calcType; }
    public void setCalcType(String calcType) { this.calcType = calcType; }
    public String getReconciliationPolicy() { return reconciliationPolicy; }
    public void setReconciliationPolicy(String reconciliationPolicy) { this.reconciliationPolicy = reconciliationPolicy; }
    public String getExpenseCategoryLink() { return expenseCategoryLink; }
    public void setExpenseCategoryLink(String expenseCategoryLink) { this.expenseCategoryLink = expenseCategoryLink; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public BigDecimal getRateValue() { return rateValue; }
    public void setRateValue(BigDecimal rateValue) { this.rateValue = rateValue; }
    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }
    public Integer getAssumedMonthlyTrips() { return assumedMonthlyTrips; }
    public void setAssumedMonthlyTrips(Integer assumedMonthlyTrips) { this.assumedMonthlyTrips = assumedMonthlyTrips; }
    public String getExpressionFormula() { return expressionFormula; }
    public void setExpressionFormula(String expressionFormula) { this.expressionFormula = expressionFormula; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public String getVehicleTypeFilter() { return vehicleTypeFilter; }
    public void setVehicleTypeFilter(String vehicleTypeFilter) { this.vehicleTypeFilter = vehicleTypeFilter; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
