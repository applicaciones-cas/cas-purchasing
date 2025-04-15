package org.guanzon.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.StockRequest;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class PurchaseOrder extends Transaction {

    List<Model_Inv_Stock_Request_Master> paStockRequest;
    List<Model_PO_Master> paPOMaster;
    List<StockRequest> poStockRequest;
    boolean pbPrinted = false;
    private boolean pbApproval = false;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "PO";
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();
        poStockRequest = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }
    
    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CONFIRMED;
        boolean lbConfirm = true;
        
        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }


//        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", (String) poJSON.get("message"));
//            return poJSON;
//        }
        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() == 16){
//            poJSON = ShowDialogFX.getUserApproval(poGRider);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }
        }    
        
        
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm,true);

        if (!"success".equals((String) poJSON.get("result"))) {            
            poGRider.rollbackTrans();      
            return poJSON;
        }
        
        //Update Purchase Order
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poGRider.commitTrans();
        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.APPROVED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }
        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.APPROVED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Update Purchase Order
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.APPROVED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.APPROVED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CANCELLED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }
        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

         if (poGRider.getUserLevel() == 16)
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
        }
            
        //Update Purchase Order
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.VOID;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }
        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

         if (poGRider.getUserLevel() == 16)
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
        }
            
        //Update Purchase Order
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.RETURNED;
        boolean lbReturn = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }
        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() == 16)
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
        }
            
        //Update Purchase Order
        poJSON = updateStockRequest(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbReturn) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction returned request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = new JSONObject();
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "Last row has empty item.");
            return loJSON;
        }
        return addDetail();
    }

    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryID(object.getModel().getIndustryId());
        }

        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTermCode(object.getModel().getTermId());
        }

        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = poGRider.getIndustry().isEmpty() ? null : poGRider.getIndustry();

        poJSON = object.searchRecord(value, byCode, supplier, brand, industry);

        String scannedStockID = object.getModel().getStockId();
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingSourceID = (String) Detail(lnCtr).getSouceNo();
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (existingSourceID.isEmpty() && scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
            Detail(row).setStockID(scannedStockID);
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());

        }
        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException,
            NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = poGRider.getIndustry().isEmpty() ? null : poGRider.getIndustry();

        poJSON = object.searchRecord(value, byCode, supplier, brand, industry);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        String scannedStockID = object.getModel().getStockId();
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingSourceID = (String) Detail(lnCtr).getSouceNo();
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (existingSourceID.isEmpty() && scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
        }

        Detail(row).setStockID(scannedStockID);
        Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
        Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.Master().getModel().getClientId());
            Master().setAddressID(object.ClientAddress().getModel().getAddressId()); //TODO
            Master().setContactID(object.ClientInstitutionContact().getModel().getClientId()); //TODO
        }

        return poJSON;
    }

    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyID(object.getModel().getCompanyId());
        }
        return poJSON;
    }

    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestinationID(object.getModel().getBranchCode());
        }
        return poJSON;
    }

    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Brand brand = new ParamControllers(poGRider, logwrapr).Brand();
        brand.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = brand.searchRecord(value, byCode, poGRider.getIndustry());

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).setBrandId(brand.getModel().getBrandId());
        }

        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException, NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = poGRider.getIndustry().isEmpty() ? null : poGRider.getIndustry();

        poJSON = object.searchRecord(value, byCode, supplier, brand, industry);        
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }

        return poJSON;
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_PO_Master Master() {
        return (Model_PO_Master) poMaster;
    }

    @Override
    public Model_PO_Detail Detail(int row) {
        return (Model_PO_Detail) paDetail.get(row);
    }

    @Override
    protected JSONObject isEntryOkay(String status) throws CloneNotSupportedException {
        GValidator loValidator = PurchaseOrderValidatorFactory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());

        poJSON = loValidator.validate();
        return poJSON;
    }

    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException {
        /* Put system validations and other assignments here */
        poJSON = new JSONObject();

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {
            Master().setTransactionStatus(PurchaseOrderStatus.OPEN); // If edited, update transaction status to OPEN
        }

        boolean allZeroQuantity = true;  // Assume all items have zero quantity initially
        boolean hasValidItem = false;    // Flag for at least one valid item

        // Step 1: Scan all items first
        for (int lnCntr = 0; lnCntr < getDetailCount(); lnCntr++) {
            int quantity = Detail(lnCntr).getQuantity().intValue();
            String stockID = (String) Detail(lnCntr).getValue("sStockIDx");

            if (quantity > 0 && !stockID.isEmpty()) {
                allZeroQuantity = false;  // Found at least one valid item
                hasValidItem = true;
            }
        }

        // Step 2: If all items have zero quantity, return an error and stop
        if (allZeroQuantity) {
            poJSON.put("result", "error");
            poJSON.put("message", "All items have zero quantity. Please enter a valid quantity.");
            return poJSON;
        }

        // Step 3: If there is at least one valid item, proceed to remove invalid rows
        if (hasValidItem) {
            Iterator<Model> detail = Detail().iterator();
            while (detail.hasNext()) {
                Model item = detail.next();
                int quantity = (int) item.getValue("nQuantity");
                String stockID = (String) item.getValue("sStockIDx");

                // Remove only items with empty stock ID or zero quantity
                if (stockID.isEmpty() || quantity <= 0) {
                    detail.remove();
                }
            }
        }
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        return isEntryOkay(PurchaseOrderStatus.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;

    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + "  a.sTransNox,"
                + "  a.dTransact,"
                + "  b.sDescript,"
                + "  c.sCompnyNm,"
                + "  e.sCompnyNm "
                + " FROM po_master a "
                + "LEFT JOIN Industry b ON a.sIndstCdx = b.sIndstCdx "
                + "LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + "LEFT JOIN inv_supplier d ON a.sSupplier = d.sSupplier "
                + "LEFT JOIN client_master e ON d.sSupplier = e.sClientID";

    }

    public JSONObject SearchTransaction(String fsValue) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        String lsIndustryCondition = !poGRider.getIndustry().isEmpty()
                ? " a.sIndstCdx = " + SQLUtil.toSQL(poGRider.getIndustry())
                : " a.sIndstCdx LIKE '%'";
        String lsCompanyCondition = !Master().getCompanyID().isEmpty()
                ? " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                : " a.sCompnyID LIKE '%'";
        String lsSupplier = !Master().getSupplierID().isEmpty()
                ? " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID())
                : " a.sSupplier LIKE '%'";
        String lsReferNo = !Master().getReference().isEmpty()
                ? " a.sReferNox = " + SQLUtil.toSQL(Master().getReference())
                : " a.sReferNox LIKE '%'";
        String lsFilterCondition = lsIndustryCondition
                + " AND "
                + lsCompanyCondition
                + " AND "
                + lsSupplier
                + " AND "
                + lsReferNo;
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY a.sTransNox";

        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Company»Supplier",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject SearchTransaction(String fsValue, String fsSupplierID, String fsReferID) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        String lsSupplier = !fsSupplierID.isEmpty()
                ? " a.sSupplier = " + SQLUtil.toSQL(fsSupplierID)
                : " a.sSupplier LIKE '%'";
        String lsReferNo = !fsReferID.isEmpty()
                ? " a.sReferNox = " + SQLUtil.toSQL(fsReferID)
                : " a.sReferNox LIKE '%'";
        String lsFilterCondition = " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
                + " AND "
                + " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                + " AND "
                + lsSupplier
                + " AND "
                + lsReferNo;
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Company»Supplier",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    private Model_Inv_Stock_Request_Master InvStockRequestList() {
        return new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
    }

    public Model_Inv_Stock_Request_Master InvStockRequestMaster(int row) {
        return (Model_Inv_Stock_Request_Master) paStockRequest.get(row);
    }

    public int getInvStockRequestCount() {
        return this.paStockRequest.size();
    }

    public JSONObject getApprovedStockRequests() throws SQLException, GuanzonException {
        String lsIndustryCondition = poGRider.getIndustry().isEmpty()
                ? "a.sIndstCdx LIKE '%'"
                : "a.sIndstCdx = " + SQLUtil.toSQL(poGRider.getIndustry());
        String lsCompanyCondition = Master().getCompanyID().isEmpty()
                ? "e.sCompnyID LIKE '%'"
                : "e.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID());
        String lsSupplier = Master().getSupplierID().isEmpty()
                ? "g.sSupplier LIKE '%'"
                : "g.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID());

        String lsFilterCondition = lsIndustryCondition
                + " AND "
                + lsCompanyCondition
                + " AND "
                + lsSupplier
                + " AND b.nApproved > 0 ";
        String lsSQL = "SELECT"
                + "  a.sTransNox,"
                + "  e.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details"
                + " FROM inv_stock_request_master a"
                + " LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN industry f ON a.sIndstCdx = f.sIndstCdx"
                + " LEFT JOIN inv_supplier g ON g.sStockIDx = c.sStockIDx";
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition + " AND a.cTranStat = '1'");

        lsSQL = lsSQL + " AND b.nApproved <> (b.nIssueQty + b.nOrderQty) "
                + " GROUP BY a.sTransNox "
                + " ORDER BY a.dTransact DESC";

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        JSONArray dataArray = new JSONArray();
        JSONObject loJSON = new JSONObject();

        if (loRS == null) {
            loJSON.put("result", "error");
            loJSON.put("message", "Query execution failed.");
            return loJSON;
        }

        try {
            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    JSONObject request = new JSONObject();
                    request.put("sTransNox", loRS.getString("sTransNox"));
                    request.put("sBranchCd", loRS.getString("sBranchCd"));
                    request.put("dTransact", loRS.getDate("dTransact"));
                    request.put("sReferNox", loRS.getString("sReferNox"));
                    request.put("cTranStat", loRS.getString("cTranStat"));
                    request.put("sBranchNm", loRS.getString("sBranchNm"));
                    request.put("total_details", loRS.getInt("total_details"));

                    dataArray.add(request);
                    lnctr++;
                }
                loJSON.put("data", dataArray);
                loJSON.put("result", "success");
                loJSON.put("message", "Record loaded successfully.");
            } else {
                loJSON.put("result", "error");
                loJSON.put("data", new JSONArray());
                loJSON.put("message", "No records found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        }

        return loJSON;
    }

    private boolean areAllStockRequestDetailsInPODetail(InvWarehouseControllers loTrans) {
        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            boolean found = false;

            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId()
                        )) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }

    public JSONObject addStockRequestOrdersToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        InvWarehouseControllers loTrans = new InvWarehouseControllers(poGRider, logwrapr);
        poJSON = loTrans.StockRequest().InitTransaction();

        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        poJSON = loTrans.StockRequest().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        if (areAllStockRequestDetailsInPODetail(loTrans)) {
            poJSON.put("result", "error");
            poJSON.put("message", "All stock request details are already in purchase order detail.");
            return poJSON;
        }

        // Create SQL filter conditions
        String lsIndustryCondition = poGRider.getIndustry().isEmpty()
                ? "a.sIndstCdx LIKE '%'"
                : "a.sIndstCdx = " + SQLUtil.toSQL(poGRider.getIndustry());
        String lsCompanyCondition = Master().getCompanyID().isEmpty()
                ? "e.sCompnyID LIKE '%'"
                : "e.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID());
        String lsSupplier = Master().getSupplierID().isEmpty()
                ? "g.sSupplier LIKE '%'"
                : "g.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID());

        String lsFilterCondition = String.join(" AND ", lsIndustryCondition, lsCompanyCondition, lsSupplier, "b.nApproved > 0");

        // Build SQL Query
        String detailQuery = "SELECT b.sStockIDx "
                + "FROM inv_stock_request_master a "
                + "LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd "
                + "LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox "
                + "LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx "
                + "LEFT JOIN inv_supplier g ON g.sStockIDx = c.sStockIDx";
        detailQuery = MiscUtil.addCondition(detailQuery, lsFilterCondition + " AND a.cTranStat = '1'");

        System.out.println("Executing SQL: " + detailQuery);

        // Fetch valid stock IDs
        Set<String> validStockIds = new HashSet<>();
        try (ResultSet rsDetail = poGRider.executeQuery(detailQuery)) {
            if (rsDetail == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }
            while (rsDetail.next()) {
                validStockIds.add(rsDetail.getString("sStockIDx"));
            }
        }

        boolean allProcessed = true;

        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            if (!validStockIds.contains(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                continue; // Skip details that do not match the master conditions
            }

            // If at least one stock is not fully processed, set flag to false
            if (loTrans.StockRequest().Detail(lnCtr).getApproved()
                    != loTrans.StockRequest().Detail(lnCtr).getIssued()
                    + loTrans.StockRequest().Detail(lnCtr).getPurchase()) {
                allProcessed = false;
            }

            boolean exists = false;
            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                int remainingStock = loTrans.StockRequest().Detail(lnCtr).getApproved()
                        - (loTrans.StockRequest().Detail(lnCtr).getIssued() + loTrans.StockRequest().Detail(lnCtr).getPurchase());

                if (remainingStock > 0) {
                    AddDetail();
                    int lnLastIndex = getDetailCount() - 1;
                    Detail(lnLastIndex).setSouceNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setTransactionNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setEntryNo(loTrans.StockRequest().Detail(lnCtr).getEntryNumber());
                    Detail(lnLastIndex).setStockID(loTrans.StockRequest().Detail(lnCtr).getStockId());
                    Detail(lnLastIndex).setRecordOrder(0);
                    Detail(lnLastIndex).setUnitPrice(loTrans.StockRequest().Detail(lnCtr).Inventory().getCost().doubleValue());
                    Detail(lnLastIndex).setQuantity(0);

//                    Detail(lnLastIndex).setSourceEntryNo(loTrans.StockRequest().Detail(lnCtr).getEntryNumber());
//                    Detail(lnLastIndex).setReceivedQuantity(loTrans.StockRequest().Detail(lnCtr).getReceived());
//                    Detail(lnLastIndex).setCancelledQuantity(loTrans.StockRequest().Detail(lnCtr).getCancelled());
                    Detail(lnLastIndex).setSouceCode(SOURCE_CODE);
                }
            }
        }

        // ✅ Only check `allProcessed` **after** the loop
        if (allProcessed) {
            poJSON.put("result", "error");
            poJSON.put("message", "All records are already processed!");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }

    private Model_PO_Master POMasterList() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }

    public Model_PO_Master POMaster(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }

    public int getPOMasterCount() {
        return this.paPOMaster.size();
    }

    private StockRequest StockRequest() {
        return new InvWarehouseControllers(poGRider, logwrapr).StockRequest();
    }

    private JSONObject updateStockRequest(String status, boolean isUpdateStatus) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnCtr, lnRow, lnList;
        int lnRecQty = 0;
        boolean lbExist = false;
//        StockRequest().setWithParent(true);
        // Update Stock Request
        for (lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            System.out.println("PO DETAIL STOCK ID");
            System.out.println("sourceno : " + lnCtr + " : " + Detail(lnCtr).getSouceNo());
            System.out.println("stockid : " + lnCtr + " : " + Detail(lnCtr).getStockID());
            System.out.println("---------------------------------------------------------------------------");
            if (Detail(lnCtr).getSouceNo() != null && !Detail(lnCtr).getSouceNo().isEmpty()) {
                // Check for discrepancy
                if (Detail(lnCtr).getQuantity().intValue() != Detail(lnCtr).InvStockRequestDetail().getQuantity()) {
                    if (!pbApproval && isUpdateStatus) {
//                        poJSON = ShowDialogFX.getUserApproval(poGRider);
//                        if (!"success".equals(poJSON.get("result"))) {
//                            return poJSON;
//                        }
                        pbApproval = true; // User approval obtained
                    }
                }

                // Check if order number exists in stock request list
                lbExist = false;
                lnList = -1;
                for (lnRow = 0; lnRow < poStockRequest.size(); lnRow++) {
                    if (poStockRequest.get(lnRow).Master().getTransactionNo() != null
                            && poStockRequest.get(lnRow).Master().getTransactionNo().equals(Detail(lnCtr).getSouceNo())) {
                        lbExist = true;
                        lnList = lnRow;
                        break;
                    }
                }

                if (!lbExist) {
                    StockRequest newRequest = StockRequest();
                    newRequest.InitTransaction();
                    newRequest.OpenTransaction(Detail(lnCtr).getSouceNo());
                    newRequest.UpdateTransaction();
                    poStockRequest.add(newRequest);
                    lnList = poStockRequest.size() - 1;
                }

                if (lnList >= 0) { // Ensure lnList is valid
                    for (lnRow = 0; lnRow < poStockRequest.get(lnList).getDetailCount(); lnRow++) {

                        if (Detail(lnCtr).getStockID().equals(poStockRequest.get(lnList).Detail(lnRow).getStockId())) {
                            // Get total received quantity
                            lnRecQty = getReceivedQty(Detail(lnCtr).getSouceNo(), Detail(lnCtr).getStockID());
                            // Update stock request with the correct quantity
                            StockRequest stockRequest = new InvWarehouseControllers(poGRider, logwrapr).StockRequest();
                            
                            poJSON = stockRequest.InitTransaction();
                            if (!"success".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                            
                            poJSON = stockRequest.OpenTransaction(Detail(lnCtr).getSouceNo());
                            if (!"success".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                            
                            int request = stockRequest.Detail(lnRow).getApproved() - (stockRequest.Detail(lnRow).getIssued() + stockRequest.Detail(lnRow).getPurchase());
                            if (request == 0) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "All stock requests related to this order number have already been processed.");
                                return poJSON;
                            }
                            
                            poJSON = stockRequest.UpdateTransaction();
                            if (!"success".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                            
                            int currentqty = stockRequest.Detail(lnRow).getPurchase();
//                            System.out.println("lnRow ng inv >> " + lnRow + " getPurchase >> " + currentqty + " order >> " + Detail(lnCtr).getQuantity() );
//                            System.out.println("--------------");
                            switch (status) {
                                case PurchaseOrderStatus.CONFIRMED:
                                    stockRequest.Detail(lnRow).setPurchase(currentqty + lnRecQty);
                                    break;
                                case PurchaseOrderStatus.APPROVED:
                                    stockRequest.Master().setProcessed(true);
                                    break;
                                case PurchaseOrderStatus.RETURNED:
                                    stockRequest.Detail(lnRow).setPurchase(currentqty - lnRecQty);
                                    break;
                                case PurchaseOrderStatus.VOID:
                                    stockRequest.Detail(lnRow).setPurchase(currentqty - lnRecQty);
                                    break;
                            }
                            stockRequest.Detail(lnRow).setModifiedDate(poGRider.getServerDate());


                            StockRequest().setWithParent(true);
                            poJSON = stockRequest.SaveTransaction();
                            if (!"success".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }
                }
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private int getReceivedQty(String orderNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnRecQty = 0;
        String lsSQL = "SELECT b.nQuantity AS nQuantity "
                + "FROM po_master a "
                + "LEFT JOIN po_detail b ON b.sTransNox = a.sTransNox ";

        // Add conditions to SQL query
        lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND b.sSourceNo = " + SQLUtil.toSQL(orderNo)
                + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId));
//                + " AND (a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
//                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)  ")";

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.OPEN)) {
            lsSQL = lsSQL + "AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.OPEN);
        } else {
            lsSQL = lsSQL + " AND (a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                    + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + ")";
        }

        System.out.println("executeQuery: >>>> " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            // Check if result set contains records
            if (loRS != null && loRS.next()) {
                // Iterate through result set and sum quantities
                do {
                    lnRecQty = loRS.getInt("nQuantity");
                } while (loRS.next()); // Continue to sum if there are multiple rows
            }
        } catch (SQLException e) {
            // Handle exception: log or rethrow as appropriate
            System.out.println("Error loading received quantities: " + e.getMessage());
            lnRecQty = 0;
        } finally {
            // Always close ResultSet to avoid memory leak
            MiscUtil.close(loRS);
        }
        return lnRecQty;
    }

    public JSONObject getPurchaseOrder(String fsSupplierID, String fsReferID) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat"
                + " FROM po_master a "
                + " LEFT JOIN po_detail b ON b.sTransNox = a.sTransNox"
                + " LEFT JOIN branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN industry d ON a.sIndstCdx = d.sIndstCdx";

        String lsSupplier = !fsSupplierID.isEmpty()
                ? " a.sSupplier = " + SQLUtil.toSQL(fsSupplierID)
                : " a.sSupplier LIKE '%'";
        String lsReferNo = !fsReferID.isEmpty()
                ? " a.sReferNox = " + SQLUtil.toSQL(fsReferID)
                : " a.sReferNox LIKE '%'";
        String lsFilterCondition = " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
                + " AND "
                + "a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                + " AND "
                + lsSupplier
                + " AND "
                + lsReferNo;
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paPOMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                paPOMaster.add(POMasterList());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            paPOMaster = new ArrayList<>();
            paPOMaster.add(POMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    public JSONObject printTransaction() {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", poGRider.getClientName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("sApprval1", "John Doe");
            parameters.put("sApprval2", "Lu Cifer");
            parameters.put("sApprval3", "Le Min Hoo");
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));

            if (Master().getTransactionStatus().equals(PurchaseOrderStatus.APPROVED)) {
                watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
            }
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0;
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrice().doubleValue() * Detail(lnCtr).getQuantity().intValue();

                orderDetails.add(new OrderDetail(lnCtr,
                        String.valueOf(Detail(lnCtr).getSouceNo()),
                        Detail(lnCtr).Inventory().getBarCode(),
                        Detail(lnCtr).Inventory().getDescription(),
                        Detail(lnCtr).getUnitPrice().doubleValue(),
                        Detail(lnCtr).getQuantity().intValue(),
                        lnTotal));
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            String jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrder.jrxml"; //TODO
            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            CustomJasperViewer viewer = new CustomJasperViewer(jasperPrint);
            viewer.setVisible(true);
            poJSON.put("result", "success");
        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;
    }

    public static class OrderDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sDescription;
        private double nUprice;
        private Integer nOrder;
        private double nTotal;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String description,
                double uprice, Integer order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = total;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsOrderNo() {
            return sOrderNo;
        }

        public String getsBarcode() {
            return sBarcode;
        }

        public String getsDescription() {
            return sDescription;
        }

        public double getnUprice() {
            return nUprice;
        }

        public Integer getnOrder() {
            return nOrder;
        }

        public double getnTotal() {
            return nTotal;
        }
    }

    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        private void customizePrintButton(JasperPrint jasperPrint) {
            poJSON = new JSONObject();
            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    System.out.println("JRViewer not found!");
                    return;
                }

                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {
                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {
                                JButton button = (JButton) toolbar.getComponent(j);

                                //if ever na kailangan e hide si button save
//                                if (button.getToolTipText() != null) {
//                                    if (button.getToolTipText().equals("Save")) {
//                                        button.setEnabled(false);  // Disable instead of hiding
//                                        button.setVisible(false);  // Hide it completely
//                                    }
//                                }
                                if ("Print".equals(button.getToolTipText())) {
                                    for (ActionListener al : button.getActionListeners()) {
                                        button.removeActionListener(al);
                                    }
                                    button.addActionListener(e -> {
                                        try {
                                            boolean isPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                            if (isPrinted) {
                                                PrintTransaction(true);
                                            } else {
                                                Platform.runLater(() -> {
                                                    ShowMessageFX.Warning("Printing was canceled by the user.", "Print Purchase Order", null);
                                                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());

                                                });
                                            }
                                        } catch (JRException ex) {
                                            Platform.runLater(() -> {
                                                ShowMessageFX.Warning("Print Failed: " + ex.getMessage(), "Computerized Accounting System", null);
                                                SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                                            });
                                        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                                            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    });
                                }
                            }
                        }

                        // Force UI refresh after hiding the button
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
            }
        }

        private void PrintTransaction(boolean fbIsPrinted) throws SQLException, CloneNotSupportedException, GuanzonException {
            poJSON = new JSONObject();
            if (fbIsPrinted) {
                if (((String) poMaster.getValue("cTranStat")).equals(PurchaseOrderStatus.APPROVED)) {
                    poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    poMaster.setValue("dModified", poGRider.getServerDate());
                    poMaster.setValue("sModified", poGRider.getUserID());
                    poMaster.setValue("cPrintxxx", Logical.YES);

                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed successfully.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            } else {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed aborted.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            }
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] components = ((Container) parent).getComponents();
                for (Component component : components) {
                    JRViewer viewer = findJRViewer(component);
                    if (viewer != null) {
                        return viewer;
                    }
                }
            }
            return null;
        }

    }

    public String getInventoryTypeCode() throws SQLException {
        String lsSQL = "SELECT sInvTypCd FROM category";
        lsSQL = MiscUtil.addCondition(lsSQL, " sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()));

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String inventoryTypeCode = null;

        if (loRS.next()) {
            inventoryTypeCode = loRS.getString("sInvTypCd");
        }

        MiscUtil.close(loRS);
        return inventoryTypeCode;
    }
}
