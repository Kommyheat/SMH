package com.app.smh.scan;

public class DrugResultItem {

    private String recognizedName;
    private boolean expanded;
    private boolean loading;

    private String itemName;
    private String entpName;
    private String efcyQesitm;
    private String useMethodQesitm;
    private String atpnWarnQesitm;
    private String depositMethodQesitm;

    public DrugResultItem(String recognizedName) {
        this.recognizedName = recognizedName;
        this.expanded = false;
        this.loading = false;
    }

    public String getRecognizedName() {
        return recognizedName;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getEntpName() {
        return entpName;
    }

    public void setEntpName(String entpName) {
        this.entpName = entpName;
    }

    public String getEfcyQesitm() {
        return efcyQesitm;
    }

    public void setEfcyQesitm(String efcyQesitm) {
        this.efcyQesitm = efcyQesitm;
    }

    public String getUseMethodQesitm() {
        return useMethodQesitm;
    }

    public void setUseMethodQesitm(String useMethodQesitm) {
        this.useMethodQesitm = useMethodQesitm;
    }

    public String getAtpnWarnQesitm() {
        return atpnWarnQesitm;
    }

    public void setAtpnWarnQesitm(String atpnWarnQesitm) {
        this.atpnWarnQesitm = atpnWarnQesitm;
    }

    public String getDepositMethodQesitm() {
        return depositMethodQesitm;
    }

    public void setDepositMethodQesitm(String depositMethodQesitm) {
        this.depositMethodQesitm = depositMethodQesitm;
    }

    public boolean hasDetail() {
        return itemName != null && !itemName.isEmpty();
    }
}