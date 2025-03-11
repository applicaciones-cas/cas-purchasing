package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.client.model.Model_Client_Address;
import org.guanzon.cas.client.model.Model_Client_Institution_Contact;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModel;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_PO_Master extends Model{      
    //reference objects
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;    
    Model_Term poTerm;
    
    Model_Client_Master poSupplier;    
    Model_Client_Address poSupplierAdress;
    Model_Client_Institution_Contact poSupplierContactPerson;
    
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dTransact", "0000-00-00");
            poEntity.updateObject("dApproved", "0000-00-00");
            poEntity.updateObject("nCurrInvx", 0);
            poEntity.updateObject("nEstInvxx", 0);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateString("cConfirmd", Logical.NO);
            poEntity.updateString("cTranStat", StockRequestStatus.OPEN);
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
            poTerm = model.Term();
            
            ClientModel clientModel = new ClientModel(poGRider); 
            poSupplier = clientModel.ClientMaster();
            poSupplierAdress = clientModel.ClientAddress();
            poSupplierContactPerson = clientModel.ClientInstitutionContact();
            
            
           
            //end - initialize reference objects
            
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    public JSONObject setTransactionNo(String transactionNo){
        return setValue("sTransNox", transactionNo);
    }
    
    public String getTransactionNo  (){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setBranchCode(String branchCode){
        return setValue("sBranchCd", branchCode);
    }
    
    public String getBranchCode(){
        return (String) getValue("sBranchCd");
    }
    
    public JSONObject setIndustryID(String industryID){
        return setValue("sIndstCdx", industryID);
    }
    
    public String getIndustryID(){
        return (String) getValue("sIndstCdx");
    }
    
    public JSONObject setTransactionDate(Date transactionDate){
        return setValue("dTransact", transactionDate);
    }
    
    public Date getTransactionDate(){
        return (Date) getValue("dTransact");
    }
    
    public JSONObject setCompanyID(String companyID){
        return setValue("sCompnyID", companyID);
    }
    
    public String getCompanyID(){
        return (String) getValue("sCompnyID");
    }
    
    public JSONObject setDestinationID(String destinationID){
        return setValue("sDestinat", destinationID);
    }
    
    public String getDestinationID(){
        return (String) getValue("sDestinat");
    }
    
    public JSONObject setSupplierID(String supplierID){
        return setValue("sSupplier", supplierID);
    }
    
    public String getSupplierID(){
        return (String) getValue("sSupplier");
    }
    
    public JSONObject setAddressID(String addressID){
        return setValue("sAddrssID", addressID);
    }
    
    public String getAddressID(){
        return (String) getValue("sAddrssID");
    }
    
    public JSONObject setCategoryID(String CategoryID){
        return setValue("sCategrCd", CategoryID);
    }
    
    public String getCategoryID(){
        return (String) getValue("sCategrCd");
    }
    
    public JSONObject setContactID(String contactID){
        return setValue("sContctID", contactID);
    }
    
    public String getContactID(){
        return (String) getValue("sContctID");
    }
    
    public JSONObject setReference(String reference){
        return setValue("sReferNox", reference);
    }
    
    public String getReference(){
        return (String) getValue("sReferNox");
    }
    
    public JSONObject setTermCode(String termCode){
        return setValue("sTermCode", termCode);
    }
    
    public String getTermCode(){
        return (String) getValue("sTermCode");
    }
    
    public JSONObject setTranTotal(Number tranTotal){
        return setValue("nTranTotl", tranTotal);
    }
    
    public Number getTranTotal(){
        return (Number) getValue("nTranTotl");
    }
    
    public JSONObject setVatable(String vatable){
        return setValue("cVATaxabl", vatable);
    }
    
    public String getVatable(){
        return (String) getValue("cVATaxabl");
    }
    
    public JSONObject setVatRate(Number vatRate){
        return setValue("nVatRatex", vatRate);
    }
    
    public Number getVatRate(){
        return (Number) getValue("nVatRatex");
    }
    
    public JSONObject setWithHoldingTax(Number withHoldingTax){
        return setValue("nTWithHld", withHoldingTax);
    }
    
    public Number getWithHoldingTax(){
        return (Number) getValue("nTWithHld");
    }
    
    public JSONObject setDiscount(Number discount){
        return setValue("nDiscount", discount);
    }
    
    public Number getDiscount(){
        return (Number) getValue("nDiscount");
    }
    
    public JSONObject setAdditionalDiscount(Number additionalDiscount){
        return setValue("nAddDiscx", additionalDiscount);
    }
    
    public Number getAdditionalDiscount(){
        return (Number) getValue("nAddDiscx");
    }
    
    public JSONObject setAmountPaid(Number amountPaid){
        return setValue("nAmtPaidx", amountPaid);
    }
    
    public Number getAmountPaid(){
        return (Number) getValue("nAmtPaidx");
    }
    
    public JSONObject setNetTotal(Number netTotal){
        return setValue("nNetTotal", netTotal);
    }
    
    public Number getNetTotal(){
        return (Number) getValue("nNetTotal");
    }
    
    public JSONObject setRemarks(String industryId){
        return setValue("sRemarksx", industryId);
    }
    
    public String getRemarks(){
        return (String) getValue("sRemarksx");
    }
    
    public JSONObject setSourceCode(String sourceCode){
        return setValue("sSourceCd", sourceCode);
    }
    
    public String getSourceCode(){
        return (String) getValue("sSourceCd");
    }
    
    public JSONObject setSourceNo(String sourceNo){
        return setValue("sSourceNo", sourceNo);
    }
    
    public String getSourceNo(){
        return (String) getValue("sSourceNo");
    }
        
    public JSONObject setEmailSent(String emailSent){
        return setValue("cEmailSnt", emailSent);
    }
    
    public String getEmailSent(){
        return (String) getValue("cEmailSnt");
    }
    
    public JSONObject setNoEmailSent(int noEmailSent){
        return setValue("nEmailSnt", noEmailSent);
    }
    
    public int getNoEmailSent(){
        return (int) getValue("nEmailSnt");
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
    
    public JSONObject setInventoryTypeCode(String inventoryTypeCode){
        return setValue("sInvTypCd", inventoryTypeCode);
    }
    
    public String getInventoryTypeCode(){
        return (String) getValue("sInvTypCd");
    }
    
    public JSONObject setExpectedDate(Date expectedDate){
        return setValue("dExpected", expectedDate);
    }
    
    public Date getExpectedDate(){
        return (Date) getValue("dExpected");
    }
    
    public JSONObject setWithAdditional(Number withAdditional){
        return setValue("cWithAddx", withAdditional);
    }
    
    public Number getWithAdditional(){
        return (Number) getValue("cWithAddx");
    }
    
    public JSONObject setTransactionStatus(String transactionStatus){
        return setValue("cTranStat", transactionStatus);
    }
    
    public String getTransactionStatus(){
        return (String) getValue("cTranStat");
    }
    
    public JSONObject setPreparedID(String preparedID){
        return setValue("sPrepared", preparedID);
    }
    
    public String getPreparedID(){
        return (String) getValue("sPrepared");
    }
    
    public JSONObject setDatePrepared(Date datePrepared){
        return setValue("dPrepared", datePrepared);
    }
    
    public Date getDatePrepared(){
        return (Date) getValue("dPrepared");
    }
    
    public JSONObject setApprovedID(String approvedID){
        return setValue("sApproved", approvedID);
    }
    
    public String getApprovedID(){
        return (String) getValue("sApproved");
    }
    
    public JSONObject setDateApproved(Date dateApproved){
        return setValue("dApproved", dateApproved);
    }
    
    public Date getDateApproved(){
        return (Date) getValue("dApproved");
    }
    
    public JSONObject setApprovalCode(String approvalCode){
        return setValue("sAprvCode", approvalCode);
    }
    
    public String getApprovalCode(){
        return (String) getValue("sAprvCode");
    }
    
    public JSONObject setPostedeID(String postedID){
        return setValue("sPostedxx", postedID);
    }
    
    public String getPostedeID(){
        return (String) getValue("sPostedxx");
    }
    
    public JSONObject setDatePost(Date datePost){
        return setValue("dPostedxx", datePost);
    }
    
    public Date getDatePost(){
        return (Date) getValue("dPostedxx");
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
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getConnection(), poGRider.getBranchCode());
    }
    
    //reference object models
    public Model_Branch Branch() {
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
    
    public Model_Industry Industry() {
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
    
    public Model_Category Category() {
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
    
    public Model_Company Company() {
        if (!"".equals((String) getValue("sCompanyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompanyID"))) {
                return poCompany;
            } else {
                poJSON = poCompany.openRecord((String) getValue("sCategrCd"));

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
    
    public Model_Term Term() {
        if (!"".equals((String) getValue("sTermCode"))) {
            if (poTerm.getEditMode() == EditMode.READY
                    && poTerm.getTermCode().equals((String) getValue("sTermCode"))) {
                return poTerm;
            } else {
                poJSON = poTerm.openRecord((String) getValue("sTermCode"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poTerm;
                } else {
                    poTerm.initialize();
                    return poTerm;
                }
            }
        } else {
            poTerm.initialize();
            return poTerm;
        }
    }
    
    
    public Model_Client_Master Supplier() {
        if (!"".equals((String) getValue("sClientID"))) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals((String) getValue("sClientID"))) {
                return poSupplier;
            } else {
                poJSON = poSupplier.openRecord((String) getValue("sClientID"));

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
    
    public Model_Client_Address SupplierAddress() {
        if (!"".equals((String) getValue("sClientID"))) {
            if (poSupplierAdress.getEditMode() == EditMode.READY
                    && poSupplierAdress.getClientId().equals((String) getValue("sAddressID"))) {
                return poSupplierAdress;
            } else {
                poJSON = poSupplierAdress.openRecord((String) getValue("sClientID"));

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
    
    public Model_Client_Institution_Contact SupplierContactPerson() {
        if (!"".equals((String) getValue("sClientID"))) {
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
    //end - reference object models
}