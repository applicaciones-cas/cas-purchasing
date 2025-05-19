/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.client.model.Model_Client_Address;
import org.guanzon.cas.client.model.Model_Client_Institution_Contact;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class Model_POReturn_Master extends Model {
        
    //reference objects
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;   
    Model_Client_Master poSupplier;    
    Model_Client_Address poSupplierAdress;
    Model_Client_Institution_Contact poSupplierContactPerson;    
    Model_POR_Master poPurchaseOrderReceiving;    
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dTransact", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nDiscount", 0.00);
            poEntity.updateObject("nAddDiscx", 0.00);
            poEntity.updateObject("nTranTotl", 0.00);
            poEntity.updateObject("nVATRatex", 0.00);
            poEntity.updateObject("nTWithHld", 0.00);
            poEntity.updateObject("nAmtPaidx", 0.00);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateString("cPrintxxx", Logical.NO);
            poEntity.updateString("cProcessd", Logical.NO);
            poEntity.updateString("cTranStat", PurchaseOrderReceivingStatus.OPEN);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            
            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poBranch = model.Branch();
            poIndustry = model.Industry();
            poCategory = model.Category();
            poCompany = model.Company();
            
            ClientModels clientModel = new ClientModels(poGRider); 
            poSupplier = clientModel.ClientMaster();
            poSupplierAdress = clientModel.ClientAddress();
            poSupplierContactPerson = clientModel.ClientInstitutionContact();
            
            PurchaseOrderReceivingModels porModel = new PurchaseOrderReceivingModels(poGRider); 
            poPurchaseOrderReceiving = porModel.PurchaseOrderReceivingMaster();
//            end - initialize reference objects
            
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    public JSONObject setTransactionNo(String transactionNo){
        return setValue("sTransNox", transactionNo);
    }
    
    public String getTransactionNo(){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setBranchCode(String branchCode){
        return setValue("sBranchCd", branchCode);
    }
    
    public String getBranchCode(){
        return (String) getValue("sBranchCd");
    }
    
    public JSONObject setIndustryId(String industryID){
        return setValue("sIndstCdx", industryID);
    }
    
    public String getIndustryId(){
        return (String) getValue("sIndstCdx");
    }
    
    public JSONObject setTransactionDate(Date transactionDate){
        return setValue("dTransact", transactionDate);
    }
    
    public Date getTransactionDate(){
        return (Date) getValue("dTransact");
    }
    
    public JSONObject setCompanyId(String companyID){
        return setValue("sCompnyID", companyID);
    }
    
    public String getCompanyId(){
        return (String) getValue("sCompnyID");
    }
    
    public JSONObject setSupplierId(String supplierID){
        return setValue("sSupplier", supplierID);
    }
    
    public String getSupplierId(){
        return (String) getValue("sSupplier");
    }
    
    public JSONObject setAddressId(String addressID){
        return setValue("sAddrssID", addressID);
    }
    
    public String getAddressId(){
        return (String) getValue("sAddrssID");
    }
    
    public JSONObject setContactId(String contactID){
        return setValue("sContctID", contactID);
    }
    
    public String getContactId(){
        return (String) getValue("sContctID");
    }
    
    public JSONObject setDiscountRate(Number discountRate){
        return setValue("nDiscount", discountRate);
    }
    
    public Number getDiscountRate(){
        return (Number) getValue("nDiscount");
    }
    
    public JSONObject setDiscount(Number discount){
        return setValue("nAddDiscx", discount);
    }
    
    public Number getDiscount(){
        return (Number) getValue("nAddDiscx");
    }
    
    public JSONObject setTransactionTotal(Number transactionTotal){
        return setValue("nTranTotl", transactionTotal);
    }
    
    public Number getTransactionTotal(){
        return (Number) getValue("nTranTotl");
    }
    
    public JSONObject setVatRate(Number vatRate){
        return setValue("nVATRatex", vatRate);
    }
    
    public Number getVatRate(){
        return (Number) getValue("nVATRatex");
    }
    
    public JSONObject setWithHoldingTax(Number withHoldingTax){
        return setValue("nTWithHld", withHoldingTax);
    }
    
    public Number getWithHoldingTax(){
        return (Number) getValue("nTWithHld");
    }
    
    public JSONObject setAmountPaid(Number amountPaid){
        return setValue("nAmtPaidx", amountPaid);
    }
    
    public Number getAmountPaid(){
        return (Number) getValue("nAmtPaidx");
    }
    
    public JSONObject setFreight(Number freight){
        return setValue("nFreightx", freight);
    }
    
    public Number getFreight(){
        return (Number) getValue("nFreightx");
    }
    
    public JSONObject setRemarks(String remarks){
        return setValue("sRemarksx", remarks);
    }
    
    public String getRemarks(){
        return (String) getValue("sRemarksx");
    }
    
    public JSONObject setSourceNo(String sourceNo){
        return setValue("sSourceNo", sourceNo);
    }
    
    public String getSourceNo(){
        return (String) getValue("sSourceNo");
    }
    
    public JSONObject setSourceCode(String sourceCode){
        return setValue("sSourceCd", sourceCode);
    }
    
    public String getSourceCode(){
        return (String) getValue("sSourceCd");
    }
    
    public JSONObject setPrint(String print){
        return setValue("cPrintxxx", print);
    }
    
    public String getPrint(){
        return (String) getValue("cPrintxxx");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject isProcessed(boolean isProcessed){
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }
    
    public boolean isProcessed(){
        return ((String) getValue("cProcessd")).equals("1");
    }
    
    public JSONObject setTransactionStatus(String transactionStatus){
        return setValue("cTranStat", transactionStatus);
    }
    
    public String getTransactionStatus(){
        return (String) getValue("cTranStat");
    }
    
    public JSONObject setCategoryCode(String categoryCode){
        return setValue("sCategrCd", categoryCode);
    }
    
    public String getCategoryCode(){
        return (String) getValue("sCategrCd");
    }
    
    public JSONObject setModifyingId(String modifyingId){
        return setValue("sModified", modifyingId);
    }
    
    public String getModifyingId(){
        return (String) getValue("sModified");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    @Override
    public String getNextCode() {
//        return "";
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
    //reference object models
    public Model_Branch Branch() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poBranch;
                } else {
                    poBranch.initialize();
                    return poBranch;
                }
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
    }
    
    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
    }
    
    public Model_Category Category() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sCategrCd"))) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals((String) getValue("sCategrCd"))) {
                return poCategory;
            } else {
                poJSON = poCategory.openRecord((String) getValue("sCategrCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCategory;
                } else {
                    poCategory.initialize();
                    return poCategory;
                }
            }
        } else {
            poCategory.initialize();
            return poCategory;
        }
    }
    
    public Model_Company Company() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sCompnyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompnyID"))) {
                return poCompany;
            } else {
                poJSON = poCompany.openRecord((String) getValue("sCompnyID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCompany;
                } else {
                    poCompany.initialize();
                    return poCompany;
                }
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
    }
    
    public Model_Client_Master Supplier() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSupplier"))) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals((String) getValue("sSupplier"))) {
                return poSupplier;
            } else {
                poJSON = poSupplier.openRecord((String) getValue("sSupplier"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poSupplier;
                } else {
                    poSupplier.initialize();
                    return poSupplier;
                }
            }
        } else {
            poSupplier.initialize();
            return poSupplier;
        }
    }
    
    public Model_Client_Address SupplierAddress() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sAddressID"))) {
            if (poSupplierAdress.getEditMode() == EditMode.READY
                    && poSupplierAdress.getClientId().equals((String) getValue("sAddressID"))) {
                return poSupplierAdress;
            } else {
                poJSON = poSupplierAdress.openRecord((String) getValue("sAddressID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poSupplierAdress;
                } else {
                    poSupplierAdress.initialize();
                    return poSupplierAdress;
                }
            }
        } else {
            poSupplierAdress.initialize();
            return poSupplierAdress;
        }
    }
    
    public Model_Client_Institution_Contact SupplierContactPerson() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sContctID"))) {
            if (poSupplierContactPerson.getEditMode() == EditMode.READY
                    && poSupplierContactPerson.getClientId().equals((String) getValue("sContctID"))) {
                return poSupplierContactPerson;
            } else {
                poJSON = poSupplierContactPerson.openRecord((String) getValue("sContctID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poSupplierContactPerson;
                } else {
                    poSupplierContactPerson.initialize();
                    return poSupplierContactPerson;
                }
            }
        } else {
            poSupplierContactPerson.initialize();
            return poSupplierContactPerson;
        }
    }
    
    public Model_POR_Master PurchaseOrderReceivingMaster() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPurchaseOrderReceiving.getEditMode() == EditMode.READY
                    && poPurchaseOrderReceiving.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPurchaseOrderReceiving;
            } else {
                poJSON = poPurchaseOrderReceiving.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPurchaseOrderReceiving;
                } else {
                    poPurchaseOrderReceiving.initialize();
                    return poPurchaseOrderReceiving;
                }
            }
        } else {
            poPurchaseOrderReceiving.initialize();
            return poPurchaseOrderReceiving;
        }
    }
    //end - reference object models
    
}
