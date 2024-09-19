package org.guanzon.cas.purchasing.model;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.base.CommonUtils;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.appdriver.iface.GEntity;
import org.json.simple.JSONObject;

/**
 *
 * @author Maynard
 */
public class Model_PO_Master implements GEntity {

    final String XML = "Model_PO_Master.xml";

    GRider poGRider;                //application driver
    CachedRowSet poEntity;          //rowset
    JSONObject poJSON;              //json container
    int pnEditMode;                 //edit mode
    private String fsExclude="xBranchNm»xCompnyNm»xDestinat»xSupplier»"
                +"xAddressx»xCPerson1»xCPPosit1»xCPMobil1»xTermName»" 
                +"xCategrNm»xInvTypNm»sDescript»"
                +"nQtyOnHnd»nROQQtyxx»nOrderQty";
    /**
     * Entity constructor
     *
     * @param foValue - GhostRider Application Driver
     */
    public Model_PO_Master(GRider foValue) {
        if (foValue == null) {
            System.err.println("Application Driver is not set.");
            System.exit(1);
        }

        poGRider = foValue;

        initialize();
    }

    /**
     * Gets edit mode of the record
     *
     * @return edit mode
     */
    @Override
    public int getEditMode() {
        return pnEditMode;
    }

    /**
     * Gets the column index name.
     *
     * @param fnValue - column index number
     * @return column index name
     */
    @Override
    public String getColumn(int fnValue) {
        try {
            return poEntity.getMetaData().getColumnLabel(fnValue);
        } catch (SQLException e) {
        }
        return "";
    }

    /**
     * Gets the column index number.
     *
     * @param fsValue - column index name
     * @return column index number
     */
    @Override
    public int getColumn(String fsValue) {
        try {
            return MiscUtil.getColumnIndex(poEntity, fsValue);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Gets the total number of column.
     *
     * @return total number of column
     */
    @Override
    public int getColumnCount() {
        try {
            return poEntity.getMetaData().getColumnCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Gets the table name.
     *
     * @return table name
     */
    @Override
    public String getTable() {
        return "PO_Master";
    }

    /**
     * Gets the value of a column index number.
     *
     * @param fnColumn - column index number
     * @return object value
     */
    @Override
    public Object getValue(int fnColumn) {
        try {
            return poEntity.getObject(fnColumn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the value of a column index name.
     *
     * @param fsColumn - column index name
     * @return object value
     */
    @Override
    public Object getValue(String fsColumn) {
        try {
            return poEntity.getObject(MiscUtil.getColumnIndex(poEntity, fsColumn));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets column value.
     *
     * @param fnColumn - column index number
     * @param foValue - value
     * @return result as success/failed
     */
    @Override
    public JSONObject setValue(int fnColumn, Object foValue) {
        try {
            poJSON = MiscUtil.validateColumnValue(System.getProperty("sys.default.path.metadata") + XML, MiscUtil.getColumnLabel(poEntity, fnColumn), foValue);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poEntity.updateObject(fnColumn, foValue);
            poEntity.updateRow();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            poJSON.put("value", getValue(fnColumn));
        } catch (SQLException e) {
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }

    /**
     * Sets column value.
     *
     * @param fsColumn - column index name
     * @param foValue - value
     * @return result as success/failed
     */
    @Override
    public JSONObject setValue(String fsColumn, Object foValue) {
        poJSON = new JSONObject();

        try {
            return setValue(MiscUtil.getColumnIndex(poEntity, fsColumn), foValue);
        } catch (SQLException e) {
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }

    /**
     * Set the edit mode of the entity to new.
     *
     * @return result as success/failed
     */
    @Override
    public JSONObject newRecord() {
        pnEditMode = EditMode.ADDNEW;

        //replace with the primary key column info
        setTransactionNo(MiscUtil.getNextCode(getTable(), "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode()));

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Opens a record.
     *
     * @param fsCondition - filter values
     * @return result as success/failed
     */
    @Override
    public JSONObject openRecord(String fsCondition) {
        poJSON = new JSONObject();

        String lsSQL = getSQL();

        //replace the condition based on the primary key column of the record
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(fsCondition));
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
                    setValue(lnCtr, loRS.getObject(lnCtr));
                }

                pnEditMode = EditMode.UPDATE;

                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;    
    }

    /**
     * Save the entity.
     *
     * @return result as success/failed
     */
    @Override
    public JSONObject saveRecord() {
        poJSON = new JSONObject();

//        poModelMaster.setModifiedDate(poGRider.getServerDate());

        if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
            String lsSQL;
            if (pnEditMode == EditMode.ADDNEW) {
                //replace with the primary key column info
                setTransactionNo(MiscUtil.getNextCode(getTable(), "sTransNox", true, poGRider.getConnection(), poGRider.getBranchCode()));

                lsSQL = makeSQL();

                if (!lsSQL.isEmpty()) {
                    if (poGRider.executeQuery(lsSQL, getTable(), poGRider.getBranchCode(), "") > 0) {
                        poJSON.put("result", "success");
                        poJSON.put("message", "Record saved successfully.");
                    } else {
                        poJSON.put("result", "error");
                        poJSON.put("message", poGRider.getErrMsg());
                    }
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No record to save.");
                }
            } else {
                Model_PO_Master loOldEntity = new Model_PO_Master(poGRider);

                //replace with the primary key column info
                JSONObject loJSON = loOldEntity.openRecord(this.getTransactionNo());

                if ("success".equals((String) loJSON.get("result"))) {
                    //replace the condition based on the primary key column of the record
                    lsSQL = MiscUtil.makeSQL(this, loOldEntity, "sTransNox = " + SQLUtil.toSQL(this.getTransactionNo()), fsExclude);

                    if (!lsSQL.isEmpty()) {
                        if (poGRider.executeQuery(lsSQL, getTable(), poGRider.getBranchCode(), "") > 0) {
                            poJSON.put("result", "success");
                            poJSON.put("message", "Record saved successfully.");
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", poGRider.getErrMsg());
                        }
                    } else {
                        poJSON.put("result", "success");
                        poJSON.put("message", "No updates has been made.");
                    }
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Record discrepancy. Unable to save record.");
                }
            }
        } else {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Invalid update mode. Unable to save record.");
//            return poJSON;
        }

        return poJSON;
    }

    /**
     * Prints all the public methods used<br>
     * and prints the column names of this entity.
     */
    @Override
    public void list() {
        Method[] methods = this.getClass().getMethods();

        System.out.println("--------------------------------------------------------------------");
        System.out.println("LIST OF PUBLIC METHODS FOR " + this.getClass().getName() + ":");
        System.out.println("--------------------------------------------------------------------");
        for (Method method : methods) {
            System.out.println(method.getName());
        }

        try {
            int lnRow = poEntity.getMetaData().getColumnCount();

            System.out.println("--------------------------------------------------------------------");
            System.out.println("ENTITY COLUMN INFO");
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Total number of columns: " + lnRow);
            System.out.println("--------------------------------------------------------------------");

            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
                System.out.println("Column index: " + (lnCtr) + " --> Label: " + poEntity.getMetaData().getColumnLabel(lnCtr));
                if (poEntity.getMetaData().getColumnType(lnCtr) == Types.CHAR
                        || poEntity.getMetaData().getColumnType(lnCtr) == Types.VARCHAR) {

                    System.out.println("Column index: " + (lnCtr) + " --> Size: " + poEntity.getMetaData().getColumnDisplaySize(lnCtr));
                }
            }
        } catch (SQLException e) {
        }

    }

    /**
     * Description: Sets the sTransNox of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTransactionNo(String fsValue) {
        return setValue("sTransNox", fsValue);
    }

    /**
     * @return The sTransNox of this record.
     */
    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    /**
     * Description: Sets the sBranchCd of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setBranchCode(String fsValue) {
        return setValue("sBranchCd", fsValue);
    }

    /**
     * @return The sBranchCd of this record.
     */
    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    /**
     * Description: Sets the dTransact of this record.
     *
     * @param fdValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTransDate(Date fdValue) {
        return setValue("dTransact", fdValue);
    }

    /**
     * @return The dTransact of this record.
     */
    public Date getTransDate() {
        return (Date) getValue("dTransact");
    }

    /**
     * Description: Sets the sCompnyID of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setCompanyID(String fsValue) {
        return setValue("sCompnyID", fsValue);
    }

    /**
     * @return The sCompnyID of this record.
     */
    public String getCompanyID() {
        return (String) getValue("sCompnyID");
    }

    /**
     * Description: Sets the sDestinat of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setDestination(String fsValue) {
        return setValue("sDestinat", fsValue);
    }

    /**
     * @return The sDestinat of this record.
     */
    public String getDestination() {
        return (String) getValue("sDestinat");
    }

    /**
     * Description: Sets the sSupplier of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setSupplier(String fsValue) {
        return setValue("sSupplier", fsValue);
    }

    /**
     * @return The sSupplier of this record.
     */
    public String getSupplier() {
        return (String) getValue("sSupplier");
    }

    /**
     * Description: Sets the sAddrssID of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setAddressID(String fsValue) {
        return setValue("sAddrssID", fsValue);
    }

    /**
     * @return The sAddrssID of this record.
     */
    public String getAddressID() {
        return (String) getValue("sAddrssID");
    }

    /**
     * Description: Sets the sContctID of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setContactID(String fsValue) {
        return setValue("sContctID", fsValue);
    }

    /**
     * @return The sContctID of this record.
     */
    public String getContactID() {
        return (String) getValue("sContctID");
    }

    /**
     * Description: Sets the sReferNox of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setReferNo(String fsValue) {
        return setValue("sReferNox", fsValue);
    }

    /**
     * @return The sReferNox of this record.
     */
    public String getReferNo() {
        return (String) getValue("sReferNox");
    }

    /**
     * Description: Sets the sTermCode of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTermCode(String fsValue) {
        return setValue("sTermCode", fsValue);
    }

    /**
     * @return The sTermCode of this record.
     */
    public String getTermCode() {
        return (String) getValue("sTermCode");
    }

    /**
     * Description: Sets the nTranTotl of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTransactionTotal(Number fnValue) {
        return setValue("nTranTotl", fnValue);
    }

    /**
     * @return The nTranTotl of this record.
     */
    public Number getTransactionTotal() {
        return (Number) getValue("nTranTotl");
    }

    /**
     * Description: Sets the nVatRatex of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setVatRate(Number fnValue) {
        return setValue("nVatRatex", fnValue);
    }

    /**
     * @return The nVatRatex of this record.
     */
    public Number getVatRate() {
        return (Number) getValue("nVatRatex");
    }

    /**
     * Description: Sets the nVatRatex of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setVatAmount(Number fnValue) {
        return setValue("nVatAmtxx", fnValue);
    }

    /**
     * @return The nVatRatex of this record.
     */
    public Number getVatAmount() {
        return (Number) getValue("nVatAmtxx");
    }

    /**
     * Sets the cVATAdded of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setVATAdded(String fsValue) {
        return setValue("cVATAdded", fsValue);
    }

    /**
     * @return The cVATAdded of this record.
     */
    public String setVATAdded() {
        return (String) getValue("cVATAdded");
    }

    /**
     * @return If VAT Added is selected.
     */
    public boolean isVATAdded() {
        return ((String) getValue("cVATAdded")).equals("1");
    }

    /**
     * Description: Sets the nTWithHld of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTaxWithHolding(Number fnValue) {
        return setValue("nTWithHld", fnValue);
    }

    /**
     * @return The nTWithHld of this record.
     */
    public Number getTaxWithHolding() {
        return (Number) getValue("nTWithHld");
    }

    /**
     * Description: Sets the nDiscount of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setDiscount(Number fnValue) {
        return setValue("nDiscount", fnValue);
    }

    /**
     * @return The nDiscount of this record.
     */
    public Number getDiscount() {
        return (Number) getValue("nDiscount");
    }

    /**
     * Description: Sets the nAddDiscx of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setAddDiscount(Number fnValue) {
        return setValue("nAddDiscx", fnValue);
    }

    /**
     * @return The nAddDiscx of this record.
     */
    public Number getAddDiscount() {
        return (Number) getValue("nAddDiscx");
    }

    /**
     * Description: Sets the nAddDiscx of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setAmountPaid(Number fnValue) {
        return setValue("nAmtPaidx", fnValue);
    }

    /**
     * @return The nAddDiscx of this record.
     */
    public Number getAmountPaid() {
        return (Number) getValue("nAmtPaidx");
    }

    /**
     * Description: Sets the nNetTotal of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setNetTotal(Number fnValue) {
        return setValue("nNetTotal", fnValue);
    }

    /**
     * @return The nNetTotal of this record.
     */
    public Number getNetTotal() {
        return (Number) getValue("nNetTotal");
    }

    /**
     * Description: Sets the sRemarksx of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setRemarks(String fsValue) {
        return setValue("sRemarksx", fsValue);
    }

    /**
     * @return The sRemarksx of this record.
     */
    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    /**
     * Description: Sets the sSourceCd of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setSourceCode(String fsValue) {
        return setValue("sSourceCd", fsValue);
    }

    /**
     * @return The sSourceCd of this record.
     */
    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    /**
     * Description: Sets the sSourceNo of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setSourceNo(String fsValue) {
        return setValue("sSourceNo", fsValue);
    }

    /**
     * @return The sSourceNo of this record.
     */
    public String getSourceNo() {
        return (String) getValue("sSourceCd");
    }

    /**
     * Description: Sets the cEmailSnt of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setEmailSentStatus(String fsValue) {
        return setValue("cEmailSnt", fsValue);
    }

    /**
     * @return The cEmailSnt of this record.
     */
    public String getEmailSentStatus() {
        return (String) getValue("cEmailSnt");
    }

    /**
     * @return If cEmailSnt is True or 1.
     */
    public boolean isEmailSent() {
        return ((String) getValue("cEmailSnt")).equals("1");
    }

    /**
     * Description: Sets the nEmailSnt of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setEmailSentNo(int fsValue) {
        return setValue("nEmailSnt", fsValue);
    }

    /**
     * @return The nEmailSnt of this record.
     */
    public int getEmailSentNo() {
        return (Integer) getValue("nEmailSnt");
    }

    /**
     * Description: Sets the nEntryNox of this record.
     *
     * @param fnValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setEntryNo(Number fnValue) {
        return setValue("nEntryNox", fnValue);
    }

    /**
     * @return The nEntryNox of this record.
     */
    public Number getEntryNo() {
        return  (Number) getValue("nEntryNox");
    }

    /**
     * Description: Sets the sCategrCd of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setCategoryCode(String fsValue) {
        return setValue("sCategrCd", fsValue);
    }

    /**
     * @return The sCategrCd of this record.
     */
    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }

    /**
     * Description: Sets the cPOTypexx of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setPOrderType(String fsValue) {
        return setValue("cPOTypexx", fsValue);
    }

    /**
     * @return The cPOTypexx of this record.
     */
    public String getPOrderType() {
        return (String) getValue("cPOTypexx");
    }

    /**
     * Description: Sets the cTranStat of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTransactionStatus(String fsValue) {
        return setValue("cTranStat", fsValue);
    }

    /**
     * @return The cTranStat of this record.
     */
    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    /**
     * Description: Sets the sPrepared of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setPreparedBy(String fsValue) {
        return setValue("sPrepared", fsValue);
    }

    /**
     * @return The sPrepared of this record.
     */
    public String getPreparedBy() {
        return (String) getValue("sPrepared");
    }

    /**
     * Description: Sets the dPrepared of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setPreparedDate(Date fdValue) {
        return setValue("dPrepared", fdValue);
    }

    /**
     * @return The dPrepared of this record.
     */
    public Date getPreparedDate() {
        return (Date) getValue("dPrepared");
    }

    /**
     * Description: Sets the sApproved of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setApprovedBy(String fsValue) {
        return setValue("sApproved", fsValue);
    }

    /**
     * @return The sApproved of this record.
     */
    public String getApprovedBy() {
        return (String) getValue("sApproved");
    }

    /**
     * Description: Sets the dApproved of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setApprovedate(Date fdValue) {
        return setValue("dApproved", fdValue);
    }

    /**
     * @return The dApproved of this record.
     */
    public Date getApprovedDate() {
        return (Date) getValue("dApproved");
    }

    /**
     * Description: Sets the sAprvCode of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setApprovalCode(String fsValue) {
        return setValue("sAprvCode", fsValue);
    }

    /**
     * @return The sAprvCode of this record.
     */
    public String sgtApprovalCode() {
        return (String) getValue("sAprvCode");
    }

    /**
     * Description: Sets the sPostedxx of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setPostedBy(String fsValue) {
        return setValue("sPostedxx", fsValue);
    }

    /**
     * @return The sPostedxx of this record.
     */
    public String getPostedBy() {
        return (String) getValue("sPostedxx");
    }

    /**
     * Description: Sets the dPostedxx of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setPostedDate(Date fdValue) {
        return setValue("dPostedxx", fdValue);
    }

    /**
     * @return The dPostedxx of this record.
     */
    public Date getPostedDate() {
        return (Date) getValue("dPostedxx");
    }

    /**
     * Description: Sets the sModified of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setModifiedBy(String fsValue) {
        return setValue("sModified", fsValue);
    }

    /**
     * @return The sModified of this record.
     */
    public String getModifiedBy() {
        return (String) getValue("sModified");
    }

    /**
     * Description: Sets the dModified of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setModifiedDate(Date fdValue) {
        return setValue("dModified", fdValue);
    }

    /**
     * @return The dModified of this record.
     */
    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    /**
     * Description: Sets the xBranchNm of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setBranchName(String fsValue) {
        return setValue("xBranchNm", fsValue);
    }

    /**
     * @return The xBranchNm of this record.
     */
    public String getBranchName() {
        return (String) getValue("xBranchNm");
    }

    /**
     * Description: Sets the sCompnyNm of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setCompanyName(String fsValue) {
        return setValue("xCompnyNm", fsValue);
    }

    /**
     * @return The sCompnyNm of this record.
     */
    public String getCompanyName() {
        return (String) getValue("xCompnyNm");
    }

    /**
     * Description: Sets the xDestinat of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setDestinationOther(String fsValue) {
        return setValue("xDestinat", fsValue);
    }

    /**
     * @return The xDestinat of this record.
     */
    public String getDestinationOther() {
        return (String) getValue("xDestinat");
    }

    /**
     * Description: Sets the xSupplier of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setSupplierName(String fsValue) {
        return setValue("xSupplier", fsValue);
    }

    /**
     * @return The xSupplier of this record.
     */
    public String getSupplierName() {
        return (String) getValue("xSupplier");
    }

    /**
     * Description: Sets the xAddressx of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setAddress(String fsValue) {
        return setValue("xAddressx", fsValue);
    }

    /**
     * @return The xAddressx of this record.
     */
    public String getAddress() {
        return (String) getValue("xAddressx");
    }

    /**
     * Description: Sets the xCPerson1 of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setContactPerson1(String fsValue) {
        return setValue("xCPerson1", fsValue);
    }

    /**
     * @return The xCPerson1 of this record.
     */
    public String getContactPerson1() {
        return (String) getValue("xCPerson1");
    }

    /**
     * Description: Sets the xCPPosit1 of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setContactPersonPosition(String fsValue) {
        return setValue("xCPPosit1", fsValue);
    }

    /**
     * @return The xCPPosit1 of this record.
     */
    public String getContactPersonPosition() {
        return (String) getValue("xCPPosit1");
    }

    /**
     * Description: Sets the xCPMobil1 of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setMobileNo(String fsValue) {
        return setValue("xCPMobil1", fsValue);
    }

    /**
     * @return The xCPMobil1 of this record.
     */
    public String getMobileNo() {
        return (String) getValue("xCPMobil1");
    }

    /**
     * Description: Sets the xTermName of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setTermName(String fsValue) {
        return setValue("xTermName", fsValue);
    }

    /**
     * @return The xTermName of this record.
     */
    public String getTermName() {
        return (String) getValue("xTermName");
    }

    /**
     * Description: Sets the xTexCategrNmrmName of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setCategoryName(String fsValue) {
        return setValue("xCategrNm", fsValue);
    }

    /**
     * @return The xCategrNm of this record.
     */
    public String getCategoryName() {
        return (String) getValue("xCategrNm");
    }
    
        /**
     * Description: Sets the xInvTypNm of this record.
     *
     * @param fsValue
     * @return True if the record assignment is successful.
     */
    public JSONObject setInvTypeName(String fsValue) {
        return setValue("xInvTypNm", fsValue);
    }

    /**
     * @return The xInvTypNm of this record.
     */
    public String getInvTypeName() {
        return (String) getValue("xInvTypNm");
    }

    /**
     * Gets the SQL statement for this entity.
     *
     * @return SQL Statement
     */
    public String makeSQL() {
        return MiscUtil.makeSQL(this, fsExclude);
    }
    
    public String makeSelectSQL() {
        return MiscUtil.makeSelect(this, fsExclude);
    }


    private void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            poEntity.updateString("cTranStat", TransactionStatus.STATE_OPEN);
            
            poEntity.updateString("dApproved", null);
            poEntity.updateString("dPostedxx", null);
            poEntity.updateString("sAprvCode", null);
//            poEntity.updateInt("nEntryNox", 0);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    
    private String getSQL(){
    String lsSQL = " SELECT "
            + " a.sTransNox sTransNox "
            + ", a.sBranchCd sBranchCd "
            + ", a.dTransact dTransact "
            + ", a.sCompnyID sCompnyID "
            + ", a.sDestinat sDestinat "
            + ", a.sSupplier sSupplier "
            + ", a.sAddrssID sAddrssID "
            + ", a.sContctID sContctID "
            + ", a.sReferNox sReferNox "
            + ", a.sTermCode sTermCode "
            + ", a.nTranTotl nTranTotl "
            + ", a.nVatRatex nVatRatex "
            + ", a.nVatAmtxx nVatAmtxx "
            + ", a.cVATAdded cVATAdded "
            + ", a.nTWithHld nTWithHld "
            + ", a.nDiscount nDiscount "
            + ", a.nAddDiscx nAddDiscx "
            + ", a.nAmtPaidx nAmtPaidx "
            + ", a.nNetTotal nNetTotal "
            + ", a.sRemarksx sRemarksx "
            + ", a.sSourceCd sSourceCd "
            + ", a.sSourceNo sSourceNo "
            + ", a.cEmailSnt cEmailSnt "
            + ", a.nEmailSnt nEmailSnt "
            + ", a.nEntryNox nEntryNox "
            + ", a.sCategrCd sCategrCd "
            + ", a.cPOTypexx cPOTypexx "
            + ", a.cTranStat cTranStat "
            + ", a.sPrepared sPrepared "
            + ", a.dPrepared dPrepared "
            + ", a.sApproved sApproved "
            + ", a.dApproved dApproved "
            + ", a.sAprvCode sAprvCode "
            + ", a.sPostedxx sPostedxx "
            + ", a.dPostedxx dPostedxx "
            + ", a.sModified sModified "
            + ", a.dModified dModified "
            + ", b.sBranchNm xBranchNm "
            + ", c.sCompnyNm xCompnyNm "
            + ", d.sBranchNm xDestinat "
            + ", e.sCompnyNm xSupplier "
            + ", f.sAddressx xAddressx "
            + ", g.sCPerson1 xCPerson1 "
            + ", h.sCPerson1 xCPerson2 "
            + ", i.sMobileNo xCPMobil1 "
            + ", j.sDescript xTermName "
            + ", k.sDescript xInvTypNm "
            + " FROM " + getTable() + " a "
            + " LEFT JOIN Branch b  ON a.sBranchCd = b.sBranchCd "
            + " LEFT JOIN Company c  ON a.sCompnyID = c.sCompnyID "
            + " LEFT JOIN Branch d ON a.sBranchCd = d.sBranchCd "
            + " LEFT JOIN Client_Master e  ON a.sSupplier = e.sClientID "
            + " LEFT JOIN Client_Address f  ON a.sAddrssID = f.sAddrssID "
            + " LEFT JOIN Client_Institution_Contact_Person g  ON a.sContctID = g.sContctID AND  g.cPrimaryx = '1'"
            + " LEFT JOIN Client_Institution_Contact_Person h  ON a.sContctID = g.sContctID AND  h.cPrimaryx = '0'"
            + " LEFT JOIN Client_Mobile i  ON a.sContctID = i.sClientID "
            + " LEFT JOIN Term j  ON a.sTermCode = j.sTermCode "
            + " LEFT JOIN Category_Level2 k  ON a.sCategrCd = k.sCategrCd "
            + " LEFT JOIN Inv_Type l  ON k.sInvTypCd = l.sInvTypCd ";
    
    
    return lsSQL;
    } 
}
